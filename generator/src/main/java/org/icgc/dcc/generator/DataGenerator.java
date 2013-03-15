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

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.Relation;
import org.icgc.dcc.dictionary.model.ValueType;

import com.google.common.io.Resources;

/**
 * This File contains a static methods used by CreateCore/Meta/Primary/SecondaryFile Classes It holds the CodeList,
 * FileSchemas and the static listOfPrimaryKeys From here the openFile methods of CreateCore/Meta/Primary/SecondaryFile
 * classes is called
 */

public class DataGenerator {

  private static Long uniqueId = 0L;

  public static final String TAB = "\t";;

  public static final String NEW_LINE = "\n";

  private static final DecimalFormat df = new DecimalFormat("#.00");

  private static final String SEPERATOR = "__";

  private static final String FILE_EXTENSION = ".txt";

  private static String OUTPUT_DIRECTORY;

  private static ObjectMapper mapper = new ObjectMapper();

  public static List<CodeList> codeList;

  private static Random random;

  private static List<List<String>> listOfPrimaryKeys = new ArrayList<List<String>>();

  private static List<FileSchema> fileSchemas;

  public DataGenerator(String outputDirectory, Long seed) throws JsonParseException, JsonMappingException, IOException {

    OUTPUT_DIRECTORY = outputDirectory;

    fileSchemas =
        mapper.readValue(Resources.getResource("dictionary.json"), org.icgc.dcc.dictionary.model.Dictionary.class)
            .getFiles();

    codeList = mapper.readValue(Resources.getResource("codeList.json"), new TypeReference<List<CodeList>>() {
    });

    random = new Random(seed);

  }

  public static List<List<String>> getListOfPrimaryKeys() {
    return listOfPrimaryKeys;
  }

