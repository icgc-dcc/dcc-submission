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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

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
import org.icgc.dcc.generator.utils.SubmissionUtils;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

@Slf4j
public class SecondaryFileGenerator {

  private static final String TAB = "\t";

  private static final String NEW_LINE = "\n";

  private static final String MIRNA_MIRBASE_FILE_NAME = "org/icgc/dcc/generator/Mirna_MirbaseSystemFile.txt";

  private static final String HSAPIENS_SYSTEM_FILE_NAME = "org/icgc/dcc/generator/HsapSystemFile.txt";

  private static final String SECONDARY_MIRNA_SCHEMA_NAME = "mirna_s";

  private static final String MIRBASE_ID_FIELD_NAME = "xref_mirbase_id";

  private static final String MIRNA_SEQUENCE_ID_FIELD_NAME = "mirna_seq";

  private static final String SECONDARY_GENE_FIELD_NAME = "gene_affected";

  private static final String SECONDARY_TRANSCRIPT_FIELD_NAME = "transcript_affected";

  private DataGenerator datagen;

  private final List<CodeListTerm> codeListTerms = newArrayList();

  private final MutableLong uniqueId = new MutableLong(0L);

  private final MutableInt uniqueInteger = new MutableInt(0);

  private final MutableDouble uniqueDecimal = new MutableDouble(0.0);

  public void createFile(DataGenerator datagen, ResourceWrapper resourceWrapper, FileSchema schema,
      Integer linesPerForeignKey, String leadJurisdiction, String institution, String tumourType, String platform)
      throws IOException {

    this.datagen = datagen;

    @Cleanup
    BufferedWriter writer = prepareFile(datagen, schema, leadJurisdiction, institution, tumourType, platform);

    populateFileHeader(schema, writer);

    datagen.populateTermList(resourceWrapper, schema, codeListTerms);

    log.info("Populating " + schema.getName() + " file");
    populateFile(resourceWrapper, schema, linesPerForeignKey, writer);
    log.info("Finished populating " + schema.getName() + " file");

    writer.close();
  }

