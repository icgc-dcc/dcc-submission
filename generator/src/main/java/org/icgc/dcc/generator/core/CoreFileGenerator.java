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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

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
import org.icgc.dcc.generator.model.PrimaryKey;
import org.icgc.dcc.generator.utils.ResourceWrapper;
import org.icgc.dcc.generator.utils.SubmissionUtils;

import com.fasterxml.jackson.databind.MappingIterator;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;

@Slf4j
public class CoreFileGenerator {

  private static final String TAB = "\t";

  private static final String NEW_LINE = "\n";

  private static final String CODELIST_RESTRICTION_NAME = "codelist";

  private static final String DONOR_SCHEMA_NAME = "donor";

  private static final String SAMPLE_SCHEMA_NAME = "sample";

  private static final String TUMOUR_PRIMARY_KEY_FIELD_IDENTIFIER = "tumourSampleTypeID";

  private static final String CONTROL_PRIMARY_KEY_FIELD_IDENTIFIER = "controlledSampleTypeID";

  private static final String SAMPLE_TYPE_FIELD_NAME = "analyzed_sample_id";

  private final List<CodeListTerm> codeListTerms = new ArrayList<CodeListTerm>();

  private final MutableLong uniqueId = new MutableLong(0L);

  private final MutableInt uniqueInteger = new MutableInt(0);

  private final MutableDouble uniqueDecimal = new MutableDouble(0.0);

  private DataGenerator datagen;

