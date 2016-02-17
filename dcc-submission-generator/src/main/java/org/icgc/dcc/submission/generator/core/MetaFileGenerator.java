/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.generator.core;

import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.JCN_M_TYPE;
import static org.icgc.dcc.submission.generator.utils.Dictionaries.SAMPLE_SCHEMA_NAME;
import static org.icgc.dcc.submission.generator.utils.Dictionaries.isUniqueField;

import java.io.File;
import java.util.List;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Relation;
import org.icgc.dcc.submission.generator.model.Project;
import org.supercsv.io.CsvListWriter;

@Slf4j
public class MetaFileGenerator extends BaseFileGenerator {

  /**
   * Field name constants.
   */
  private static final String MATCHED_SAMPLE_FIELD_NAME = "matched_sample_id";

  private static final String TUMOUR_KEY_FIELD_IDENTIFIER = "tumourSampleTypeID";
  private static final String CONTROL_KEY_FIELD_IDENTIFIER = "controlledSampleTypeID";

  public MetaFileGenerator(FileSchema schema, DataGenerator generator) {
    super(schema, generator);
  }

  @SneakyThrows
  public File createFile(String outputDirectory, FileSchema schema, Integer linesPerForeignKey, Project project) {
    File outputFile = getExperimentalFile(outputDirectory, schema.getName(), project);

    @Cleanup
    CsvListWriter writer = buildFileWriter(outputFile);

    generator.addCodeListTerms(schema);
    generator.addKeyAppliedFields(schema);

    log.info("Populating {} file", schema.getName());
    populateFile(schema, linesPerForeignKey, writer);
    generator.resetUniqueValueFields();
    log.info("Finished populating {} file ", schema.getName());

    return outputFile;
  }

  @SneakyThrows
  private void populateFile(FileSchema schema, Integer linesPerForeignKey, CsvListWriter writer) {
    List<String> header = getFileHeader(schema);
    writer.write(header);

    List<Relation> schemaRelations = schema.getRelations();

    logNumberOfLines(linesPerForeignKey, schemaRelations);

    int sampleCount = calculateLengthOfForeignKeys(schema);
    for (int i = 0; i < sampleCount; i++) {
      int sampleAnalysisCount = calculateNumberOfLinesPerForeignKey(schemaRelations, linesPerForeignKey);
      for (int j = 0; j < sampleAnalysisCount; j++) {
        val record = createRecord();
        for (val field : schema.getFields()) {
          String value = getFieldValue(schema, i, field);
          record.add(value);
        }

        writer.write(record);
      }
    }
  }

  private String getFieldValue(FileSchema schema, int foreignKeyEntry, Field field) {
    String schemaName = schema.getName();
    String fieldName = field.getName();
    String fieldValue = null;

    List<Integer> foreignKeys = generator.getForeignKeyValues(schemaName, fieldName);
    if (foreignKeys != null) {
      fieldValue =
          FOREIGN_KEY_PREFIX + (isSystemMetaFile(schemaName) ?
              getSampleType(fieldName) :
              foreignKeys.get(foreignKeyEntry));
    } else {
      fieldValue = generator.getFieldValue(schemaName, field, schema.getUniqueFields());
    }

    if (isUniqueField(schema.getUniqueFields(), fieldName)) {
      generator.getPrimaryKeyValues(schemaName, fieldName).add(Integer.parseInt(fieldValue));
    }

    return fieldValue;
  }

  /**
   * Returns true if the schema name passed in is not a junction schema. All other schemas have a sample type field.
   */
  private boolean isSystemMetaFile(String schemaName) {
    return (schemaName.equals(JCN_M_TYPE.getId()) == false);
  }

  private int getSampleType(String fieldName) {
    val matched = fieldName.equals(MATCHED_SAMPLE_FIELD_NAME);
    val identifier = matched ? TUMOUR_KEY_FIELD_IDENTIFIER : CONTROL_KEY_FIELD_IDENTIFIER;
    val sampleTypes = generator.getPrimaryKeyValues(SAMPLE_SCHEMA_NAME, identifier);
    checkState(!sampleTypes.isEmpty(), "No sample type value for field '%s' with sample type identifier '%s'",
        fieldName, identifier);

    Integer randomSampleType = generator.generateRandomElement(sampleTypes);
    return randomSampleType;
  }
}
