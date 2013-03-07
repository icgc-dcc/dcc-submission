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
public class PrimaryFileGenerator {

  /**
   * 
   */
  public ArrayList<String> uniqueString;

  /**
   * 
   */
  public Integer uniqueInteger;

  /**
   * 
   */
  public Double uniqueDecimal;

  public String tab;

  public String newLine;

  public PrimaryFileGenerator() {
    tab = DataGenerator.tab;
    newLine = DataGenerator.newLine;
    uniqueString = new ArrayList<String>();
    uniqueInteger = 0;
    uniqueDecimal = 0.0;
  }

  public void populateFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, Writer writer) throws IOException {
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

          ArrayList<String> foreignKeyArray = DataGenerator.getForeignKey(schema, currentField.getName());

          output =
              foreignKeyArray != null ? foreignKeyArray.get(i + 2) : DataGenerator.getFieldValue(schema, currentField,
                  uniqueString, uniqueInteger, uniqueDecimal);

          if(DataGenerator.isUniqueField(schema.getUniqueFields(), currentField.getName())) {
            DataGenerator.getPrimaryKey(schema.getName(), currentField.getName()).add(output);
            if(currentField.getName().equals("placement")) {
              System.out.println(output);
            }
          }

          writer.write(output + tab);
        }
        writer.write(newLine);
      }
      numberOfLines =
          (schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) ? DataGenerator
              .randomIntGenerator(1, numberOfLinesPerPrimaryKey) : DataGenerator.randomIntGenerator(0,
              numberOfLinesPerPrimaryKey);
    }
  }

  public void createFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, String leadJurisdiction,
      Long institution, Long tumourType, Long platform) throws IOException {
    boolean isCore = false;
    String fileUrl =
        DataGenerator.generateFileName(schema.getName(), leadJurisdiction, institution, tumourType, platform, isCore);
    File outputFile = new File(fileUrl);
    outputFile.createNewFile();
    Writer writer = new BufferedWriter(new FileWriter(outputFile));

    for(String fieldName : schema.getFieldNames()) {
      writer.write(fieldName + tab);
    }

    writer.write(newLine);

    populateFile(schema, numberOfLinesPerPrimaryKey, writer);

    writer.close();

    uniqueString.removeAll(uniqueString);
    uniqueInteger = 0;
    uniqueDecimal = 0.0;
  }
}
