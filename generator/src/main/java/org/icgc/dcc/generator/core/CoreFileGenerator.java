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

  private final List<CodeListTerm> codeListArrayList = new ArrayList<CodeListTerm>();

  private final MutableLong uniqueId = new MutableLong(0L);

  private final MutableInt uniqueInteger = new MutableInt(0);

  private final MutableDouble uniqueDecimal = new MutableDouble(0.0);

  private DataGenerator datagen;

  public void createFile(DataGenerator datagen, FileSchema schema, Integer numberOfLinesPerPrimaryKey,
      String leadJurisdiction, String institution, String tumourType, String platform) throws IOException {

    this.datagen = datagen;

    String fileUrl =
        SubmissionUtils.generateCoreFileUrl(datagen.getOutputDirectory(), schema.getName(), leadJurisdiction,
            institution, tumourType, platform);
    File outputFile = new File(fileUrl);
    checkArgument(!outputFile.exists(), "A file with the name '%s' already exists.", fileUrl);
    outputFile.createNewFile();

    FileOutputStream fos = new FileOutputStream(outputFile);
    OutputStreamWriter osw = new OutputStreamWriter(fos, Charsets.UTF_8);
    @Cleanup
    BufferedWriter writer = new BufferedWriter(osw);

    int counterForFieldNames = 0;
    for(String fieldName : schema.getFieldNames()) {
      if(counterForFieldNames == schema.getFields().size() - 1) {
        writer.write(fieldName);
      } else {
        writer.write(fieldName + TAB);
      }
      counterForFieldNames++;
    }

    populateCodeListArray(schema);

    writer.write(NEW_LINE);

    log.info("Populating " + schema.getName() + " file");
    populateFile(schema, numberOfLinesPerPrimaryKey, writer);
    log.info("Finished populating " + schema.getName() + " file");
    writer.close();

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

  public void populateFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, BufferedWriter writer)
      throws IOException {

    String schemaName = schema.getName();
    List<Relation> relations = schema.getRelations();

    int numberOfPrimaryKeyValues = calcuateNumberOfPrimaryKeyValues(schema, numberOfLinesPerPrimaryKey, relations);
    int numberOfLinesPerKey = calculateNumberOfLinesPerKey(schema, numberOfLinesPerPrimaryKey, relations);

    if(schemaName.equals(SAMPLE_SCHEMA_NAME)) {
      PrimaryKey tumourPrimaryKey = new PrimaryKey(SAMPLE_SCHEMA_NAME, TUMOUR_PRIMARY_KEY_FIELD_IDENTIFIER);
      datagen.getListOfPrimaryKeys().add(tumourPrimaryKey);

      PrimaryKey controlPrimaryKey = new PrimaryKey(SAMPLE_SCHEMA_NAME, CONTROL_PRIMARY_KEY_FIELD_IDENTIFIER);
      datagen.getListOfPrimaryKeys().add(controlPrimaryKey);
    }

    for(int i = 0; i < numberOfPrimaryKeyValues; i++) {
      for(int j = 0; j < numberOfLinesPerKey; j++) {
        int counterForFields = 0;
        for(Field field : schema.getFields()) {
          String output = null;
          String fieldName = field.getName();

          List<String> foreignKeyArray = DataGenerator.getForeignKey(datagen, schema, fieldName);

          if(foreignKeyArray != null) {
            output = foreignKeyArray.get(i);
          } else {
            output = getFieldValue(schema, schemaName, field, fieldName);
          }

          if(ResourceWrapper.isUniqueField(schema.getUniqueFields(), fieldName)) {
            DataGenerator.getPrimaryKey(datagen, schemaName, fieldName).add(output);
          }
          // Special case for sample, to add whether sample type is controlled or tumour
          if(schema.getName().equals(SAMPLE_SCHEMA_NAME) && field.getName().equals(SAMPLE_TYPE_FIELD_NAME)) {

            int x = datagen.randomIntGenerator(0, 1);
            // Instead here you could check if output(which will be the value of analyzed_sample_type) = 'c' then
            // control one or, if output = 't' then go to control two
            if(x == 0) {
              DataGenerator.getPrimaryKey(datagen, SAMPLE_SCHEMA_NAME, TUMOUR_PRIMARY_KEY_FIELD_IDENTIFIER).add(output);
            } else {
              DataGenerator.getPrimaryKey(datagen, SAMPLE_SCHEMA_NAME, CONTROL_PRIMARY_KEY_FIELD_IDENTIFIER)
                  .add(output);
            }
          }
          if(schema.getFields().size() - 1 == counterForFields) {
            writer.write(output);
          } else {
            writer.write(output + TAB);
          }
          counterForFields++;
        }
        writer.write(NEW_LINE);
      }
      numberOfLinesPerKey = calculateNumberOfLinesPerKey(schema, numberOfLinesPerPrimaryKey, relations);
    }
  }

  /**
   * @param schema
   * @param numberOfLinesPerPrimaryKey
   * @param relations
   * @return
   */
  private int calcuateNumberOfPrimaryKeyValues(FileSchema schema, Integer numberOfLinesPerPrimaryKey,
      List<Relation> relations) {
    int numberOfPrimaryKeyValues;
    if(schema.getName().equals(DONOR_SCHEMA_NAME)) {
      numberOfPrimaryKeyValues = numberOfLinesPerPrimaryKey;
    } else {
      numberOfPrimaryKeyValues =
          DataGenerator.getForeignKey(datagen, schema, relations.get(0).getFields().get(0)).size() - 2;
    }
    return numberOfPrimaryKeyValues;
  }

  /**
   * @param schema
   * @param numberOfLinesPerPrimaryKey
   * @param relations
   * @return
   */
  private int calculateNumberOfLinesPerKey(FileSchema schema, Integer numberOfLinesPerPrimaryKey,
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
   * @param currentFieldName
   * @return
   */
  private String getFieldValue(FileSchema schema, String schemaName, Field currentField, String currentFieldName) {
    String output = null;
    if(codeListArrayList.size() > 0) {
      for(CodeListTerm codeListTerm : codeListArrayList) {
        if(codeListTerm.getFieldName().equals(currentFieldName)) {
          List<Term> terms = codeListTerm.getTerms();
          output = terms.get(datagen.randomIntGenerator(0, terms.size() - 1)).getCode();
        }
      }
    }
    if(output == null) {
      output =
          DataGenerator.generateFieldValue(datagen, schema.getUniqueFields(), schemaName, currentField, uniqueId,
              uniqueInteger, uniqueDecimal);
    }
    return output;
  }

}
