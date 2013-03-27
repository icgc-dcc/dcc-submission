/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.generator.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.mutable.MutableDouble;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang.mutable.MutableLong;
import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.Relation;
import org.icgc.dcc.dictionary.model.Term;
import org.icgc.dcc.generator.model.CodeListTerm;
import org.icgc.dcc.generator.utils.ResourceWrapper;
import org.icgc.dcc.generator.utils.SubmissionFileUtils;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;

@Slf4j
public class SecondaryFileGenerator {

  private static final String FIELD_SEPERATOR = "\t";

  private static final String LINE_SEPERATOR = "\n";

  private static final String FILE_PATH_TOKEN_SEPERATOR = "/";

  private static final String MIRNA_MIRBASE_FILE_NAME = "org/icgc/dcc/generator/Mirna_MirbaseSystemFile.txt";

  private static final String HSAPIENS_SYSTEM_FILE_NAME = "org/icgc/dcc/generator/HsapSystemFile.txt";

  private static final String SECONDARY_MIRNA_SCHEMA_NAME = "mirna_s";

  private static final String MIRBASE_ID_FIELD_NAME = "xref_mirbase_id";

  private static final String MIRNA_SEQUENCE_ID_FIELD_NAME = "mirna_seq";

  private static final String SECONDARY_GENE_FIELD_NAME = "gene_affected";

  private static final String SECONDARY_TRANSCRIPT_FIELD_NAME = "transcript_affected";

  private final List<CodeListTerm> codeListTerms = newArrayList();

  private final MutableLong uniqueId = new MutableLong(0L);

  private final MutableInt uniqueInteger = new MutableInt(0);

  private final MutableDouble uniqueDouble = new MutableDouble(0.0);

  private final DataGenerator datagen;

  private final String outputDirectory;

  public SecondaryFileGenerator(DataGenerator datagen, String outputDirectory) {
    this.datagen = datagen;
    this.outputDirectory = outputDirectory;
  }

  public void createFile(ResourceWrapper resourceWrapper, FileSchema schema, Integer linesPerForeignKey,
      String leadJurisdiction, String institution, String tumourType, String platform) throws IOException {

    File outputFile = generateFileName(datagen, schema, leadJurisdiction, institution, tumourType, platform);
    @Cleanup
    Writer writer = buildFileWriter(outputFile);

    datagen.populateTermList(resourceWrapper, schema, codeListTerms);

    log.info("Populating {} file", schema.getName());
    populateFile(resourceWrapper, schema, linesPerForeignKey, writer);
    log.info("Finished populating {} file ", schema.getName());
  }

  private Writer buildFileWriter(File outputFile) throws FileNotFoundException {
    FileOutputStream fos = new FileOutputStream(outputFile);
    OutputStreamWriter osw = new OutputStreamWriter(fos, Charsets.UTF_8);

    return new BufferedWriter(osw);
  }

  private File generateFileName(DataGenerator datagen, FileSchema schema, String leadJurisdiction, String institution,
      String tumourType, String platform) throws IOException {
    String schemaName = schema.getName();
    String expName = schemaName.substring(0, schemaName.length() - 2);
    String expType = schemaName.substring(schemaName.length() - 1);
    List<String> fileNameTokens = newArrayList(expName, leadJurisdiction, tumourType, institution, expType, platform);

    String fileName = SubmissionFileUtils.generateFileName(fileNameTokens);
    String pathName = outputDirectory + FILE_PATH_TOKEN_SEPERATOR + fileName;
    File outputFile = new File(pathName);
    checkArgument(outputFile.exists() == false, "A file with the name '%s' already exists.", fileName);
    outputFile.createNewFile();
    return outputFile;
  }

  private void populateFileHeader(FileSchema schema, Writer writer) throws IOException {
    int counterForFields = 1;
    int numberOfFields = schema.getFields().size();
    for(String fieldName : schema.getFieldNames()) {
      writeFieldValue(writer, counterForFields, numberOfFields, fieldName);
      counterForFields++;
    }
    writer.write(LINE_SEPERATOR);
  }

