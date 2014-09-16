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

import static org.icgc.dcc.generator.utils.Dictionaries.SAMPLE_SCHEMA_NAME;
import static org.icgc.dcc.generator.utils.Dictionaries.isUniqueField;

import java.io.File;
import java.util.List;
import java.util.Map;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.generator.model.Key;
import org.icgc.dcc.generator.model.Project;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Relation;
import org.supercsv.io.CsvListWriter;

import com.google.common.collect.ImmutableMap;

@Slf4j
public class SampleFileGenerator extends BaseFileGenerator {

  private static final String TUMOUR_KEY_FIELD_IDENTIFIER = "tumourSampleTypeID";
  private static final String CONTROL_KEY_FIELD_IDENTIFIER = "controlledSampleTypeID";

  private static final Map<String, Integer> ANALYZED_SAMPLE_TYPE_CODE = new ImmutableMap.Builder<String, Integer>()
      .put(CONTROL_KEY_FIELD_IDENTIFIER, 2)
      .put(TUMOUR_KEY_FIELD_IDENTIFIER, 1)
      .build();

  // Essentially the primary key called analyzed_sample_id is not used
  private static final String SAMPLE_TYPE_FIELD_NAME = "analyzed_sample_id";

  public SampleFileGenerator(FileSchema schema, DataGenerator generator) {
    super(schema, generator);
  }

  @SneakyThrows
  public File createFile(String outputDirectory, FileSchema schema, Integer linesPerForeignKey, Project project) {
    File outputFile = getClinicalFile(outputDirectory, schema.getName(), project);

    @Cleanup
    CsvListWriter writer = buildFileWriter(outputFile);

    generator.addCodeListTerms(schema);
    generator.addKeyAppliedFields(schema);

    log.info("Populating {} file", schema.getName());

    populateFile(schema, linesPerForeignKey, writer);
    generator.resetUniqueValueFields();

    log.info("Finished populating {} file", schema.getName());

    return outputFile;
  }

  @SneakyThrows
  private void populateFile(FileSchema schema, Integer linesPerForeignKey, CsvListWriter writer) {
    List<String> header = getFileHeader(schema);
    writer.write(header);

    String schemaName = schema.getName();
    List<Relation> schemaRelations = schema.getRelations();

    logNumberOfLines(linesPerForeignKey, schemaRelations);

    addSampleTypeKeys(schemaName);

    int specimenCount = calculateLengthOfForeignKeys(schema);
    for (int i = 0; i < specimenCount; i++) {
      int specimenSampleCount = calculateNumberOfLinesPerForeignKey(schemaRelations, linesPerForeignKey);
      for (int j = 0; j < specimenSampleCount / 2; j++) {
        String[] sampleTypes = { TUMOUR_KEY_FIELD_IDENTIFIER, CONTROL_KEY_FIELD_IDENTIFIER };
        for (String sampleType : sampleTypes) {
          val record = createRecord();
          for (val field : schema.getFields()) {
            String value = getFieldValue(schema, field, i, sampleType);
            record.add(value);
          }

          writer.write(record);
        }
      }
    }
  }

  private String getFieldValue(FileSchema schema, Field field, int i, String sampleType) {
    String schemaName = schema.getName();
    String fieldName = field.getName();
    String fieldValue = resolveFieldValue(schema, field, i);

    if (isSampleTypeField(schema, field)) {
      fieldValue = FOREIGN_KEY_PREFIX + appendSampleKeys(schema, field, fieldValue, sampleType);
    }

    if (isUniqueField(schema.getUniqueFields(), fieldName)) {
      generator.getPrimaryKeyValues(schemaName, fieldName).add(Integer.parseInt(fieldValue));
    }

    return fieldValue;
  }

  private boolean isSampleTypeField(FileSchema schema, Field field) {
    return schema.getName().equals(SAMPLE_SCHEMA_NAME) && field.getName().equals(SAMPLE_TYPE_FIELD_NAME);
  }

  /**
   * Add sample type keys to track sample types independently.
   */
  private void addSampleTypeKeys(String schemaName) {
    Key tumourPrimaryKey = new Key(SAMPLE_SCHEMA_NAME, TUMOUR_KEY_FIELD_IDENTIFIER);
    generator.addKey(tumourPrimaryKey);

    Key controlPrimaryKey = new Key(SAMPLE_SCHEMA_NAME, CONTROL_KEY_FIELD_IDENTIFIER);
    generator.addKey(controlPrimaryKey);
  }

  /**
   * Appends a field with either a 1 or a 2 to represent tumour or control respectively
   * @param k
   */
  private String appendSampleKeys(FileSchema schema, Field field, String fieldValue, String sampleType) {
    int value = appendFileIdentifierSuffix(fieldValue, sampleType);

    generator.addFieldValueToKey(SAMPLE_SCHEMA_NAME, sampleType, value);

    return Integer.toString(value);
  }

  private int appendFileIdentifierSuffix(String fieldValue, String identifier) {
    Integer suffix = ANALYZED_SAMPLE_TYPE_CODE.get(identifier);
    String combined = fieldValue + suffix;

    return Integer.parseInt(combined);
  }

}