  public void createFile(DataGenerator datagen, FileSchema schema, Integer linesPerForeignKey, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException {

    this.datagen = datagen;

    // File building
    String fileUrl =
        SubmissionUtils.generateCoreFileUrl(datagen.getOutputDirectory(), schema.getName(), leadJurisdiction,
            institution, tumourType, platform);
    File outputFile = new File(fileUrl);
    checkArgument(!outputFile.exists(), "A file with the name '%s' already exists.", fileUrl);
    outputFile.createNewFile();

    // Prepare file writer
    FileOutputStream fos = new FileOutputStream(outputFile);
    OutputStreamWriter osw = new OutputStreamWriter(fos, Charsets.UTF_8);
    @Cleanup
    BufferedWriter writer = new BufferedWriter(osw);

    // Output field names (eliminate trailing tab)
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

    populateTermList(schema);

    log.info("Populating " + schema.getName() + " file");
    populateFile(schema, linesPerForeignKey, writer);
    log.info("Finished populating " + schema.getName() + " file");

    writer.close();
  }

  /**
   * @param schema
   */
  private void populateTermList(FileSchema schema) {
    for(Field field : schema.getFields()) {
      Optional<Restriction> restriction = field.getRestriction(CODELIST_RESTRICTION_NAME);
      if(restriction.isPresent()) {
        String codeListName = restriction.get().getConfig().getString("name");
        MappingIterator<CodeList> iterator = ResourceWrapper.getCodeLists();

        while(iterator.hasNext()) {
          CodeList codeList = iterator.next();

          if(codeList.getName().equals(codeListName)) {
            CodeListTerm term = new CodeListTerm(field.getName(), codeList.getTerms());
            codeListTerms.add(term);
          }
        }
      }
    }
  }

  public void populateFile(FileSchema schema, Integer linesPerForeignKey, BufferedWriter writer) throws IOException {

    String schemaName = schema.getName();
    List<Relation> relations = schema.getRelations();

    int lengthOfForeignKeys = calcuateLengthOfForeignKeys(schema, linesPerForeignKey, relations);
    int numberOfLinesPerForeignKey = calculateNumberOfLinesPerForeingKey(schema, linesPerForeignKey, relations);

    addSamplePrimaryKeys(schemaName);

    for(int foreignKeyEntry = 0; foreignKeyEntry < lengthOfForeignKeys; foreignKeyEntry++) {
      for(int foreignKeyEntryLineNumber = 0; foreignKeyEntryLineNumber < numberOfLinesPerForeignKey; foreignKeyEntryLineNumber++) {
        int counterForFields = 0;
        for(Field field : schema.getFields()) {
          String output = getFieldValue(schema, writer, schemaName, foreignKeyEntry, counterForFields, field);

          if(schema.getFields().size() - 1 == counterForFields) {
            writer.write(output);
          } else {
            writer.write(output + TAB);
          }

          counterForFields++;
        }
        writer.write(NEW_LINE);
      }
      numberOfLinesPerForeignKey = calculateNumberOfLinesPerForeingKey(schema, linesPerForeignKey, relations);
    }
  }

  /**
   * @param schema
   * @param writer
   * @param schemaName
   * @param foreignKeyEntry
   * @param counterForFields
   * @param field
   * @throws IOException
   */
  private String getFieldValue(FileSchema schema, BufferedWriter writer, String schemaName, int foreignKeyEntry,
      int counterForFields, Field field) throws IOException {
    String output = null;
    String fieldName = field.getName();

    // Output foreign key if current field is to be populated a foreign key
    List<String> foreignKeys = DataGenerator.getForeignKeys(datagen, schema, fieldName);
    if(foreignKeys != null) {
      output = foreignKeys.get(foreignKeyEntry);
    }

    if(output == null) {
      output = getCodeListValue(schema, schemaName, field, fieldName);
    }
    if(output == null) {
      output =
          DataGenerator.generateFieldValue(datagen, schema.getUniqueFields(), schemaName, field, uniqueId,
              uniqueInteger, uniqueDecimal);
    }

    // Add the output to the corresponding Primary Key if this primary key is a foreign key else where
    if(ResourceWrapper.isUniqueField(schema.getUniqueFields(), fieldName)) {
      DataGenerator.getPrimaryKeys(datagen, schemaName, fieldName).add(output);
    }

    // Special case for sample, to add whether sample type is controlled or tumor
    addOutputToSamplePrimaryKeys(schema, field, output);

    // Eliminate trailing tab
    return output;
  }

  /**
   * @param schema
   * @param field
   * @param output
   */
  private void addOutputToSamplePrimaryKeys(FileSchema schema, Field field, String output) {
    if(schema.getName().equals(SAMPLE_SCHEMA_NAME) && field.getName().equals(SAMPLE_TYPE_FIELD_NAME)) {
      int x = datagen.randomIntGenerator(0, 1);
      if(x == 0) {
        DataGenerator.getPrimaryKeys(datagen, SAMPLE_SCHEMA_NAME, TUMOUR_PRIMARY_KEY_FIELD_IDENTIFIER).add(output);
      } else {
        DataGenerator.getPrimaryKeys(datagen, SAMPLE_SCHEMA_NAME, CONTROL_PRIMARY_KEY_FIELD_IDENTIFIER).add(output);
      }
    }
  }

  /**
   * @param schemaName
   */
  private void addSamplePrimaryKeys(String schemaName) {
    if(schemaName.equals(SAMPLE_SCHEMA_NAME)) {
      PrimaryKey tumourPrimaryKey = new PrimaryKey(SAMPLE_SCHEMA_NAME, TUMOUR_PRIMARY_KEY_FIELD_IDENTIFIER);
      datagen.getListOfPrimaryKeys().add(tumourPrimaryKey);

      PrimaryKey controlPrimaryKey = new PrimaryKey(SAMPLE_SCHEMA_NAME, CONTROL_PRIMARY_KEY_FIELD_IDENTIFIER);
      datagen.getListOfPrimaryKeys().add(controlPrimaryKey);
    }
  }

  /**
   * Calculates the number Of non-repetitive entries (with regards to the foreign key fields) to be inserted in the file
   * @param schema
   * @param numberOfLinesPerPrimaryKey
   * @param relations
   * @return
   */
  private int calcuateLengthOfForeignKeys(FileSchema schema, Integer numberOfLinesPerPrimaryKey,
      List<Relation> relations) {
    int lengthOfForeingKeys;
    if(schema.getName().equals(DONOR_SCHEMA_NAME)) {
      lengthOfForeingKeys = numberOfLinesPerPrimaryKey;
    } else {
      Relation randomRelation = relations.get(0);
      String relatedFieldName = randomRelation.getFields().get(0);
      lengthOfForeingKeys = DataGenerator.getForeignKeys(datagen, schema, relatedFieldName).size() - 2;
    }
    return lengthOfForeingKeys;
  }

  /**
   * Calculates the number of times a file entry repeats with regards to the foreign key
   * @param schema
   * @param numberOfLinesPerPrimaryKey
   * @param relations
   * @return
   */
  private int calculateNumberOfLinesPerForeingKey(FileSchema schema, Integer numberOfLinesPerPrimaryKey,
      List<Relation> relations) {
    if(schema.getName().equals(DONOR_SCHEMA_NAME)) {
      return 1;
    } else if(relations.size() > 0 && relations.get(0).isBidirectional()) {
      return datagen.randomIntGenerator(1, numberOfLinesPerPrimaryKey);
    } else {
      return datagen.randomIntGenerator(0, numberOfLinesPerPrimaryKey);
    }
  }

  /**
   * @param schema
   * @param schemaName
   * @param indexOfCodeListArray
   * @param currentField
   * @param fieldName
   * @return
   */
  private String getCodeListValue(FileSchema schema, String schemaName, Field currentField, String fieldName) {
    String output = null;
    if(!codeListTerms.isEmpty()) {
      for(CodeListTerm codeListTerm : codeListTerms) {
        if(codeListTerm.getFieldName().equals(fieldName)) {
          List<Term> terms = codeListTerm.getTerms();
          output = terms.get(datagen.randomIntGenerator(0, terms.size() - 1)).getCode();
        }
      }
    }
    return output;
  }

}
