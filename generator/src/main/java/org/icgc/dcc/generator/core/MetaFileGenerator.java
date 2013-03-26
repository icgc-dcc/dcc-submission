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

@Slf4j
public class MetaFileGenerator {

  private static final String FIELD_SEPERATOR = "\t";

  private static final String LINE_SEPERATOR = "\n";

  private static final String SAMPLE_SCHEMA_NAME = "sample";

  private static final String NON_SYSTEM_META_FILE_EXPRESSION = "exp_m";

  private static final String NON_SYSTEM_META_FILE_JUNCTION = "jcn_m";

  private static final String NON_SYSTEM_META_FILE_MIRNA = "mirna_m";

  private static final String TUMOUR_PRIMARY_KEY_FIELD_IDENTIFIER = "tumourSampleTypeID";

  private static final String CONTROL_PRIMARY_KEY_FIELD_IDENTIFIER = "controlledSampleTypeID";

  private static final String MATCHED_SAMPLE_FIELD_NAME = "matched_sample_id";

  private final List<CodeListTerm> codeListTerms = newArrayList();

  private final MutableLong uniqueId = new MutableLong(0L);

  private final MutableInt uniqueInteger = new MutableInt(0);

  private final MutableDouble uniqueDecimal = new MutableDouble(0.0);

  private DataGenerator datagen;

  public void createFile(DataGenerator datagen, ResourceWrapper resourceWrapper, FileSchema schema,
      Integer linesPerForeignKey, String leadJurisdiction, String institution, String tumourType, String platform)
      throws IOException {

    this.datagen = datagen;

    @Cleanup
    BufferedWriter writer = prepareFile(datagen, schema, leadJurisdiction, institution, tumourType, platform);

    // Output field names (eliminate trailing tab)
    populateFileHeader(resourceWrapper, schema, writer);

    datagen.populateTermList(resourceWrapper, schema, codeListTerms);

    log.info("Populating {} file", schema.getName());
    populateFile(resourceWrapper, schema, linesPerForeignKey, writer);
    log.info("Finished populating {} file ", schema.getName());

    writer.close();
  }

  private void populateFileHeader(ResourceWrapper resourceWrapper, FileSchema schema, BufferedWriter writer)
      throws IOException {
    int counterForFieldNames = 0;
    for(String fieldName : schema.getFieldNames()) {
      if(counterForFieldNames == schema.getFields().size() - 1) {
        writer.write(fieldName);
      } else {
        writer.write(fieldName + FIELD_SEPERATOR);
      }
      counterForFieldNames++;
    }
    writer.write(LINE_SEPERATOR);
  }

