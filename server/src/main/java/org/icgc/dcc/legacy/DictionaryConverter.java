/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT 
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED 
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.legacy;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.FileSchemaRole;
import org.icgc.dcc.dictionary.model.Relation;
import org.icgc.dcc.dictionary.model.Restriction;
import org.icgc.dcc.dictionary.model.SummaryType;
import org.icgc.dcc.dictionary.model.ValueType;
import org.icgc.dcc.validation.restriction.RequiredRestriction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.mongodb.BasicDBObject;

/**
 * 
 */
public class DictionaryConverter {

  private static final Logger log = LoggerFactory.getLogger(DictionaryConverter.class);

  private static String DICTIONARY_VERSION = "0.6c";

  private Dictionary dictionary;

  private final ValueTypeConverter valueConverter = new ValueTypeConverter();

  public void saveToJSON(String fileName) throws JsonGenerationException, JsonMappingException, IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File(fileName), dictionary);
    mapper.enable(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY);
    mapper.readValue(new File(fileName), Dictionary.class);
  }

  public Dictionary readDictionary(String folder) throws IOException, XPathExpressionException,
      ParserConfigurationException, SAXException {
    if(dictionary == null) {
      dictionary = new Dictionary(DICTIONARY_VERSION);
    }
    File tsvFolder = new File(folder);
    FilenameFilter tsvFilter = new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".tsv");
      }
    };

    File[] tsvFiles = tsvFolder.listFiles(tsvFilter);

    Map<String, FileSchema> fileSchemas = new HashMap<String, FileSchema>();

    for(File tsvFile : tsvFiles) {
      fileSchemas.put(extractFileSchemaName(tsvFile), this.readFileSchema(tsvFile, FileSchemaRole.SUBMISSION));
    }

    // Read System FileSchema gene, transcript, mirna_base, Override the previous ones
    File systemFolder = new File("src/test/resources/integrationtest/fs/SystemFiles");

    tsvFiles = systemFolder.listFiles(tsvFilter);

    for(File tsvFile : tsvFiles) {
      fileSchemas.put(extractFileSchemaName(tsvFile), this.readFileSchema(tsvFile, FileSchemaRole.SYSTEM));
    }

    dictionary.setFiles(new ArrayList<FileSchema>(fileSchemas.values()));

    this.readFilePattern("src/test/resources/converter/file_to_pattern.tsv");

    Map<String, List<String>> schemaToUniqueFields =
        this.readRelations("src/test/resources/converter/icgc_relations.txt");
    log.info("schemaToUniqueFields = \n" + buildDebugString(schemaToUniqueFields));

    for(String fileSchemaName : schemaToUniqueFields.keySet()) {
      FileSchema fileSchema = fileSchemas.get(fileSchemaName);
      fileSchema.setUniqueFields(schemaToUniqueFields.get(fileSchemaName));
    }

    this.readXMLinfo("src/test/resources/converter/icgc.0.7.xml");

    return dictionary;
  }

  private String extractFileSchemaName(File tsvFile) {
    return FilenameUtils.getBaseName(tsvFile.getName());
  }

  private Map<String, List<String>> readRelations(String tsvFile) throws IOException {
    Map<String, List<String>> schemaToUniqueFields = new LinkedHashMap<String, List<String>>();
    String relationsText = Files.toString(new File(tsvFile), Charsets.UTF_8);

    Iterable<String> lines = Splitter.on('\n').trimResults().omitEmptyStrings().split(relationsText);
    Iterator<String> lineIterator = lines.iterator();

    this.readTSVHeader(lineIterator.next());

    while(lineIterator.hasNext()) {
      String line = lineIterator.next();

      Iterable<String> values = Splitter.on('\t').trimResults().split(line);
      Iterator<String> valueIterator = values.iterator();

      String leftTable = valueIterator.next();
      String leftKey = valueIterator.next();
      Iterable<String> leftKeys = Splitter.on(',').trimResults().omitEmptyStrings().split(leftKey);

      String rightTable = valueIterator.next();
      String rightKey = valueIterator.next();
      Iterable<String> rightKeys = Splitter.on(',').trimResults().omitEmptyStrings().split(rightKey);

      boolean bidirectionality = getBidirectionality(valueIterator.next());
      boolean rightUniqueness = Boolean.valueOf(valueIterator.next());
      if(rightUniqueness) {
        List<String> uniqueFields = schemaToUniqueFields.get(rightTable);
        List<String> uniqueFieldsTmp = Lists.newArrayList(rightKeys);
        if(uniqueFields == null) {
          Collections.sort(uniqueFieldsTmp);
          schemaToUniqueFields.put(rightTable, uniqueFieldsTmp);
        } else if(uniqueFields.equals(uniqueFieldsTmp) == false) {
          throw new ConverterException(String.format("schema %s already defines %s as unique fields, %s differs",
              rightTable, uniqueFieldsTmp, uniqueFields));
        }
      }

      String optional = valueIterator.next();
      Iterable<Integer> optionals =
          Iterables.transform(Splitter.on(',').trimResults().omitEmptyStrings().split(optional),
              new Function<String, Integer>() {
                @Override
                public Integer apply(String input) {
                  return Integer.valueOf(input);
                }
              });

      if(this.dictionary.hasFileSchema(leftTable)) {
        FileSchema leftFileSchema = this.dictionary.fileSchema(leftTable).get();
        leftFileSchema.addRelation(new Relation(leftKeys, rightTable, rightKeys, bidirectionality, optionals));
      }

      // mark relation fields to be required
      FileSchema leftFileSchema = this.dictionary.fileSchema(leftTable).get();
      // calculate optional keys
      List<String> optionalKeys = Lists.newArrayList();
      for(Integer optionalIndex : optionals) {
        optionalKeys.add(Iterables.toArray(leftKeys, String.class)[optionalIndex.intValue()]);
      }
      for(String key : leftKeys) {
        Field leftField = leftFileSchema.field(key).get();
        Optional<Restriction> leftRestriction = leftField.getRestriction(RequiredRestriction.NAME);
        if(leftRestriction.isPresent()) {
          // remove any existing required restrictions
          leftField.removeRestriction(leftRestriction.get());
        }
        Restriction requiredRestriction = new Restriction();
        requiredRestriction.setType(RequiredRestriction.NAME);
        BasicDBObject parameter = new BasicDBObject();
        // exceptions for making required field accept missing code
        if(rightTable.equals("hsap_gene") || rightTable.equals("hsap_transcript")) {
          parameter.append(RequiredRestriction.ACCEPT_MISSING_CODE, true);
        } else if(optionalKeys.contains(key)) {
          parameter.append(RequiredRestriction.ACCEPT_MISSING_CODE, true);
        } else {
          parameter.append(RequiredRestriction.ACCEPT_MISSING_CODE, false);
        }
        requiredRestriction.setConfig(parameter);
        if(leftFileSchema.getRole() != FileSchemaRole.SYSTEM) {
          leftField.addRestriction(requiredRestriction);
        }
      }
      FileSchema rightFileSchema = this.dictionary.fileSchema(rightTable).get();
      for(String key : rightKeys) {
        Field rightField = rightFileSchema.field(key).get();

        Optional<Restriction> rightRestriction = rightField.getRestriction(RequiredRestriction.NAME);
        if(rightRestriction.isPresent()) {
          // remove any existing required restrictions
          rightField.removeRestriction(rightRestriction.get());
        }
        Restriction requiredRestriction = new Restriction();
        requiredRestriction.setType(RequiredRestriction.NAME);
        BasicDBObject parameter = new BasicDBObject();
        // exceptions for making required field accept missing code
        if(rightTable.equals("hsap_gene") || rightTable.equals("hsap_transcript")) {
          parameter.append(RequiredRestriction.ACCEPT_MISSING_CODE, true);
        } else {
          parameter.append(RequiredRestriction.ACCEPT_MISSING_CODE, false);
        }
        requiredRestriction.setConfig(parameter);
        if(rightFileSchema.getRole() != FileSchemaRole.SYSTEM) {
          rightField.addRestriction(requiredRestriction);
        }
      }
    }

    return schemaToUniqueFields;
  }

  private Boolean getBidirectionality(String cardinalityString) {
    if("1..n".equals(cardinalityString)) {
      return true;
    } else if("0..n".equals(cardinalityString)) {
      return false;
    }
    return null;
  }

  private String buildDebugString(Map<String, List<String>> schemaToUniqueFields) {
    StringBuffer sb = new StringBuffer();
    for(String fileSchema : schemaToUniqueFields.keySet()) {
      sb.append("\t" + fileSchema + " -> " + schemaToUniqueFields.get(fileSchema) + "\n");
    }
    return sb.toString();
  }

  private void readFilePattern(String tsvFile) throws IOException {
    String filePatternText = Files.toString(new File(tsvFile), Charsets.UTF_8);

    Iterable<String> lines = Splitter.on('\n').trimResults().omitEmptyStrings().split(filePatternText);

    Iterator<String> lineIterator = lines.iterator();
    while(lineIterator.hasNext()) {
      String line = lineIterator.next();
      if(!line.startsWith("#")) {
        Iterable<String> values = Splitter.on('\t').trimResults().omitEmptyStrings().split(line);
        Iterator<String> valueIterator = values.iterator();

        String fileSchemaName = valueIterator.next();
        String filePattern = valueIterator.next();

        this.dictionary.fileSchema(fileSchemaName).get().setPattern(filePattern);
      }
    }
  }

  private void readXMLinfo(String xmlFile) throws ParserConfigurationException, SAXException, IOException,
      XPathExpressionException {

    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = domFactory.newDocumentBuilder();
    Document doc = builder.parse(xmlFile);

    NodeList sourceTables = doc.getElementsByTagName("sourcetable");

    for(int i = 0; i < sourceTables.getLength(); i++) {
      Element node = (Element) sourceTables.item(i);
      String name = node.getAttributes().getNamedItem("name").getTextContent();

      if(this.dictionary.hasFileSchema(name)) {
        FileSchema fileSchema = this.dictionary.fileSchema(name).get();
        NodeList columns = node.getElementsByTagName("column");

        for(int j = 0; j < columns.getLength(); j++) {
          Node columnNode = columns.item(j);
          String colName = columnNode.getAttributes().getNamedItem("name").getTextContent();
          String colLabel = columnNode.getAttributes().getNamedItem("description").getTextContent();

          if(fileSchema.hasField(colName)) {
            fileSchema.field(colName).get().setLabel(colLabel);
          }
        }
      }

    }
  }

  private FileSchema readFileSchema(File tsvFile, FileSchemaRole role) throws IOException {
    String FileSchemaName = FilenameUtils.removeExtension(extractFileSchemaName(tsvFile));
    FileSchema fileSchema = new FileSchema(FileSchemaName);

    String fileSchemaText = Files.toString(tsvFile, Charsets.UTF_8);

    Iterable<String> lines = Splitter.on('\n').omitEmptyStrings().split(fileSchemaText);

    Iterator<String> lineIterator = lines.iterator();
    // Read TSV header
    if(lineIterator.hasNext()) {
      this.readTSVHeader(lineIterator.next());
    }
    // initialize unique fields
    List<String> uniqueFields = new ArrayList<String>();
    fileSchema.setUniqueFields(uniqueFields);
    // Read field
    List<Field> fields = new ArrayList<Field>();
    while(lineIterator.hasNext()) {
      Field field = this.readField(lineIterator.next(), fileSchema);
      // hardcode special case for stsm_s gene_affected and transcript_affected
      if(FileSchemaName.equals("stsm_s") && field.getName().equals("gene_affected")) {
        Field field_from = new Field(field);
        Field field_to = new Field(field);
        field_from.setName("gene_affected_by_bkpt_from");
        field_to.setName("gene_affected_by_bkpt_to");

        fields.add(field_from);
        fields.add(field_to);
      } else if(FileSchemaName.equals("stsm_s") && field.getName().equals("transcript_affected")) {
        Field field_from = new Field(field);
        Field field_to = new Field(field);
        field_from.setName("transcript_affected_by_bkpt_from");
        field_to.setName("transcript_affected_by_bkpt_to");
        fields.add(field_from);
        fields.add(field_to);
      } else {
        fields.add(field);
      }
    }

    // special case for mirna_m, stsm_m, cnsm_m, jcn_m, ssm_m, meth_m, exp_m, add missing donor_id
    if(FileSchemaName.equals("mirna_m") || FileSchemaName.equals("stsm_m") || FileSchemaName.equals("cnsm_m")
        || FileSchemaName.equals("jcn_m") || FileSchemaName.equals("ssm_m") || FileSchemaName.equals("meth_m")
        || FileSchemaName.equals("exp_m")) {
      if(!this.containField(fields, "donor_id")) {
        Field donorIDField = new Field();
        donorIDField.setName("donor_id");
        donorIDField.setValueType(ValueType.TEXT);
        fields.add(donorIDField);
      }
    }
    fileSchema.setFields(fields);

    fileSchema.setRole(role);

    return fileSchema;
  }

  private Field readField(String line, FileSchema fileSchema) {
    Field field = new Field();
    Iterable<String> values = Splitter.on('\t').split(line);

    Iterator<String> iterator = values.iterator();
    String name = iterator.next();
    field.setName(name);

    String dataType = iterator.next();
    ValueType valueType = valueConverter.getMap().get(dataType);
    field.setValueType(valueType);

    // set Summary Type according to some rules
    if(valueType == ValueType.DECIMAL || valueType == ValueType.INTEGER) {
      field.setSummaryType(SummaryType.AVERAGE);
    }

    List<Restriction> restrictions = new ArrayList<Restriction>();

    // add required restriction
    String required = iterator.next();
    // only add required restriction for submission files
    if(Boolean.parseBoolean(required) && fileSchema.getRole() != FileSchemaRole.SYSTEM) {
      Restriction requiredRestriction = new Restriction();
      requiredRestriction.setType(RequiredRestriction.NAME);
      BasicDBObject parameter = new BasicDBObject();
      parameter.append(RequiredRestriction.ACCEPT_MISSING_CODE, true);
      requiredRestriction.setConfig(parameter);
      restrictions.add(requiredRestriction);
    }

    // add unique field if primary key is true
    String primaryKey = iterator.next();
    if(Boolean.parseBoolean(primaryKey)) {
      fileSchema.getUniqueFields().add(name);
    }

    // deconvolution
    String deconvolution = iterator.next();
    if(deconvolution.isEmpty()) {

    }

    String codeList = iterator.next();
    if(!codeList.isEmpty()) {
      Restriction codeListRestriction = new Restriction();
      codeListRestriction.setType("codelist");
      codeListRestriction.setConfig(new BasicDBObject());
      Iterator<String> nameIterator = Splitter.on('/').split(codeList).iterator();
      nameIterator.next();
      String codeListName = nameIterator.next();
      codeListName = codeListName.substring(0, codeListName.length() - 4);
      codeListRestriction.getConfig().append("name", codeListName);
      restrictions.add(codeListRestriction);

      // set Summary Type to Frequency for codelist
      field.setSummaryType(SummaryType.FREQUENCY);
    }

    field.setRestrictions(restrictions);

    return field;
  }

  private Iterable<String> readTSVHeader(String line) {
    return Splitter.on('\t').trimResults().omitEmptyStrings().split(line);
  }

  private boolean containField(List<Field> fields, String fieldName) {
    for(Field field : fields) {
      if(field.getName().equals(fieldName)) {
        return true;
      }
    }
    return false;
  }
}
