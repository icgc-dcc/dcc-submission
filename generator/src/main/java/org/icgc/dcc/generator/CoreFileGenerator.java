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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;

/**
 * 
 */
public class CoreFileGenerator {

  public static String DONOR_SCHEMA_NAME = "donor";

  public static String SAMPLE_SCHEMA_NAME = "sample";

  private final String TAB = DataGenerator.TAB;

  private final String newLine = DataGenerator.NEW_LINE;

  private final ArrayList<String> tumourSampleTypeID;

  private final ArrayList<String> controlledSampleTypeID;

  public ArrayList<String> donorID;

  public ArrayList<String> specimenID;

  public ArrayList<String> sampleID;

  public ArrayList<String> uniqueString;

  public Integer uniqueInteger;

  public Double uniqueDecimal;

  public CoreFileGenerator() {
    this.donorID = new ArrayList<String>();
    donorID.add("donor");
    donorID.add("donor_id");

    this.specimenID = new ArrayList<String>();
    specimenID.add("specimen");
    specimenID.add("specimen_id");

    this.sampleID = new ArrayList<String>();
    sampleID.add("sample");
    sampleID.add("analyzed_sample_id");

    this.tumourSampleTypeID = new ArrayList<String>();
    tumourSampleTypeID.add("sample");
    tumourSampleTypeID.add("tumourSampleTypeID");

    this.controlledSampleTypeID = new ArrayList<String>();
    controlledSampleTypeID.add("sample");
    controlledSampleTypeID.add("controlledSampleTypeID");

    DataGenerator.getListOfPrimaryKeys().add(donorID);
    DataGenerator.getListOfPrimaryKeys().add(specimenID);
    DataGenerator.getListOfPrimaryKeys().add(sampleID);
    DataGenerator.getListOfPrimaryKeys().add(tumourSampleTypeID);
    DataGenerator.getListOfPrimaryKeys().add(controlledSampleTypeID);

    uniqueString = new ArrayList<String>();
    uniqueInteger = 0;
    uniqueDecimal = 0.0;
  }

  public void populateFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, Writer writer) throws IOException {
    int numberOfPrimaryKeyValues = 0;
    int numberOfLinesPerKey = 0;
    if(schema.getName().equals(DONOR_SCHEMA_NAME)) {
      numberOfPrimaryKeyValues = numberOfLinesPerPrimaryKey;
      numberOfLinesPerKey = 1;

    } else {
      numberOfPrimaryKeyValues =
          DataGenerator.getForeignKey(schema, schema.getRelations().get(0).getFields().get(0)).size() - 2;
      numberOfLinesPerKey =
          (schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) ? DataGenerator
              .randomIntGenerator(1, numberOfLinesPerPrimaryKey) : DataGenerator.randomIntGenerator(0,
              numberOfLinesPerPrimaryKey);
    }

    for(int i = 0; i < numberOfPrimaryKeyValues; i++) {
      for(int j = 0; j < numberOfLinesPerKey; j++) {
        for(Field currentField : schema.getFields()) {
          String output = null;
          ArrayList<String> foreignKeyArray = DataGenerator.getForeignKey(schema, currentField.getName());
          if(foreignKeyArray != null) {

            output = foreignKeyArray.get(i + 2);
          } else {
            output = DataGenerator.getFieldValue(schema, currentField, uniqueString, uniqueInteger, uniqueDecimal);
          }

          if(DataGenerator.isUniqueField(schema.getUniqueFields(), currentField.getName())) {
            DataGenerator.getPrimaryKey(schema.getName(), currentField.getName()).add(output);
          }
          // Special case for sample, to add whether sample type is controlled or tumour
          if(schema.getName().equals(SAMPLE_SCHEMA_NAME) && currentField.getName().equals("analyzed_sample_type")) {

            int x = DataGenerator.randomIntGenerator(0, 1);
            // Instead here you could check if output(which will be the value of analyzed_sample_type) = 'c' then
            // control one or, if output = 't' then go to control two
            if(x == 0) {
              DataGenerator.getPrimaryKey(SAMPLE_SCHEMA_NAME, "tumourSampleTypeID").add(output);
            } else {
              DataGenerator.getPrimaryKey(SAMPLE_SCHEMA_NAME, "controlledSampleTypeID").add(output);
            }
          }
          writer.write(output + TAB);
        }
        writer.write(newLine);
      }
      if(schema.getName().equals(DONOR_SCHEMA_NAME)) {
        numberOfLinesPerKey = 1;
      } else {
        numberOfLinesPerKey =
            (schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) ? DataGenerator
                .randomIntGenerator(1, numberOfLinesPerPrimaryKey) : DataGenerator.randomIntGenerator(0,
                numberOfLinesPerPrimaryKey);
      }
    }
  }

  public void createFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, String leadJurisdiction,
      Long institution, Long tumourType, Long platform) throws IOException {
    boolean isCore = true;
    String fileUrl =
        DataGenerator.generateFileName(schema.getName(), leadJurisdiction, institution, tumourType, platform, isCore);
    File outputFile = new File(fileUrl);
    outputFile.createNewFile();
    Writer writer = new BufferedWriter(new FileWriter(outputFile));

    for(String fieldName : schema.getFieldNames()) {
      writer.write(fieldName + TAB);
    }

    writer.write(newLine);

    populateFile(schema, numberOfLinesPerPrimaryKey, writer);

    writer.close();

    uniqueString.removeAll(uniqueString);
    uniqueInteger = 0;
    uniqueDecimal = 0.0;
  }
}
