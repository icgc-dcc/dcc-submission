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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.mutable.MutableDouble;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang.mutable.MutableLong;
import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.Relation;
import org.icgc.dcc.dictionary.model.Restriction;
import org.icgc.dcc.dictionary.model.Term;
import org.icgc.dcc.generator.model.CodeListTerm;
import org.icgc.dcc.generator.utils.ResourceWrapper;
import org.icgc.dcc.generator.utils.SubmissionUtils;

import com.fasterxml.jackson.databind.MappingIterator;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Resources;

@Slf4j
public class PrimaryFileGenerator {

  private static final String TAB = "\t";

  private static final String NEW_LINE = "\n";

  private static final String CODELIST_RESTRICTION_NAME = "codelist";

  private static final String SSM_SCHEMA_NAME = "ssm_p";

  private static final String SIMULATED_DATA_FILE_URL = "org/icgc/dcc/generator/ssmp_simulated.txt";

  private DataGenerator datagen;

  private final List<CodeListTerm> codeListArrayList = new ArrayList<CodeListTerm>();

  private final Set<String> simulatedData = new HashSet<String>(Arrays.asList("mutation_type", "chromosome",
      "chromosome_start", "chromosome_end", "reference_genome_allele", "control_genotype", "tumour_genotype mutation"));

  private final MutableLong uniqueId = new MutableLong(0L);

  private final MutableInt uniqueInteger = new MutableInt(0);

  private final MutableDouble uniqueDecimal = new MutableDouble(0.0);

