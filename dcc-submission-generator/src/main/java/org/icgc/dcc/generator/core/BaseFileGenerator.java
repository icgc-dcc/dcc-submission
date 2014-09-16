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
import static org.icgc.dcc.generator.utils.SubmissionFiles.generateFileName;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.generator.model.Project;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Relation;
import org.supercsv.io.CsvListWriter;

/**
 * Super class for all file generating classes. Utilized mainly to reuse common code.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseFileGenerator {

  protected static final String FOREIGN_KEY_PREFIX = "00";

  @NonNull
  final FileSchema schema;
  @NonNull
  final DataGenerator generator;

  public List<String> getFileHeader(FileSchema schema) {
    List<String> header = newArrayList();
    for (String fieldName : schema.getFieldNames()) {
      header.add(fieldName);
    }

    return header;
  }

  /**
   * Generates an experimental file with a valid name
   */
  @SneakyThrows
  public File getExperimentalFile(String outputDirectory, String schemaName, Project project) {
    String fileName = generateFileName(schema);

    File outputFile = new File(outputDirectory, fileName);
    checkArgument(!outputFile.exists(), "An experimental file with the name '%s' already exists.", fileName);
    outputFile.createNewFile();

    return outputFile;
  }

  /**
   * Generates a clinical file with a valid name
   */
  @SneakyThrows
  public File getClinicalFile(String outputDirectory, String schemaName, Project project) {
    val fileName = generateFileName(schema);

    File outputFile = new File(outputDirectory, fileName);
    checkArgument(!outputFile.exists(), "A clinical file with the name '%s' already exists.", fileName);
    outputFile.createNewFile();

    return outputFile;
  }

  /**
   * Builds a CSV List Writer using a tab field separator (aka TSV).
   */
  @SneakyThrows
  public CsvListWriter buildFileWriter(File outputFile) {
    return new CsvListWriter(new FileWriter(outputFile), TAB_PREFERENCE);
  }

  /**
   * Calculates the number Of non-repetitive entries (with regards to the foreign key fields) to be inserted in the file
   */
  public int calculateLengthOfForeignKeys(FileSchema schema) {
    for (val field : schema.getFields()) {
      val values = generator.getForeignKeyValues(schema.getName(), field.getName());

      if (values != null) {
        return values.size();
      }
    }

    return -1;
  }

  /**
   * Calculates the number of times a file entry repeats with regards to the foreign key
   */
  public int calculateNumberOfLinesPerForeignKey(List<Relation> relations, Integer maxLineCount) {
    int firstRelation = 0; // Doesn't matter which relation. assumption is there is at least one relation
    Relation randomRelation = relations.get(firstRelation); // If one relation is bidirectional, assumption is both are

    boolean required = !relations.isEmpty() && randomRelation.isBidirectional();
    int minLineCount = required ? 1 : 0;
    int lineCount = generator.generateRandomInteger(minLineCount, maxLineCount);

    return lineCount;
  }

  /**
   * Outputs the number of lines that are being generated on average for each foreign key
   */
  public void logNumberOfLines(int linesPerForeignKey, List<Relation> schemaRelations) {
    int firstRelation = 0; // Doesn't matter which relation. assumption is there is at least one relation
    Relation randomRelation = schemaRelations.get(firstRelation);
    long averageLineCount = Math.round(linesPerForeignKey / 2.0);

    log.info("Generating on average {} lines per each relation to the {} file", averageLineCount,
        randomRelation.getOther());
  }

  protected String resolveFieldValue(FileSchema schema, Field field, int foreignKeyEntry) {
    List<Integer> foreignKeys = generator.getForeignKeyValues(schema.getName(), field.getName());
    if (foreignKeys != null) {
      return FOREIGN_KEY_PREFIX + foreignKeys.get(foreignKeyEntry);
    }

    return generator.getFieldValue(schema.getName(), field, schema.getUniqueFields());
  }

  protected List<String> createRecord() {
    return newArrayList();
  }

}
