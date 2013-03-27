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
import static com.google.common.collect.Sets.newHashSet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;

@Slf4j
public class PrimaryFileGenerator {

  private static final String FIELD_SEPERATOR = "\t";

  private static final String LINE_SEPERATOR = "\n";

  private static final String FILE_PATH_TOKEN_SEPERATOR = "/";

  private static final String SSM_SCHEMA_NAME = "ssm_p";

  private static final String SIMULATED_DATA_FILE_URL = "org/icgc/dcc/generator/ssmp_simulated.txt";

  private final Set<String> simulatedData = newHashSet("mutation_type", "chromosome", "chromosome_start",
      "chromosome_end", "reference_genome_allele", "control_genotype", "tumour_genotype mutation");

  private final List<CodeListTerm> codeListTerms = newArrayList();

  private final MutableLong uniqueId = new MutableLong(0L);

  private final MutableInt uniqueInteger = new MutableInt(0);

  private final MutableDouble uniqueDouble = new MutableDouble(0.0);

  private final DataGenerator datagen;

  private final String outputDirectory;

  public PrimaryFileGenerator(DataGenerator datagen, String outputDirectory) {
    this.datagen = datagen;
    this.outputDirectory = outputDirectory;
  }

  public void createFile(ResourceWrapper resourceWrapper, FileSchema schema, Integer linesPerForeignKey,
      String leadJurisdiction, String institution, String tumourType, String platform) throws IOException {

    File outputFile = generateFileName(datagen, schema, leadJurisdiction, institution, tumourType, platform);
    @Cleanup
    Writer writer = buildFileWriter(outputFile);

    datagen.populateTermList(resourceWrapper, schema, codeListTerms);

    log.info("Populating {} file", schema.getName());
    populateFile(resourceWrapper, schema, linesPerForeignKey, writer);
    log.info("Finished populating {}", schema.getName());
  }

  private void populateFileHeader(FileSchema schema, Writer writer) throws IOException {
    int counterForFields = 1;
    int numberOfFields = schema.getFields().size();
    for(String fieldName : schema.getFieldNames()) {
      writeFieldValue(writer, counterForFields, numberOfFields, fieldName);
      counterForFields++;
    }
    writer.write(LINE_SEPERATOR);
  }

  private Writer buildFileWriter(File outputFile) throws FileNotFoundException {
    FileOutputStream fos = new FileOutputStream(outputFile);
    OutputStreamWriter osw = new OutputStreamWriter(fos, Charsets.UTF_8);
    return new BufferedWriter(osw);
  }

  private File generateFileName(DataGenerator datagen, FileSchema schema, String leadJurisdiction, String institution,
      String tumourType, String platform) throws IOException {
    String schemaName = schema.getName();
    String expName = schemaName.substring(0, schemaName.length() - 2);
    String expType = schemaName.substring(schemaName.length() - 1);
    List<String> fileNameTokens = newArrayList(expName, leadJurisdiction, tumourType, institution, expType, platform);

    String fileName = SubmissionFileUtils.generateFileName(fileNameTokens);
    String pathName = outputDirectory + FILE_PATH_TOKEN_SEPERATOR + fileName;
    File outputFile = new File(pathName);
    checkArgument(outputFile.exists() == false, "A file with the name '%s' already exists.", fileName);
    outputFile.createNewFile();

    return outputFile;
  }

  private void populateFile(ResourceWrapper resourceWrapper, FileSchema schema, Integer linesPerForeignKey,
      Writer writer) throws IOException {
    populateFileHeader(schema, writer);
    String schemaName = schema.getName();
    List<Relation> relations = schema.getRelations();

    Iterator<String> iterator = null;
    if(schemaName.equals(SSM_SCHEMA_NAME)) {
      iterator = readSimulatedDataFile();
    }

    int lengthOfForeignKeys = calculateLengthOfForeignKeys(schema, relations);
    int numberOfLinesPerForeignKey = calculateNumberOfLinesPerForeignKey(schema, linesPerForeignKey, relations);

    for(int foreignKeyEntry = 0; foreignKeyEntry < lengthOfForeignKeys; foreignKeyEntry++) {
      for(int foreignKeyEntryLineNumber = 0; foreignKeyEntryLineNumber < numberOfLinesPerForeignKey; foreignKeyEntryLineNumber++) {
        int counterForFields = 1;
        int numberOfFields = schema.getFields().size();
        MutableInt nextTabIndex = new MutableInt(0);
        String line = iterator.next();

        for(Field field : schema.getFields()) {
          String fieldValue =
              getFieldValue(resourceWrapper, schema, schemaName, foreignKeyEntry, nextTabIndex, line, field);

          writeFieldValue(writer, counterForFields, numberOfFields, fieldValue);

          counterForFields++;
        }
        writer.write(LINE_SEPERATOR);
      }
      numberOfLinesPerForeignKey = calculateNumberOfLinesPerForeignKey(schema, linesPerForeignKey, relations);
    }
  }

