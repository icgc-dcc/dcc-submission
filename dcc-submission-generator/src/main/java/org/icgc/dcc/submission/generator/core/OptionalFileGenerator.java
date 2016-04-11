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

import static org.icgc.dcc.submission.generator.utils.Dictionaries.isUniqueField;

import java.io.File;
import java.util.List;

import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Relation;
import org.icgc.dcc.submission.generator.model.Project;
import org.supercsv.io.CsvListWriter;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OptionalFileGenerator extends BaseFileGenerator {

  private static final String DONOR_SCHEMA_NAME = "donor";
  private static final String BIOMARKER_SCHEMA_NAME = "biomarker";
  private static final String SURGERY_SCHEMA_NAME = "surgery";

  private static final String DONOR_ID_FIELD_NAME = "donor_id";
  private static final String DONOR_ID_PRIMARY_KEY_FIELD_IDENTIFIER = "donor_id_for_optional_file";

  public OptionalFileGenerator(FileSchema schema, DataGenerator generator) {
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

    int lengthOfForeignKeys = calculateLengthOfForeignKeys(schema);
    for (int foreignKeyEntry = 0; foreignKeyEntry < lengthOfForeignKeys; foreignKeyEntry++) {
      int numberOfLinesPerForeignKey = calculateNumberOfLinesPerForeignKey(schemaRelations, linesPerForeignKey);
      for (int foreignKeyEntryLineNumber = 0; foreignKeyEntryLineNumber < numberOfLinesPerForeignKey; foreignKeyEntryLineNumber++) {
        val record = createRecord();
        for (val field : schema.getFields()) {
          String value = getFieldValue(schema, field, foreignKeyEntry);
          record.add(value);
        }

        writer.write(record);
      }
    }
  }

  private String getFieldValue(FileSchema schema, Field field, int foreignKeyEntry) {
    String fieldValue = null;

    String schemaName = schema.getName();
    String fieldName = field.getName();

    List<Integer> foreignKeys;
    // Special case where surgery and biomarker schemas require utilize both specimen and donor ids.
    if ((schemaName.equals(SURGERY_SCHEMA_NAME) || schemaName.equals(BIOMARKER_SCHEMA_NAME))
        && fieldName.equals(DONOR_ID_FIELD_NAME)) {
      foreignKeys = generator.getPrimaryKeyValues(DONOR_SCHEMA_NAME, DONOR_ID_PRIMARY_KEY_FIELD_IDENTIFIER);
    } else {
      foreignKeys = generator.getForeignKeyValues(schemaName, fieldName);
    }

    if (foreignKeys != null) {
      fieldValue = "00" + foreignKeys.get(foreignKeyEntry);
    } else {
      fieldValue = generator.getFieldValue(schemaName, field, schema.getUniqueFields());
    }

    if (isUniqueField(schema.getUniqueFields(), fieldName)) {
      generator.getPrimaryKeyValues(schemaName, fieldName).add(Integer.parseInt(fieldValue));
    }
    return fieldValue;
  }
}
