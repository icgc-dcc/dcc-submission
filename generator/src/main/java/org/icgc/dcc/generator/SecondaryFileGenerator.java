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

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;

/**
 * 
 */
public class SecondaryFileGenerator {

  private static final String TAB = DataGenerator.TAB;

  private static final String NEW_LINE = DataGenerator.NEW_LINE;

  public ArrayList<String> uniqueString;

  public Integer uniqueInteger;

  public Double uniqueDecimal;

  public SecondaryFileGenerator() {
    uniqueString = new ArrayList<String>();
    uniqueInteger = 0;
    uniqueDecimal = 0.0;
  }

  public void populateMirnaFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, Writer writer)
      throws IOException {
    FileInputStream fis = null;
    BufferedReader br = null;

    fis = new FileInputStream("src/main/resources/mirna_mirbase.txt");
    br = new BufferedReader(new InputStreamReader(fis));
    int fieldIndexOne = getMirbaseIdIndex(schema, br);
    fis.getChannel().position(0);
    br = new BufferedReader(new InputStreamReader(fis));
    int fieldIndexTwo = getMirnaSequenceIndex(schema, br);

    int numberOfLines =
        (schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) ? DataGenerator
            .randomIntGenerator(1, numberOfLinesPerPrimaryKey) : DataGenerator.randomIntGenerator(0,
            numberOfLinesPerPrimaryKey);

    // 18000 is a random number i just picked cause mirna has no relation to other files
    for(int i = 0; i < 18000; i++) {
      for(int j = 0; j < numberOfLines; j++) {

        for(Field currentField : schema.getFields()) {
          String output = null;

          // Add system file fields
          String line = br.readLine();

          if(line != null) {
            output = getSystemFileOutput(fieldIndexOne, fieldIndexTwo, currentField, output, line.split(TAB));
          } else {
            fis.getChannel().position(0);
            br = new BufferedReader(new InputStreamReader(fis));
            output = getSystemFileOutput(fieldIndexOne, fieldIndexTwo, currentField, output, br.readLine().split(TAB));
          }

          if(output == null) {
            ArrayList<String> foreignKeyArray = DataGenerator.getForeignKey(schema, currentField.getName());
            output =
                foreignKeyArray != null ? foreignKeyArray.get(i + 2) : DataGenerator.getFieldValue(schema,
                    currentField, uniqueString, uniqueInteger, uniqueDecimal);
          }

          writer.write(output + TAB);
        }
        writer.write(NEW_LINE);
      }
      numberOfLines =
          (schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) ? DataGenerator
              .randomIntGenerator(1, numberOfLinesPerPrimaryKey) : DataGenerator.randomIntGenerator(0,
              numberOfLinesPerPrimaryKey);
    }
    br.close();
  }

  public void populateSecondaryFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, Writer writer)
      throws IOException {
    FileInputStream fis = null;
    BufferedReader br = null;
    fis = new FileInputStream("src/main/resources/hsapiens_gene_ensembl__transcript__main.txt");
    br = new BufferedReader(new InputStreamReader(fis));
    int fieldIndexOne = getGeneIndex(schema, br);
    fis.getChannel().position(0);
    br = new BufferedReader(new InputStreamReader(fis));
    int fieldIndexTwo = getTranscriptIndex(schema, br);

    int numberOfLines =
        (schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) ? DataGenerator
            .randomIntGenerator(1, numberOfLinesPerPrimaryKey) : DataGenerator.randomIntGenerator(0,
            numberOfLinesPerPrimaryKey);

    int numberOfIterations =
        DataGenerator.getForeignKey(schema, schema.getRelations().get(0).getFields().get(0)).size() - 2;

    for(int i = 0; i < numberOfIterations; i++) {
      for(int j = 0; j < numberOfLines; j++) {

        for(Field currentField : schema.getFields()) {
          String output = null;

          // Add system file fields
          String line = br.readLine();

          if(line != null) {
            output = getSystemFileOutput(fieldIndexOne, fieldIndexTwo, currentField, output, line.split(TAB));
          } else {
            fis.getChannel().position(0);
            br = new BufferedReader(new InputStreamReader(fis));
            output = getSystemFileOutput(fieldIndexOne, fieldIndexTwo, currentField, output, br.readLine().split(TAB));
          }
          if(output == null) {
            ArrayList<String> foreignKeyArray = DataGenerator.getForeignKey(schema, currentField.getName());
            output =
                foreignKeyArray != null ? foreignKeyArray.get(i + 2) : DataGenerator.getFieldValue(schema,
                    currentField, uniqueString, uniqueInteger, uniqueDecimal);
          }

          writer.write(output + TAB);
        }
        writer.write(NEW_LINE);
      }
      numberOfLines =
          (schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) ? DataGenerator
              .randomIntGenerator(1, numberOfLinesPerPrimaryKey) : DataGenerator.randomIntGenerator(0,
              numberOfLinesPerPrimaryKey);
    }
    br.close();
  }

  public int getGeneIndex(FileSchema schema, BufferedReader bf) throws IOException {
    int indexOfGeneID;
    String[] fields = bf.readLine().split(TAB);

    for(indexOfGeneID = 0; indexOfGeneID < fields.length; indexOfGeneID++) {
      if(fields[indexOfGeneID].equals("gene_id_1020_key")) {
        return indexOfGeneID;
      }
    }

    return -1;
  }

  public int getTranscriptIndex(FileSchema schema, BufferedReader br) throws IOException {
    int indexOfTranscriptID;
    String[] fields = br.readLine().split(TAB);

    for(indexOfTranscriptID = 0; indexOfTranscriptID < fields.length; indexOfTranscriptID++) {
      if(fields[indexOfTranscriptID].equals("transcript_id_1064_key")) {
        return indexOfTranscriptID;
      }
    }

    return -1;
  }

  public int getMirbaseIdIndex(FileSchema schema, BufferedReader br) throws IOException {
    int indexOfMirbaseIdID;
    String[] fields = br.readLine().split(TAB);

    for(indexOfMirbaseIdID = 0; indexOfMirbaseIdID < fields.length; indexOfMirbaseIdID++) {
      if(fields[indexOfMirbaseIdID].equals("xref_mirbase_id")) {
        return indexOfMirbaseIdID;
      }
    }

    return -1;
  }

  public int getMirnaSequenceIndex(FileSchema schema, BufferedReader br) throws IOException {
    int indexOfMirnaSequence;
    String[] fields = br.readLine().split(TAB);

    for(indexOfMirnaSequence = 0; indexOfMirnaSequence < fields.length; indexOfMirnaSequence++) {
      if(fields[indexOfMirnaSequence].equals("mirna_seq")) {
        return indexOfMirnaSequence;
      }
    }

    return -1;

  }

  public String getSystemFileOutput(int fieldIndexOne, int fieldIndexTwo, Field currentField, String output,
      String[] fields) {
    if(currentField.getName().equals("xref_mirbase_id")) {
      output = fields[fieldIndexOne];
    } else if(currentField.getName().equals("mirna_seq")) {
      output = fields[fieldIndexTwo];
    } else if(currentField.getName().equals("gene_affected")) {
      output = fields[fieldIndexOne];
    } else if(currentField.getName().equals("transcript_affected")) {
      output = fields[fieldIndexTwo];
    }
    return output;
  }

  public void createFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, String leadJurisdiction,
      Long institution, Long tumourType, Long platform) throws IOException {
    boolean isCore = false;
    String fileUrle =
        DataGenerator.generateFileName(schema.getName(), leadJurisdiction, institution, tumourType, platform, isCore);
    File outputFile = new File(fileUrle);
    outputFile.createNewFile();
    Writer writer = new BufferedWriter(new FileWriter(outputFile));

    for(String fieldName : schema.getFieldNames()) {
      writer.write(fieldName + TAB);
    }

    writer.write(NEW_LINE);

    if(schema.getName().equals("mirna_s")) {
      populateMirnaFile(schema, numberOfLinesPerPrimaryKey, writer);
    } else {
      populateSecondaryFile(schema, numberOfLinesPerPrimaryKey, writer);
    }

    writer.close();

    uniqueString.removeAll(uniqueString);
    uniqueInteger = 0;
    uniqueDecimal = 0.0;
  }
}
