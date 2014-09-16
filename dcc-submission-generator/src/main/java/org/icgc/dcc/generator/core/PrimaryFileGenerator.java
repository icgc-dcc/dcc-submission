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

import static com.beust.jcommander.internal.Lists.newArrayList;
import static com.google.common.collect.Iterables.cycle;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.io.Resources.getResource;
import static org.icgc.dcc.core.model.FileTypes.FileType.SSM_P_TYPE;
import static org.icgc.dcc.generator.utils.Dictionaries.isUniqueField;
import static org.supercsv.prefs.CsvPreference.TAB_PREFERENCE;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.generator.model.Project;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Relation;
import org.supercsv.io.CsvListWriter;
import org.supercsv.io.CsvMapReader;

@Slf4j
public class PrimaryFileGenerator extends BaseFileGenerator {

  /**
   * File name constants.
   */
  private static final String SIMULATED_DATA_FILE_URL = "org/icgc/dcc/generator/ssmp_simulated.txt";

  private static final Set<String> SIMULATED_FIELD_NAMES = newHashSet(
      "mutation_type",
      "chromosome",
      "chromosome_start",
      "chromosome_end",
      "reference_genome_allele",
      "control_genotype",
      "tumour_genotype",
      "mutation");

  public PrimaryFileGenerator(FileSchema schema, DataGenerator generator) {
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
    log.info("Finished populating {}", schema.getName());

    return outputFile;
  }

  @SneakyThrows
  private void populateFile(FileSchema schema, Integer linesPerForeignKey, CsvListWriter writer) {
    List<String> header = getFileHeader(schema);
    writer.write(header);

    String schemaName = schema.getName();
    List<Relation> schemaRelations = schema.getRelations();

    logNumberOfLines(linesPerForeignKey, schemaRelations);

    Iterator<Map<String, String>> iterator = null;
    if (isSimulatedSchema(schemaName)) {
      iterator = readSimulatedDataFile();
    }

    int lengthOfForeignKeys = calculateLengthOfForeignKeys(schema);
    for (int foreignKeyEntry = 0; foreignKeyEntry < lengthOfForeignKeys; foreignKeyEntry++) {
      int numberOfLinesPerForeignKey = calculateNumberOfLinesPerForeignKey(schemaRelations, linesPerForeignKey);
      for (int foreignKeyEntryLineNumber = 0; foreignKeyEntryLineNumber < numberOfLinesPerForeignKey; foreignKeyEntryLineNumber++) {
        Map<String, String> simulated = iterator != null ? iterator.next() : null;

        val record = createRecord();
        for (val field : schema.getFields()) {
          String value = getFieldValue(schema, field, foreignKeyEntry, simulated);
          record.add(value);
        }

        writer.write(record);
      }
    }
  }

  private String getFieldValue(FileSchema schema, Field field, int foreignKeyEntry, Map<String, String> simulated) {
    String schemaName = schema.getName();
    String fieldName = field.getName();
    String fieldValue = resolveFieldValue(schema, field, foreignKeyEntry);

    if (isSimulatedField(schemaName, fieldName)) {
      fieldValue = simulated.get(fieldName);
    }

    if (isUniqueField(schema.getUniqueFields(), fieldName)) {
      generator.getPrimaryKeyValues(schemaName, fieldName).add(Integer.parseInt(fieldValue));
    }

    return fieldValue;
  }

  private boolean isSimulatedField(String schemaName, String fieldName) {
    return isSimulatedSchema(schemaName) && SIMULATED_FIELD_NAMES.contains(fieldName);
  }

  private boolean isSimulatedSchema(String schemaName) {
    return schemaName.equals(SSM_P_TYPE.getId());
  }

  @SneakyThrows
  private Iterator<Map<String, String>> readSimulatedDataFile() {
    URL url = getResource(SIMULATED_DATA_FILE_URL);
    Reader input = new BufferedReader(new InputStreamReader(url.openStream()));

    log.info("Loading simulated data file from: '{}'...", url);
    @Cleanup
    CsvMapReader reader = new CsvMapReader(input, TAB_PREFERENCE);
    String[] header = reader.getHeader(true);

    List<Map<String, String>> records = newArrayList();
    Map<String, String> record = null;
    while ((record = reader.read(header)) != null) {
      records.add(record);
    }

    // Randomize
    generator.generateRandomOrdering(records);
    Iterator<Map<String, String>> iterator = cycle(records).iterator();

    return iterator;
  }
}