  private BufferedWriter prepareFile(DataGenerator datagen, FileSchema schema, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException, FileNotFoundException {
    // File building
    String fileUrl =
        SubmissionFileUtils.generateExperimentalFileUrl(datagen.getOutputDirectory(), schema.getName(),
            leadJurisdiction, institution, tumourType, platform);
    File outputFile = new File(fileUrl);
    checkArgument(outputFile.exists() == false, "A file with the name '%s' already exists.", fileUrl);
    outputFile.createNewFile();

    // Prepare file writer
    FileOutputStream fos = new FileOutputStream(outputFile);
    OutputStreamWriter osw = new OutputStreamWriter(fos, Charsets.UTF_8);

    return new BufferedWriter(osw);
  }

  private void populateFile(ResourceWrapper resourceWrapper, FileSchema schema, Integer linesPerForeignKey,
      Writer writer) throws IOException {
    String schemaName = schema.getName();
    List<Relation> relations = schema.getRelations();

    int lengthOfForeignKeys = calculatedLengthOfForeignKeys(schema, relations);
    int numberOfLinesPerForeignKey = calculateNumberOfLinesPerForeignKey(schema, linesPerForeignKey, relations);

    for(int foreignKeyEntry = 0; foreignKeyEntry < lengthOfForeignKeys; foreignKeyEntry++) {
      for(int foreignKeyEntryLineNumber = 0; foreignKeyEntryLineNumber < numberOfLinesPerForeignKey; foreignKeyEntryLineNumber++) {
        int counterForFields = 0;
        for(Field field : schema.getFields()) {
          String output = getFieldValue(schema, resourceWrapper, schemaName, foreignKeyEntry, field);

          // Eliminate trailing tab
          if(schema.getFields().size() - 1 == counterForFields) {
            writer.write(output);
          } else {
            writer.write(output + FIELD_SEPERATOR);
          }

          counterForFields++;
        }
        writer.write(LINE_SEPERATOR);
      }
      numberOfLinesPerForeignKey = calculateNumberOfLinesPerForeignKey(schema, linesPerForeignKey, relations);
    }
  }

  private String getFieldValue(FileSchema schema, ResourceWrapper resourceWrapper, String schemaName,
      int foreignKeyEntry, Field field) {
    String output = null;
    String fieldName = field.getName();

    // Output foreign key if current field is to be populated by a foreign key
    List<String> foreignKeyArray = DataGenerator.getForeignKeys(datagen, schema, fieldName);
    if(foreignKeyArray != null) {
      if(isSystemMetaFile(schemaName)) {
        output = getSampleType(fieldName);
      } else {
        output = foreignKeyArray.get(foreignKeyEntry);
      }
    } else {
      output = getCodeListValue(schema, schemaName, field, fieldName);
    }
    if(output == null) {
      output =
          DataGenerator.generateFieldValue(datagen, resourceWrapper, schema.getUniqueFields(), schemaName, field,
              uniqueId, uniqueInteger, uniqueDecimal);
    }

    // Add the output to the corresponding Primary Key if this primary key is a foreign key else where
    if(resourceWrapper.isUniqueField(schema.getUniqueFields(), fieldName)) {
      DataGenerator.getPrimaryKeys(datagen, schemaName, fieldName).add(output);
    }
    return output;
  }

  private boolean isSystemMetaFile(String schemaName) {
    boolean isNotMetaExpressionFile = (schemaName.equals(NON_SYSTEM_META_FILE_EXPRESSION) == false);
    boolean isNotMetaJunctionFile = (schemaName.equals(NON_SYSTEM_META_FILE_JUNCTION) == false);
    boolean isNotMetaMirnaFile = (schemaName.equals(NON_SYSTEM_META_FILE_MIRNA) == false);

    return (isNotMetaExpressionFile && isNotMetaJunctionFile && isNotMetaMirnaFile);
  }

  /**
   * Calculates the number Of non-repetitive entries (with regards to the foreign key fields) to be inserted in the file
   */
  private int calculatedLengthOfForeignKeys(FileSchema schema, List<Relation> relations) {
    Relation randomRelation = relations.get(0);
    String relatedFieldName = randomRelation.getFields().get(0);
    int lengthOfForeignKeys = DataGenerator.getForeignKeys(datagen, schema, relatedFieldName).size() - 2;
    return lengthOfForeignKeys;
  }

  /**
   * Calculates the number of times a file entry repeats with regards to the foreign key
   */
  private int calculateNumberOfLinesPerForeignKey(FileSchema schema, Integer linesPerForeignKey,
      List<Relation> relations) {
    if(relations.size() > 0 && relations.get(0).isBidirectional()) {
      return datagen.randomIntGenerator(1, linesPerForeignKey);
    } else {
      return datagen.randomIntGenerator(0, linesPerForeignKey);
    }
  }

  private String getSampleType(String currentFieldName) {
    if(currentFieldName.equals(MATCHED_SAMPLE_FIELD_NAME)) {
      List<String> tumourTypeIDs =
          DataGenerator.getPrimaryKeys(datagen, SAMPLE_SCHEMA_NAME, TUMOUR_PRIMARY_KEY_FIELD_IDENTIFIER);
      Integer randomInteger = datagen.randomIntGenerator(2, tumourTypeIDs.size() - 3);
      return tumourTypeIDs.get(randomInteger);
    } else {
      List<String> controlTypeIDs =
          DataGenerator.getPrimaryKeys(datagen, SAMPLE_SCHEMA_NAME, CONTROL_PRIMARY_KEY_FIELD_IDENTIFIER);
      Integer randomInteger = datagen.randomIntGenerator(2, controlTypeIDs.size() - 3);
      return controlTypeIDs.get(randomInteger);
    }
  }

  private String getCodeListValue(FileSchema schema, String schemaName, Field currentField, String currentFieldName) {
    String output = null;
    if(codeListTerms.isEmpty() == false) {
      for(CodeListTerm codeListTerm : codeListTerms) {
        if(codeListTerm.getFieldName().equals(currentFieldName)) {
          List<Term> terms = codeListTerm.getTerms();
          output = terms.get(datagen.randomIntGenerator(0, terms.size() - 1)).getCode();

        }
      }
    }

    return output;
  }

}
