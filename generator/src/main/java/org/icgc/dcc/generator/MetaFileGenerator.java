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
import java.util.List;

import lombok.Cleanup;
import lombok.extern.java.Log;

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;

/**
 * 
 */
@Log
public class MetaFileGenerator {

  private final String SAMPLE_SCHEMA_NAME = "sample";

  private final String TAB = DataGenerator.TAB;

  private final String NEW_LINE = DataGenerator.NEW_LINE;

  private final String NonSystemMetaFileExpression = "exp_m";

  private final String NonSystemMetaFileJunction = "jcn_m";

  private final String NonSystemMetaFileMirna = "mirna_m";

  private static final String tumourFieldKey = "tumourSampleTypeID";

  private static final String controlFieldKey = "controlledSampleTypeID";

  private static final String matchedSampleFieldName = "matched_sample_id";

  private final List<String> uniqueString = new ArrayList<String>();

  private final Integer uniqueInteger = 0;

  private final Double uniqueDecimal = 0.0;

  private void populateFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, Writer writer) throws IOException {
    int numberOfLines = calculateNumberOfLines(schema, numberOfLinesPerPrimaryKey);

    int numberOfIterations =
        DataGenerator.getForeignKey(schema, schema.getRelations().get(0).getFields().get(0)).size() - 2;

    for(int i = 0; i < numberOfIterations; i++) {
      for(int j = 0; j < numberOfLines; j++) {
        for(Field currentField : schema.getFields()) {
          String output = null;
          String currentFieldName = currentField.getName();
          // Is it a foreign key? if so then the only foreign key for a metafile is the matched/analyzed type from
          // sample and therefore add accordingly.
          if(DataGenerator.getForeignKey(schema, currentFieldName) != null) {
            boolean isNotMetaExpressionFile = !schema.getName().equals(NonSystemMetaFileExpression);
            boolean isNotMetaJunctionFile = !schema.getName().equals(NonSystemMetaFileJunction);
            boolean isNotMetaMirnaFile = !schema.getName().equals(NonSystemMetaFileMirna);

            if(isNotMetaExpressionFile && isNotMetaJunctionFile && isNotMetaMirnaFile) {

              output = getSampleType(currentFieldName);
            } else {
              output = DataGenerator.getForeignKey(schema, currentFieldName).get(i + 2);
            }
          } else {
            output =
                DataGenerator.getFieldValue(schema.getUniqueFields(), currentField, uniqueString, uniqueInteger,
                    uniqueDecimal);
          }

          if(DataGenerator.isUniqueField(schema.getUniqueFields(), currentFieldName)) {
            for(List<String> primaryKey : DataGenerator.getListOfPrimaryKeys()) {
              if(primaryKey.get(0).equals(schema.getName()) && primaryKey.get(1).equals(currentFieldName)) {
                primaryKey.add(output);
              }
            }
          }

          writer.write(output + TAB);
        }
        writer.write(NEW_LINE);
      }
      numberOfLines = calculateNumberOfLines(schema, numberOfLinesPerPrimaryKey);
    }
  }

  /**
   * @param schema
   * @param numberOfLinesPerPrimaryKey
   * @return
   */
  private int calculateNumberOfLines(FileSchema schema, Integer numberOfLinesPerPrimaryKey) {
    if(schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) {
      return DataGenerator.randomIntGenerator(1, numberOfLinesPerPrimaryKey);
    } else {
      return DataGenerator.randomIntGenerator(0, numberOfLinesPerPrimaryKey);
    }
  }

  /**
   * @param currentField
   * @return
   */
  private String getSampleType(String currentFieldName) {
    if(currentFieldName.equals(matchedSampleFieldName)) {
      List<String> tumourTypeIDs = DataGenerator.getPrimaryKey(SAMPLE_SCHEMA_NAME, tumourFieldKey);
      Integer randomInteger = DataGenerator.randomIntGenerator(2, tumourTypeIDs.size() - 3);
      log.info(tumourTypeIDs.get(randomInteger));
      return tumourTypeIDs.get(randomInteger);
    } else {
      List<String> controlTypeIDs = DataGenerator.getPrimaryKey(SAMPLE_SCHEMA_NAME, controlFieldKey);
      Integer randomInteger = DataGenerator.randomIntGenerator(2, controlTypeIDs.size() - 3);
      return controlTypeIDs.get(randomInteger);
    }
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

    populateFile(schema, numberOfLinesPerPrimaryKey, writer);

    writer.close();
  }
}