  private BufferedWriter prepareFile(DataGenerator datagen, FileSchema schema, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException, FileNotFoundException {
    // File building
    String fileUrl =
        SubmissionUtils.generateExperimentalFileUrl(datagen.getOutputDirectory(), schema.getName(), leadJurisdiction,
            institution, tumourType, platform);
    File outputFile = new File(fileUrl);
    checkArgument(!outputFile.exists(), "A file with the name '%s' already exists.", fileUrl);
    outputFile.createNewFile();

    // Prepare file writer
    FileOutputStream fos = new FileOutputStream(outputFile);
    OutputStreamWriter osw = new OutputStreamWriter(fos, Charsets.UTF_8);

    return new BufferedWriter(osw);
  }

  private void populateFileHeader(FileSchema schema, BufferedWriter writer) throws IOException {
    // Output field names (eliminate trailing tabs)
    int counterForFieldNames = 0;
    for(String fieldName : schema.getFieldNames()) {
      if(counterForFieldNames == schema.getFields().size() - 1) {
        writer.write(fieldName);
      } else {
        writer.write(fieldName + TAB);
      }
      counterForFieldNames++;
    }
    writer.write(NEW_LINE);
  }

  public void populateFile(ResourceWrapper resourceWrapper, FileSchema schema, Integer linesPerForeignKey, Writer writer)
      throws IOException {
    String schemaName = schema.getName();
    List<Relation> relations = schema.getRelations();

    List<String> lines = readSystemFiles(schemaName);
    Iterator<String> iterator = lines.iterator();

    int lengthOfForeignKeys = calculatedLengthOfForeignKeys(schema, relations);
    int numberOfLinesPerForeignKey = calculateNumberOfLinesPerForeignKey(schema, linesPerForeignKey, relations);

    for(int foreignKeyEntry = 0; foreignKeyEntry < lengthOfForeignKeys; foreignKeyEntry++) {
      for(int foreignKeyEntryLineNumber = 0; foreignKeyEntryLineNumber < numberOfLinesPerForeignKey; foreignKeyEntryLineNumber++) {
        int counterForFields = 0;

        // Get net line, cycling around if needed
        if(!iterator.hasNext()) {
          iterator = lines.iterator();
        }
        String line = iterator.next();

        for(Field field : schema.getFields()) {
          String output = getFieldValue(resourceWrapper, schema, schemaName, foreignKeyEntry, line, field);

          // Output field value (eliminate trailing tabs)
          if(schema.getFields().size() - 1 == counterForFields) {
            writer.write(output);
          } else {
            writer.write(output + TAB);
          }

          counterForFields++;
        }
        writer.write(NEW_LINE);
      }
      numberOfLinesPerForeignKey = calculateNumberOfLinesPerForeignKey(schema, linesPerForeignKey, relations);
    }
  }

  private List<String> readSystemFiles(String schemaName) throws IOException {
    // Read in system files
    List<String> lines = null;
    if(schemaName.equals(SECONDARY_MIRNA_SCHEMA_NAME)) {
      lines = Resources.readLines(Resources.getResource(MIRNA_MIRBASE_FILE_NAME), Charsets.UTF_8);
    } else {
      lines = Resources.readLines(Resources.getResource(HSAPIENS_SYSTEM_FILE_NAME), Charsets.UTF_8);
    }
    return lines;
  }

  private String getFieldValue(ResourceWrapper resourceWrapper, FileSchema schema, String schemaName, int i,
      String line, Field field) {
    String fieldName = field.getName();

    // populate output with systemfile value if current field is to be populated with a system file value
    String output = getSystemFileOutput(fieldName, line);

    if(output == null) {
      List<String> foreignKeyArray = DataGenerator.getForeignKeys(datagen, schema, fieldName);
      if(foreignKeyArray != null) {
        output = foreignKeyArray.get(i);
      } else {
        output = getCodeListValue(schema, schemaName, field, fieldName);
      }
      if(output == null) {
        output =
            DataGenerator.generateFieldValue(datagen, resourceWrapper, schema.getUniqueFields(), schemaName, field,
                uniqueId, uniqueInteger, uniqueDecimal);
      }
    }
    return output;
  }

  /**
   * Calculates the number Of non-repetitive entries (with regards to the foreign key fields) to be inserted in the file
   */
  private int calculatedLengthOfForeignKeys(FileSchema schema, List<Relation> relations) {
    Relation randomRelation = relations.get(0);
    String relatedFieldName = randomRelation.getFields().get(0);
    int lengthOfForeignKeys = DataGenerator.getForeignKeys(datagen, schema, relatedFieldName).size() - 2;
    return lengthOfForeignKeys;
  }

  /**
   * Calculates the number of times a file entry repeats with regards to the foreign key
   */
  private int calculateNumberOfLinesPerForeignKey(FileSchema schema, Integer linesPerForeignKey,
      List<Relation> relations) {
    if(relations.size() > 0 && relations.get(0).isBidirectional()) {
      return datagen.randomIntGenerator(1, linesPerForeignKey);
    } else {
      return datagen.randomIntGenerator(0, linesPerForeignKey);
    }
  }

  public String getSystemFileOutput(String currentFieldName, String line) {
    if(currentFieldName.equals(MIRBASE_ID_FIELD_NAME)) {
      return line.substring(0, line.indexOf(TAB));
    } else if(currentFieldName.equals(MIRNA_SEQUENCE_ID_FIELD_NAME)) {
      return line.substring(line.indexOf(TAB) + 1, line.length());
    } else if(currentFieldName.equals(SECONDARY_GENE_FIELD_NAME)) {
      return line.substring(0, line.indexOf(TAB));
    } else if(currentFieldName.equals(SECONDARY_TRANSCRIPT_FIELD_NAME)) {
      return line.substring(line.indexOf(TAB) + 1, line.length());
    }
    return null;
  }

  private String getCodeListValue(FileSchema schema, String schemaName, Field currentField, String currentFieldName) {
    String output = null;
    if(codeListTerms.size() > 0) {
      for(CodeListTerm codeListTerm : codeListTerms) {
        if(codeListTerm.getFieldName().equals(currentFieldName)) {
          List<Term> terms = codeListTerm.getTerms();
          output = terms.get(datagen.randomIntGenerator(0, terms.size() - 1)).getCode();
        }
      }
    }
    return output;
  }

}
