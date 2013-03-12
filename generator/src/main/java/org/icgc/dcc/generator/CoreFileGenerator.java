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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Cleanup;

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;

/**
 * 
 */
public class CoreFileGenerator {

  private static final String DONOR_SCHEMA_NAME = "donor";

  private static final String SAMPLE_SCHEMA_NAME = "sample";

  private static final String TAB = DataGenerator.TAB;

  private static final String NEW_LINE = DataGenerator.NEW_LINE;

  private static final String tumourFieldKey = "tumourSampleTypeID";

  private static final String controlFieldKey = "controlledSampleTypeID";

  private static final String sampleTypeFieldName = "analyzed_sample_id";

  private final List<String> tumourSampleTypeID = new ArrayList<String>(Arrays.asList("sample", "tumourSampleTypeID"));

  private final List<String> controlledSampleTypeID = new ArrayList<String>(Arrays.asList("sample",
      "controlledSampleTypeID"));

  private final List<String> donorID = new ArrayList<String>(Arrays.asList("donor", "donor_id"));

  private final List<String> specimenID = new ArrayList<String>(Arrays.asList("specimen", "specimen_id"));

  private final List<String> sampleID = new ArrayList<String>(Arrays.asList("sample", "analyzed_sample_id"));

  private final List<String> uniqueString = new ArrayList<String>();

  private final Integer uniqueInteger = 0;

  private final Double uniqueDecimal = 0.0;

  public CoreFileGenerator() {
    DataGenerator.getListOfPrimaryKeys().add(donorID);
    DataGenerator.getListOfPrimaryKeys().add(specimenID);
    DataGenerator.getListOfPrimaryKeys().add(sampleID);
    DataGenerator.getListOfPrimaryKeys().add(tumourSampleTypeID);
    DataGenerator.getListOfPrimaryKeys().add(controlledSampleTypeID);
  }

  public void populateFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, BufferedWriter writer)
      throws IOException {
    int numberOfPrimaryKeyValues = calcuateNumberOfPrimaryKeyValues(schema, numberOfLinesPerPrimaryKey);
    int numberOfLinesPerKey = calculateNumberOfLinesPerKey(schema, numberOfLinesPerPrimaryKey);

    for(int i = 0; i < numberOfPrimaryKeyValues; i++) {
      for(int j = 0; j < numberOfLinesPerKey; j++) {
        for(Field currentField : schema.getFields()) {
          String output = null;
          List<String> foreignKeyArray = DataGenerator.getForeignKey(schema, currentField.getName());

          if(foreignKeyArray != null) {
            output = foreignKeyArray.get(i + 2);
          } else {
            output =
                DataGenerator.getFieldValue(schema.getUniqueFields(), currentField, uniqueString, uniqueInteger,
                    uniqueDecimal);
          }

          if(DataGenerator.isUniqueField(schema.getUniqueFields(), currentField.getName())) {
            DataGenerator.getPrimaryKey(schema.getName(), currentField.getName()).add(output);
          }
          // Special case for sample, to add whether sample type is controlled or tumour
          if(schema.getName().equals(SAMPLE_SCHEMA_NAME) && currentField.getName().equals(sampleTypeFieldName)) {

            int x = DataGenerator.randomIntGenerator(0, 1);
            // Instead here you could check if output(which will be the value of analyzed_sample_type) = 'c' then
            // control one or, if output = 't' then go to control two
            if(x == 0) {
              DataGenerator.getPrimaryKey(SAMPLE_SCHEMA_NAME, tumourFieldKey).add(output);
            } else {
              DataGenerator.getPrimaryKey(SAMPLE_SCHEMA_NAME, controlFieldKey).add(output);
            }
          }
          writer.write(output + TAB);
        }
        writer.write(NEW_LINE);
      }
      numberOfLinesPerKey = calculateNumberOfLinesPerKey(schema, numberOfLinesPerPrimaryKey);
    }
  }

  /**
   * @param schema
   * @param numberOfLinesPerPrimaryKey
   * @return
   */
  private int calcuateNumberOfPrimaryKeyValues(FileSchema schema, Integer numberOfLinesPerPrimaryKey) {
    int numberOfPrimaryKeyValues;
    if(schema.getName().equals(DONOR_SCHEMA_NAME)) {
      numberOfPrimaryKeyValues = numberOfLinesPerPrimaryKey;
    } else {
      numberOfPrimaryKeyValues =
          DataGenerator.getForeignKey(schema, schema.getRelations().get(0).getFields().get(0)).size() - 2;
    }
    return numberOfPrimaryKeyValues;
  }

  /**
   * @param schema
   * @param numberOfLinesPerPrimaryKey
   * @return
   */
  private int calculateNumberOfLinesPerKey(FileSchema schema, Integer numberOfLinesPerPrimaryKey) {
    if(schema.getName().equals(DONOR_SCHEMA_NAME)) {
      return 1;
    } else if(schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) {
      return DataGenerator.randomIntGenerator(1, numberOfLinesPerPrimaryKey);
    } else {
      return DataGenerator.randomIntGenerator(0, numberOfLinesPerPrimaryKey);
    }
  }

  public void createFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException {
    boolean isCore = true;
    String fileUrl =
        DataGenerator.generateFileName(schema.getName(), leadJurisdiction, institution, tumourType, platform, isCore);
    File outputFile = new File(fileUrl);
    outputFile.createNewFile();
    @Cleanup
    BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

    for(String fieldName : schema.getFieldNames()) {
      writer.write(fieldName + TAB);
    }

    writer.write(NEW_LINE);

    populateFile(schema, numberOfLinesPerPrimaryKey, writer);

    writer.close();
  }
}
