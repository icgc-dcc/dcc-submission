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

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Iterables.cycle;
import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.readLines;

import java.io.File;
import java.util.Iterator;
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
public class SecondaryFileGenerator extends BaseFileGenerator {

  /**
   * File parsing constants.
   */
  private static final String SYSTEM_FILE_FIELD_SEPERATOR = "\t";

  /**
   * File name constants.
   */
  private static final String HSAPIENS_SYSTEM_FILE_NAME = "org/icgc/dcc/generator/HsapSystemFile.txt";

  /**
   * Field name constants.
   */
  private static final String MIRBASE_ID_FIELD_NAME = "xref_mirbase_id";
  private static final String MIRNA_SEQUENCE_ID_FIELD_NAME = "mirna_seq";
  private static final String SECONDARY_GENE_FIELD_NAME = "gene_affected";
  private static final String SECONDARY_TRANSCRIPT_FIELD_NAME = "transcript_affected";

  public SecondaryFileGenerator(FileSchema schema, DataGenerator generator) {
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

    String schemaName = schema.getName();
    List<Relation> schemaRelations = schema.getRelations();

    logNumberOfLines(linesPerForeignKey, schemaRelations);

    Iterator<String> iterator = readSystemFile(schemaName);

    int lengthOfForeignKeys = calculateLengthOfForeignKeys(schema);
    for (int foreignKeyEntry = 0; foreignKeyEntry < lengthOfForeignKeys; foreignKeyEntry++) {
      int numberOfLinesPerForeignKey = calculateNumberOfLinesPerForeignKey(schemaRelations, linesPerForeignKey);
      for (int foreignKeyEntryLineNumber = 0; foreignKeyEntryLineNumber < numberOfLinesPerForeignKey; foreignKeyEntryLineNumber++) {
        String line = iterator.next();

        val record = createRecord();
        for (val field : schema.getFields()) {
          String value = getFieldValue(schema, field, foreignKeyEntry, line);
          record.add(value);
        }

        writer.write(record);
      }
    }
  }

  private String getFieldValue(FileSchema schema, Field field, int foreignKeyEntry, String line) {
    String fieldName = field.getName();
    String fieldValue = getSystemFileValue(fieldName, line);

    if (fieldValue == null) {
      fieldValue = resolveFieldValue(schema, field, foreignKeyEntry);
    }

    return fieldValue;
  }

  /**
   * Returns a value from one of the system files if applicable. Returns null otherwise.
   */
  private String getSystemFileValue(String currentFieldName, String line) {
    if (currentFieldName.equals(MIRBASE_ID_FIELD_NAME)) {
      return line.substring(0, line.indexOf(SYSTEM_FILE_FIELD_SEPERATOR));
    } else if (currentFieldName.equals(MIRNA_SEQUENCE_ID_FIELD_NAME)) {
      return line.substring(line.indexOf(SYSTEM_FILE_FIELD_SEPERATOR) + 1, line.length());
    } else if (currentFieldName.equals(SECONDARY_GENE_FIELD_NAME)) {
      return line.substring(0, line.indexOf(SYSTEM_FILE_FIELD_SEPERATOR));
    } else if (currentFieldName.equals(SECONDARY_TRANSCRIPT_FIELD_NAME)) {
      return line.substring(line.indexOf(SYSTEM_FILE_FIELD_SEPERATOR) + 1, line.length());
    }
    return null;
  }

  @SneakyThrows
  private Iterator<String> readSystemFile(String schemaName) {
    String fileName = HSAPIENS_SYSTEM_FILE_NAME;
    List<String> lines = readLines(getResource(fileName), UTF_8);

    // Randomize
    generator.generateRandomOrdering(lines);

    // Circular iterator
    Iterator<String> iterator = cycle(lines).iterator();

    return iterator;
  }
}
