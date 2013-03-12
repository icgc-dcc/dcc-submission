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
package org.icgc.dcc.generator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import lombok.Cleanup;

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;

/**
 * 
 */
public class SecondaryFileGenerator {

  private static final String TAB = DataGenerator.TAB;

  private static final String NEW_LINE = DataGenerator.NEW_LINE;

  private static final String MIRNA_MIRBASE_FILE_URL = "src/main/resources/mirna_mirbase.txt";

  private static final String HSAPIENS_SYSTEM_FILE_URL =
      "src/main/resources/hsapiens_gene_ensembl__transcript__main.txt";

  private static final String SECONDARY_MIRNA_SCHEMA_NAME = "mirna_s";

  private static final String MIRBASE_ID_FIELD_NAME = "xref_mirbase_id";

  private static final String MIRNA_SEQUENCE_ID_FIELD_NAME = "mirna_seq";

  private static final String HSAPIENS_GENE_FIELD_NAME = "gene_id_1020_key";

  private static final String HSAPIENS_TRANSCRIPT_FIELD_NAME = "transcript_id_1064_key";

  private static final String SECONDARY_GENE_FIELD_NAME = "gene_affected";

  private static final String SECONDARY_TRANSCRIPT_FIELD_NAME = "transcript_affected";

  public List<String> uniqueString = new ArrayList<String>();

  public Integer uniqueInteger = 0;

  public Double uniqueDecimal = 0.0;

