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

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.generator.model.Project;
import org.supercsv.io.CsvListWriter;

@Slf4j
public class DonorFileGenerator extends BaseFileGenerator {

  public DonorFileGenerator(FileSchema schema, DataGenerator generator) {
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
    log.info("Finished populating {} file", schema.getName());

    generator.resetUniqueValueFields();

    return outputFile;
  }

  @SneakyThrows
  private void populateFile(FileSchema schema, int donorCount, CsvListWriter writer) {
    List<String> header = getFileHeader(schema);
    writer.write(header);

    for (int i = 0; i < donorCount; i++) {
      val record = createRecord();
      for (val field : schema.getFields()) {
        String value = getFieldValue(schema, field);
        record.add(value);
      }

      writer.write(record);
    }
  }

  private String getFieldValue(FileSchema schema, Field field) {
    String schemaName = schema.getName();
    String fieldName = field.getName();

    String fieldValue = generator.getFieldValue(schemaName, field, schema.getUniqueFields());

    if (isUniqueField(schema.getUniqueFields(), fieldName)) {
      val primaryKeyValues = generator.getPrimaryKeyValues(schemaName, fieldName);

      int integer = Integer.parseInt(fieldValue);
      primaryKeyValues.add(integer);
    }

    return fieldValue;
  }

}