  private void populateFile(ResourceWrapper resourceWrapper, FileSchema schema, Integer linesPerForeignKey,
      Writer writer) throws IOException {
    populateFileHeader(schema, writer);
    String schemaName = schema.getName();
    List<Relation> relations = schema.getRelations();

    List<String> lines = readSystemFiles(schemaName);
    Iterator<String> iterator = Iterables.cycle(lines).iterator();

    int lengthOfForeignKeys = calculatedLengthOfForeignKeys(schema, relations);
    int numberOfLinesPerForeignKey = calculateNumberOfLinesPerForeignKey(schema, linesPerForeignKey, relations);

    for(int foreignKeyEntry = 0; foreignKeyEntry < lengthOfForeignKeys; foreignKeyEntry++) {
      for(int foreignKeyEntryLineNumber = 0; foreignKeyEntryLineNumber < numberOfLinesPerForeignKey; foreignKeyEntryLineNumber++) {
        int counterForFields = 1;
        int numberOfFields = schema.getFields().size();

        String line = iterator.next();
        for(Field field : schema.getFields()) {
          String fieldValue = getFieldValue(resourceWrapper, schema, schemaName, foreignKeyEntry, line, field);

          writeFieldValue(writer, counterForFields, numberOfFields, fieldValue);

          counterForFields++;
        }
        writer.write(LINE_SEPERATOR);
      }
      numberOfLinesPerForeignKey = calculateNumberOfLinesPerForeignKey(schema, linesPerForeignKey, relations);
    }
  }

  private void writeFieldValue(Writer writer, int counterForFields, int numberOfFields, String fieldValue)
      throws IOException {
    if(counterForFields == numberOfFields) {
      writer.write(fieldValue);
    } else {
      writer.write(fieldValue + FIELD_SEPERATOR);
    }
  }

  private List<String> readSystemFiles(String schemaName) throws IOException {
    List<String> lines = null;
    if(schemaName.equals(SECONDARY_MIRNA_SCHEMA_NAME)) {
      lines = Resources.readLines(Resources.getResource(MIRNA_MIRBASE_FILE_NAME), Charsets.UTF_8);
    } else {
      lines = Resources.readLines(Resources.getResource(HSAPIENS_SYSTEM_FILE_NAME), Charsets.UTF_8);
    }
    return lines;
  }

  private String getFieldValue(ResourceWrapper resourceWrapper, FileSchema schema, String schemaName, int i,
      String line, Field field) {
    String fieldName = field.getName();

    String fieldValue = getSystemFileValue(fieldName, line);

    List<String> foreignKeys = DataGenerator.getForeignKeys(datagen, schema, fieldName);
    if(foreignKeys != null) {
      fieldValue = foreignKeys.get(i);
    }

    if(fieldValue == null) {
      fieldValue = getCodeListValue(schema, schemaName, field, fieldName);
    }

    if(fieldValue == null) {
      fieldValue =
          DataGenerator.generateFieldValue(datagen, resourceWrapper, schema.getUniqueFields(), schemaName, field,
              uniqueId, uniqueInteger, uniqueDouble);
    }

    return fieldValue;
  }

  /**
   * Calculates the number Of non-repetitive entries (with regards to the foreign key fields) to be inserted in the file
   */
  private int calculatedLengthOfForeignKeys(FileSchema schema, List<Relation> relations) {
    Relation randomRelation = relations.get(0);
    String relatedFieldName = randomRelation.getFields().get(0);
    int lengthOfForeignKeys = DataGenerator.getForeignKeys(datagen, schema, relatedFieldName).size();
    return lengthOfForeignKeys;
  }

  /**
   * Calculates the number of times a file entry repeats with regards to the foreign key
   */
  private int calculateNumberOfLinesPerForeignKey(FileSchema schema, Integer linesPerForeignKey,
      List<Relation> relations) {
    Relation randomRelation = relations.get(0);// If one relation is bidirectional, assumption is they both are
    if(relations.size() > 0 && randomRelation.isBidirectional()) {
      return datagen.generateRandomInteger(1, linesPerForeignKey);
    } else {
      return datagen.generateRandomInteger(0, linesPerForeignKey);
    }
  }

  public String getSystemFileValue(String currentFieldName, String line) {
    if(currentFieldName.equals(MIRBASE_ID_FIELD_NAME)) {
      return line.substring(0, line.indexOf(FIELD_SEPERATOR));
    } else if(currentFieldName.equals(MIRNA_SEQUENCE_ID_FIELD_NAME)) {
      return line.substring(line.indexOf(FIELD_SEPERATOR) + 1, line.length());
    } else if(currentFieldName.equals(SECONDARY_GENE_FIELD_NAME)) {
      return line.substring(0, line.indexOf(FIELD_SEPERATOR));
    } else if(currentFieldName.equals(SECONDARY_TRANSCRIPT_FIELD_NAME)) {
      return line.substring(line.indexOf(FIELD_SEPERATOR) + 1, line.length());
    }
    return null;
  }

  private String getCodeListValue(FileSchema schema, String schemaName, Field currentField, String currentFieldName) {
    String fieldValue = null;
    for(CodeListTerm codeListTerm : codeListTerms) {
      if(codeListTerm.getFieldName().equals(currentFieldName)) {
        List<Term> terms = codeListTerm.getTerms();
        fieldValue = terms.get(datagen.generateRandomInteger(0, terms.size())).getCode();
      }
    }
    return fieldValue;
  }

}