  private void writeFieldValue(Writer writer, int counterForFields, int numberOfFields, String fieldValue)
      throws IOException {
    if(counterForFields == numberOfFields) {
      writer.write(fieldValue);
    } else {
      writer.write(fieldValue + FIELD_SEPERATOR);
    }
  }

  private Iterator<String> readSimulatedDataFile() throws IOException {
    Iterator<String> iterator;
    List<String> lines = Resources.readLines(Resources.getResource(SIMULATED_DATA_FILE_URL), Charsets.UTF_8);
    Collections.shuffle(lines);
    iterator = Iterables.cycle(lines).iterator();

    return iterator;
  }

  private String getFieldValue(ResourceWrapper resourceWrapper, FileSchema schema, String schemaName,
      int foreignKeyEntry, MutableInt nextTabIndex, String line, Field field) {
    String fieldValue = null;
    String fieldName = field.getName();

    if(schemaName.equals(SSM_SCHEMA_NAME) && simulatedData.contains(fieldName)) {
      fieldValue = line.substring(nextTabIndex.intValue(), line.indexOf(FIELD_SEPERATOR, nextTabIndex.intValue()));
      nextTabIndex.add(fieldValue.length() + 1);
    }

    List<String> foreignKeys = DataGenerator.getForeignKeys(datagen, schema, fieldName);
    if(foreignKeys != null) {
      fieldValue = foreignKeys.get(foreignKeyEntry);
    }

    if(fieldValue == null) {
      fieldValue = getCodeListValue(schema, schemaName, field, fieldName);
    }

    if(fieldValue == null) {
      fieldValue =
          DataGenerator.generateFieldValue(datagen, resourceWrapper, schema.getUniqueFields(), schemaName, field,
              uniqueId, uniqueInteger, uniqueDouble);
    }

    if(resourceWrapper.isUniqueField(schema.getUniqueFields(), fieldName)) {
      DataGenerator.getPrimaryKeys(datagen, schemaName, fieldName).add(fieldValue);
    }
    return fieldValue;
  }

  /**
   * Calculates the number Of non-repetitive entries (with regards to the foreign key fields) to be inserted in the file
   */
  private int calculateLengthOfForeignKeys(FileSchema schema, List<Relation> relations) {
    Relation randomRelation = relations.get(0);
    String relatedFieldName = randomRelation.getFields().get(0);
    int lengthOfForeignKeys = DataGenerator.getForeignKeys(datagen, schema, relatedFieldName).size();
    return lengthOfForeignKeys;
  }

  /**
   * Calculates the number of times a file entry repeats with regards to the foreign key
   */
  private int calculateNumberOfLinesPerForeignKey(FileSchema schema, Integer linesPerForeignKey,
      List<Relation> relations) {
    Relation randomRelation = relations.get(0);// If one relation is bidirectional, assumption is they both are
    if(relations.size() > 0 && randomRelation.isBidirectional()) {
      return datagen.generateRandomInteger(1, linesPerForeignKey);
    } else {
      return datagen.generateRandomInteger(0, linesPerForeignKey);
    }
  }

  private String getCodeListValue(FileSchema schema, String schemaName, Field currentField, String currentFieldName) {
    String fieldValue = null;
    for(CodeListTerm codeListTerm : codeListTerms) {
      if(codeListTerm.getFieldName().equals(currentFieldName)) {
        List<Term> terms = codeListTerm.getTerms();
        fieldValue = terms.get(datagen.generateRandomInteger(0, terms.size())).getCode();

      }
    }
    return fieldValue;
  }
}
