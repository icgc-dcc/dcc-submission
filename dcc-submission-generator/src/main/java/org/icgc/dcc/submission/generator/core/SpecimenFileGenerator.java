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
package org.icgc.dcc.submission.generator.core;

import static org.icgc.dcc.submission.generator.utils.Dictionaries.DONOR_SCHEMA_NAME;
import static org.icgc.dcc.submission.generator.utils.Dictionaries.SPECIMEN_SCHEMA_NAME;
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
import org.icgc.dcc.submission.generator.model.Key;
import org.icgc.dcc.submission.generator.model.Project;
import org.supercsv.io.CsvListWriter;

@Slf4j
public class SpecimenFileGenerator extends BaseFileGenerator {

  /**
   * Field name constants.
   */
  private static final String DONOR_ID_FIELD_NAME = "donor_id";

  private static final String OPTIONAL_FILE_DONOR_ID_IDENTIFIER = "optionalFileDonorId";

  public SpecimenFileGenerator(FileSchema schema, DataGenerator generator) {
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

    List<Relation> schemaRelations = schema.getRelations();

    logNumberOfLines(linesPerForeignKey, schemaRelations);

    addSpecimenKeys();

    int donorCount = calculateLengthOfForeignKeys(schema);
    for (int i = 0; i < donorCount; i++) {
      int donorSpecimenCount = calculateNumberOfLinesPerForeignKey(schemaRelations, linesPerForeignKey);
      for (int j = 0; j < donorSpecimenCount; j++) {
        val record = createRecord();
        for (val field : schema.getFields()) {
          String value = getFieldValue(schema, field, i);
          record.add(value);
        }

        writer.write(record);
      }
    }
  }

  private String getFieldValue(FileSchema schema, Field field, int foreignKeyIndex) {
    String schemaName = schema.getName();
    String fieldName = field.getName();
    String fieldValue = resolveFieldValue(schema, field, foreignKeyIndex);

    if (isOptionlDonorIdField(schema, field)) {
      // A special case for optional files. The following adds the donor id at the same index as the specimen_id, but in
      // a different list
      generator.addFieldValueToKey(DONOR_SCHEMA_NAME, OPTIONAL_FILE_DONOR_ID_IDENTIFIER, Integer.parseInt(fieldValue));
    }

    if (isUniqueField(schema.getUniqueFields(), fieldName)) {
      generator.getPrimaryKeyValues(schemaName, fieldName).add(Integer.parseInt(fieldValue));
    }

    return fieldValue;
  }

  private boolean isOptionlDonorIdField(FileSchema schema, Field field) {
    return schema.getName().equals(SPECIMEN_SCHEMA_NAME) && field.getName().equals(DONOR_ID_FIELD_NAME);
  }

  private void addSpecimenKeys() {
    Key optionalDonorId = new Key(DONOR_SCHEMA_NAME, OPTIONAL_FILE_DONOR_ID_IDENTIFIER);
    generator.addKey(optionalDonorId);
  }

}