  public void populateMirnaFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, Writer writer)
      throws IOException {
    @Cleanup
    FileInputStream inputStream = new FileInputStream(MIRNA_MIRBASE_FILE_URL);
    @Cleanup
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

    String[] fields = reader.readLine().split(TAB);
    int mirbaseIdIndex = getMirbaseIdIndex(schema, fields);
    int mirnaSequenceId = getMirnaSequenceIndex(schema, fields);

    int numberOfLines = calculateNumberOfLines(schema, numberOfLinesPerPrimaryKey);

    // 18000 is a random number i just picked cause mirna has no relation to other files
    for(int i = 0; i < 18000; i++) {
      for(int j = 0; j < numberOfLines; j++) {

        for(Field currentField : schema.getFields()) {
          String output = null;

          // Add system file fields
          String line = reader.readLine();
          // Checks to see that end of file hasn't been reached
          if(line != null) {
            output = getSystemFileOutput(mirbaseIdIndex, mirnaSequenceId, currentField, output, line.split(TAB));
          } else { // Reset the reader to beginning of file and repeate
            resetReader(inputStream, reader);
            line = reader.readLine();
            output = getSystemFileOutput(mirbaseIdIndex, mirnaSequenceId, currentField, output, line.split(TAB));
          }

          if(output == null) {
            List<String> foreignKeyArray = DataGenerator.getForeignKey(schema, currentField.getName());
            output =
                foreignKeyArray != null ? foreignKeyArray.get(i + 2) : DataGenerator.getFieldValue(
                    schema.getUniqueFields(), currentField, uniqueString, uniqueInteger, uniqueDecimal);
          }

          writer.write(output + TAB);
        }
        writer.write(NEW_LINE);
      }
      numberOfLines = calculateNumberOfLines(schema, numberOfLinesPerPrimaryKey);
    }
    reader.close();
  }

  public void populateSecondaryFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, Writer writer)
      throws IOException {
    FileInputStream inputStream = new FileInputStream(HSAPIENS_SYSTEM_FILE_URL);
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

    String[] fields = reader.readLine().split(TAB);
    int geneIndex = getGeneIndex(schema, fields);
    int transcriptIndex = getTranscriptIndex(schema, fields);

    int numberOfLines = calculateNumberOfLines(schema, numberOfLinesPerPrimaryKey);

    int numberOfIterations =
        DataGenerator.getForeignKey(schema, schema.getRelations().get(0).getFields().get(0)).size() - 2;

    for(int i = 0; i < numberOfIterations; i++) {
      for(int j = 0; j < numberOfLines; j++) {

        for(Field currentField : schema.getFields()) {
          String output = null;

          // Add system file fields
          String line = reader.readLine();

          if(line != null) {
            output = getSystemFileOutput(geneIndex, transcriptIndex, currentField, output, line.split(TAB));
          } else {
            resetReader(inputStream, reader);
            line = reader.readLine();
            output = getSystemFileOutput(geneIndex, transcriptIndex, currentField, output, line.split(TAB));
          }
          if(output == null) {
            List<String> foreignKeyArray = DataGenerator.getForeignKey(schema, currentField.getName());
            output =
                foreignKeyArray != null ? foreignKeyArray.get(i + 2) : DataGenerator.getFieldValue(
                    schema.getUniqueFields(), currentField, uniqueString, uniqueInteger, uniqueDecimal);
          }

          writer.write(output + TAB);
        }
        writer.write(NEW_LINE);
      }
      numberOfLines = calculateNumberOfLines(schema, numberOfLinesPerPrimaryKey);
    }
    reader.close();
  }

  /**
   * @param inputStream
   * @return
   * @throws IOException
   */
  private void resetReader(FileInputStream inputStream, BufferedReader reader) throws IOException {
    inputStream.getChannel().position(0);
    reader = new BufferedReader(new InputStreamReader(inputStream));
  }

  /**
   * @param schema
   * @param numberOfLinesPerPrimaryKey
   * @return
   */
  private int calculateNumberOfLines(FileSchema schema, Integer numberOfLinesPerPrimaryKey) {
    boolean hasRelations = schema.getRelations().size() > 0;
    boolean isBidirectional = schema.getRelations().get(0).isBidirectional();

    if(hasRelations && isBidirectional) {
      return DataGenerator.randomIntGenerator(1, numberOfLinesPerPrimaryKey);
    } else {
      return DataGenerator.randomIntGenerator(0, numberOfLinesPerPrimaryKey);
    }
  }

  public int getGeneIndex(FileSchema schema, String[] fields) throws IOException {
    for(int indexOfGeneID = 0; indexOfGeneID < fields.length; indexOfGeneID++) {
      if(fields[indexOfGeneID].equals(HSAPIENS_GENE_FIELD_NAME)) {
        return indexOfGeneID;
      }
    }

    return -1;
  }

  public int getTranscriptIndex(FileSchema schema, String[] fields) throws IOException {
    for(int indexOfTranscriptID = 0; indexOfTranscriptID < fields.length; indexOfTranscriptID++) {
      if(fields[indexOfTranscriptID].equals(HSAPIENS_TRANSCRIPT_FIELD_NAME)) {
        return indexOfTranscriptID;
      }
    }

    return -1;
  }

  public int getMirbaseIdIndex(FileSchema schema, String[] fields) throws IOException {
    for(int indexOfMirbaseIdID = 0; indexOfMirbaseIdID < fields.length; indexOfMirbaseIdID++) {
      if(fields[indexOfMirbaseIdID].equals(MIRBASE_ID_FIELD_NAME)) {
        return indexOfMirbaseIdID;
      }
    }

    return -1;
  }

  public int getMirnaSequenceIndex(FileSchema schema, String[] fields) throws IOException {
    for(int indexOfMirnaSequence = 0; indexOfMirnaSequence < fields.length; indexOfMirnaSequence++) {
      if(fields[indexOfMirnaSequence].equals(MIRNA_SEQUENCE_ID_FIELD_NAME)) {
        return indexOfMirnaSequence;
      }
    }

    return -1;

  }

  public String getSystemFileOutput(int firstIndex, int secondIndex, Field currentField, String output, String[] fields) {
    if(currentField.getName().equals(MIRBASE_ID_FIELD_NAME)) {
      output = fields[firstIndex];
    } else if(currentField.getName().equals(MIRNA_SEQUENCE_ID_FIELD_NAME)) {
      output = fields[secondIndex];
    } else if(currentField.getName().equals(SECONDARY_GENE_FIELD_NAME)) {
      output = fields[firstIndex];
    } else if(currentField.getName().equals(SECONDARY_TRANSCRIPT_FIELD_NAME)) {
      output = fields[secondIndex];
    }
    return output;
  }

  public void createFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException {
    boolean isCore = false;
    String fileURL =
        DataGenerator.generateFileName(schema.getName(), leadJurisdiction, institution, tumourType, platform, isCore);
    File outputFile = new File(fileURL);
    outputFile.createNewFile();
    @Cleanup
    BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

    for(String fieldName : schema.getFieldNames()) {
      writer.write(fieldName + TAB);
    }

    writer.write(NEW_LINE);

    if(schema.getName().equals(SECONDARY_MIRNA_SCHEMA_NAME)) {
      populateMirnaFile(schema, numberOfLinesPerPrimaryKey, writer);
    } else {
      populateSecondaryFile(schema, numberOfLinesPerPrimaryKey, writer);
    }

    writer.close();
  }
}