  public void createFile(DataGenerator datagen, FileSchema schema, Integer linesPerForeignKey, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException {

    this.datagen = datagen;

    @Cleanup
    BufferedWriter writer = prepareFile(datagen, schema, leadJurisdiction, institution, tumourType, platform);

    // Output field names (eliminate trailing tab)
    populateFileHeader(schema, writer);

    populateCodeListArray(schema);

    log.info("Populating " + schema.getName() + " file");
    populateFile(schema, linesPerForeignKey, writer);
    log.info("Finished populating " + schema.getName() + " file");

    writer.close();
  }

  /**
   * @param schema
   * @param writer
   * @throws IOException
   */
  private void populateFileHeader(FileSchema schema, BufferedWriter writer) throws IOException {
    int counterForFieldNames = 0;
    for(String fieldName : schema.getFieldNames()) {
      if(counterForFieldNames == schema.getFields().size() - 1) {
        writer.write(fieldName);
      } else {
        writer.write(fieldName + TAB);
      }
      counterForFieldNames++;
    }
    writer.write(NEW_LINE);
  }

  /**
   * @param datagen
   * @param schema
   * @param leadJurisdiction
   * @param institution
   * @param tumourType
   * @param platform
   * @return
   * @throws IOException
   * @throws FileNotFoundException
   */
  private BufferedWriter prepareFile(DataGenerator datagen, FileSchema schema, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException, FileNotFoundException {
    // File building
    String fileUrl =
        SubmissionUtils.generateExperimentalFileUrl(datagen.getOutputDirectory(), schema.getName(), leadJurisdiction,
            institution, tumourType, platform);
    File outputFile = new File(fileUrl);
    checkArgument(!outputFile.exists(), "A file with the name '%s' already exists.", fileUrl);
    outputFile.createNewFile();

    // Prepare file writer
    FileOutputStream fos = new FileOutputStream(outputFile);
    OutputStreamWriter osw = new OutputStreamWriter(fos, Charsets.UTF_8);
    @Cleanup
    BufferedWriter writer = new BufferedWriter(osw);
    return writer;
  }

  /**
   * @param schema
   */
  private void populateCodeListArray(FileSchema schema) {
    for(Field field : schema.getFields()) {
      Optional<Restriction> restriction = field.getRestriction(CODELIST_RESTRICTION_NAME);
      if(restriction.isPresent()) {
        String codeListName = restriction.get().getConfig().getString("name");
        MappingIterator<CodeList> iterator = ResourceWrapper.getCodeLists();
        while(iterator.hasNext()) {
          CodeList codeList = iterator.next();
          if(codeList.getName().equals(codeListName)) {
            CodeListTerm term = new CodeListTerm(field.getName(), codeList.getTerms());
            codeListArrayList.add(term);
          }
        }
      }
    }
  }

  public void populateFile(FileSchema schema, Integer linesPerForeignKey, Writer writer) throws IOException {
    String schemaName = schema.getName();
    List<Relation> relations = schema.getRelations();

    List<String> lines = null;
    if(schemaName.equals(SSM_SCHEMA_NAME)) {
      lines = Resources.readLines(Resources.getResource(SIMULATED_DATA_FILE_URL), Charsets.UTF_8);
    }

    int lengthOfForeignKeys = calculateLengthOfForeignKeys(schema, relations);
    int numberOfLinesPerForeignKey = calculateNumberOfLinesPerForeignKey(schema, linesPerForeignKey, relations);

    for(int foreignKeyEntry = 0; foreignKeyEntry < lengthOfForeignKeys; foreignKeyEntry++) {
      for(int foreignKeyEntryLineNumber = 0; foreignKeyEntryLineNumber < numberOfLinesPerForeignKey; foreignKeyEntryLineNumber++) {
        int counterForFields = 0;
        MutableInt nextTabIndex = new MutableInt(0);
        String line = lines.get(datagen.randomIntGenerator(0, lines.size() - 1));// This read in the file

        for(Field field : schema.getFields()) {
          String output = getFieldValue(schema, schemaName, foreignKeyEntry, nextTabIndex, line, field);

          // Write output, eliminate trailing tabs
          if(schema.getFields().size() - 1 == counterForFields) {
            writer.write(output);
          } else {
            writer.write(output + TAB);
          }
          counterForFields++;
        }
        writer.write(NEW_LINE);
      }
      numberOfLinesPerForeignKey = calculateNumberOfLinesPerForeignKey(schema, linesPerForeignKey, relations);
    }
  }

  /**
   * @param schema
   * @param schemaName
   * @param foreignKeyEntry
   * @param nextTabIndex
   * @param line
   * @param field
   * @return
   */
  private String getFieldValue(FileSchema schema, String schemaName, int foreignKeyEntry, MutableInt nextTabIndex,
      String line, Field field) {
    String output = null;
    String fieldName = field.getName();

    // Output foreign key if current field is to be populated with one
    List<String> foreignKeyArray = DataGenerator.getForeignKeys(datagen, schema, fieldName);
    if(foreignKeyArray != null) {
      output = foreignKeyArray.get(foreignKeyEntry);
    } else {
      if(schemaName.equals(SSM_SCHEMA_NAME) && simulatedData.contains(fieldName)) {// This prints out if true
        output = line.substring(nextTabIndex.intValue(), line.indexOf(TAB, nextTabIndex.intValue()));
        nextTabIndex.add(output.length() + 1);
      } else {
        output = getCodeListValue(schema, schemaName, field, fieldName);
      }
    }
    if(output == null) {
      output =
          DataGenerator.generateFieldValue(datagen, schema.getUniqueFields(), schemaName, field, uniqueId,
              uniqueInteger, uniqueDecimal);
    }

    // Add output to primary keys if it is to be used as a foreign key else where
    if(ResourceWrapper.isUniqueField(schema.getUniqueFields(), fieldName)) {
      DataGenerator.getPrimaryKeys(datagen, schemaName, fieldName).add(output);
    }
    return output;
  }

  /**
   * Calculates the number Of non-repetitive entries (with regards to the foreign key fields) to be inserted in the file
   * @param schema
   * @param relations
   * @return
   */
  private int calculateLengthOfForeignKeys(FileSchema schema, List<Relation> relations) {
    Relation randomRelation = relations.get(0);
    String relatedFieldName = randomRelation.getFields().get(0);
    int lengthOfForeignKeys = DataGenerator.getForeignKeys(datagen, schema, relatedFieldName).size() - 2;
    return lengthOfForeignKeys;
  }

  /**
   * Calculates the number of times a file entry repeats with regards to the foreign key
   * @param schema
   * @param linesPerForeignKey
   * @param relations
   * @return
   */
  private int calculateNumberOfLinesPerForeignKey(FileSchema schema, Integer linesPerForeignKey,
      List<Relation> relations) {
    if(relations.size() > 0 && relations.get(0).isBidirectional()) {
      return datagen.randomIntGenerator(1, linesPerForeignKey);
    } else {
      return datagen.randomIntGenerator(0, linesPerForeignKey);
    }
  }

  /**
   * @param schema
   * @param schemaName
   * @param indexOfCodeListArray
   * @param currentField
   * @param currentFieldName
   * @return
   */
  private String getCodeListValue(FileSchema schema, String schemaName, Field currentField, String currentFieldName) {
    String output = null;
    if(codeListArrayList.size() > 0) {
      for(CodeListTerm codeListTerm : codeListArrayList) {
        if(codeListTerm.getFieldName().equals(currentFieldName)) {
          List<Term> terms = codeListTerm.getTerms();
          output = terms.get(datagen.randomIntGenerator(0, terms.size() - 1)).getCode();

        }
      }
    }
    return output;
  }
}