  public void createCoreFile(String schemaName, Integer numberOfLinesPerPrimaryKey, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException {
    uniqueId = 0L;
    FileSchema schema = getSchema(schemaName);
    determineUniqueFields(schema);
    CoreFileGenerator.createFile(schema, numberOfLinesPerPrimaryKey, leadJurisdiction, institution, tumourType,
        platform);
  }

  public void createMetaFile(String schemaName, Integer numberOfLinesPerPrimaryKey, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException {
    uniqueId = 0L;
    FileSchema schema = getSchema(schemaName);
    MetaFileGenerator mfg = new MetaFileGenerator();
    mfg.createFile(schema, numberOfLinesPerPrimaryKey, leadJurisdiction, institution, tumourType, platform);
  }

  public void createPrimaryFile(String schemaName, Integer numberOfLinesPerPrimaryKey, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException {
    uniqueId = 0L;
    FileSchema schema = getSchema(schemaName);
    PrimaryFileGenerator pfg = new PrimaryFileGenerator();
    pfg.createFile(schema, numberOfLinesPerPrimaryKey, leadJurisdiction, institution, tumourType, platform);
  }

  public void createSecondaryFile(String schemaName, Integer numberOfLinesPerPrimaryKey, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException {
    uniqueId = 0L;
    FileSchema schema = getSchema(schemaName);
    SecondaryFileGenerator sfg = new SecondaryFileGenerator();
    sfg.createFile(schema, numberOfLinesPerPrimaryKey, leadJurisdiction, institution, tumourType, platform);
  }

  public static List<String> getPrimaryKey(String schemaName, String currentFieldName) {
    for(List<String> primaryKeyArray : listOfPrimaryKeys) {
      if(primaryKeyArray.get(0).equals(schemaName) && primaryKeyArray.get(1).equals(currentFieldName)) {
        return primaryKeyArray;
      }
    }
    return null;
  }

  public static List<String> getForeignKey(FileSchema schema, String currentFieldName) {
    for(Relation relation : schema.getRelations()) {
      int k = 0;
      for(String foreignKeyField : relation.getFields()) {
        if(currentFieldName.equals(foreignKeyField)) {
          // Find list that carries primary keys of schema that relates to this fileschema
          for(List<String> primaryKeyArray : listOfPrimaryKeys) {
            if(primaryKeyArray.get(0).equals(relation.getOther())
                && primaryKeyArray.get(1).equals(relation.getOtherFields().get(k))) {
              return primaryKeyArray;
            }
          }
        }
        k++;
      }
    }
    return null;
  }

  public static String[] checkParameters(String leadJurisdiction, String tumourType, String institution, String platform) {
    String[] error = new String[4];
    if(leadJurisdiction.length() != 2) {
      error[0] = "The lead jurisdiction is invalid";
    }
    if(Integer.parseInt(tumourType) > 31 || Integer.parseInt(tumourType) < 1) {
      error[1] = "The tumour type is invalid";
    }
    if(Integer.parseInt(institution) > 98 || Integer.parseInt(institution) < 1) {
      error[2] = "The insitute is invalid";
    }
    if(Integer.parseInt(platform) > 75 || Integer.parseInt(institution) < 1) {
      error[3] = "The platform is invalid";
    }
    return error;
  }

  public static String generateFileName(String schemaName, String leadJurisdiction, String institution,
      String tumourType, String platform, boolean isCore) {
    if(isCore) {
      return String.format(OUTPUT_DIRECTORY + leadJurisdiction + SEPERATOR + tumourType + SEPERATOR + institution
          + SEPERATOR + schemaName + SEPERATOR + "20130313" + FILE_EXTENSION);
    } else {
      String fileType = schemaName.substring(0, schemaName.length() - 2);
      return String.format(OUTPUT_DIRECTORY + fileType + SEPERATOR + leadJurisdiction + SEPERATOR + tumourType
          + SEPERATOR + institution + SEPERATOR + schemaName.charAt(schemaName.length() - 1) + SEPERATOR + platform
          + SEPERATOR + "20130313" + FILE_EXTENSION);
    }
  }

  public void determineUniqueFields(FileSchema schema) {
    for(String uniqueField : schema.getUniqueFields()) {
      List<String> uniqueFieldArray = new ArrayList<String>();
      uniqueFieldArray.add(schema.getName());
      uniqueFieldArray.add(uniqueField);
      listOfPrimaryKeys.add(uniqueFieldArray);
    }
  }

  public static String getFieldValue(List<String> list, String schemaName, Field field, Integer uniqueInt,
      Double uniqueDecimal) {

    String output = null;
    if(field.getValueType() == ValueType.TEXT) {
      if(isUniqueField(list, field.getName())) {
        output = schemaName + Long.toString(uniqueId++);
      } else {
        output = Integer.toString(randomIntGenerator(0, 1000000000));
      }
    } else if(field.getValueType() == ValueType.INTEGER) {
      if(isUniqueField(list, field.getName())) {
        output = Integer.toString(uniqueInt++);
      } else {
        output = Integer.toString(randomIntGenerator(0, 200));
      }
    } else if(field.getValueType() == ValueType.DECIMAL) {
      if(isUniqueField(list, field.getName())) {
        output = Double.toString(uniqueDecimal + 0.1);
      } else {
        output = DataGenerator.randomDecimalGenerator(50);
      }
    } else if(field.getValueType() == ValueType.DATETIME) {
      output = "20130313";
    }
    return output;
  }

  public static FileSchema getSchema(String schemaName) {
    for(FileSchema schema : fileSchemas) {
      if(schema.getName().equals(schemaName)) {
        return schema;
      }
    }
    return null;
  }

  public static boolean isUniqueField(List<String> list, String fieldName) {
    if(list.contains(fieldName)) {
      return true;
    }
    return false;
  }

  public static int randomIntGenerator(int start, int end) {
    return random.nextInt(end + 1) + start;
  }

  public static String randomDecimalGenerator(double end) {
    return df.format(random.nextDouble() * end);
  }
}