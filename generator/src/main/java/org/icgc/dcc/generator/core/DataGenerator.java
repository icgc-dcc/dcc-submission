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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lombok.SneakyThrows;

import org.apache.commons.lang.mutable.MutableDouble;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang.mutable.MutableLong;
import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.Relation;
import org.icgc.dcc.dictionary.model.ValueType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;

/**
 * This File contains a static methods used by CreateCore/Meta/Primary/SecondaryFile Classes It holds the CodeList,
 * FileSchemas and the static listOfPrimaryKeys From here the openFile methods of CreateCore/Meta/Primary/SecondaryFile
 * classes is called
 */
public class DataGenerator {

  public static final String TAB = "\t";

  public static final String NEW_LINE = "\n";;

  private static final String SEPERATOR = "__";

  private static final String FILE_EXTENSION = ".txt";

  private static final int UPPER_LIMIT_FOR_TEXT_FIELD_INTEGER = 1000000000;

  private static final String CONSTANT_DATE = "20130313";

  // private static final DecimalFormat df = new DecimalFormat("#.00");

  private static final String CODELIST_FILE_NAME = "org/icgc/dcc/generator/codeLists.json";

  private static final String DICTIONARY_FILE_NAME = "org/icgc/dcc/generator/dictionary.json";

  private static String outputDirectory;

  private static ObjectMapper mapper = new ObjectMapper();

  static List<CodeList> codeList;

  private static List<FileSchema> fileSchemas;

  private static List<List<String>> listOfPrimaryKeys = new ArrayList<List<String>>();

  private static Random random;

  @SneakyThrows
  public static void init(String outputDirectory, Long seed) {
    fileSchemas = mapper.readValue(Resources.getResource(DICTIONARY_FILE_NAME), Dictionary.class).getFiles();

    codeList = mapper.readValue(Resources.getResource(CODELIST_FILE_NAME), new TypeReference<List<CodeList>>() {
    });
    DataGenerator.outputDirectory = outputDirectory;

    if(seed != null) {
      random = new Random(seed);
    } else {
      random = new Random();
    }

  }

  public static List<List<String>> getListOfPrimaryKeys() {
    return listOfPrimaryKeys;
  }

  public static void createCoreFile(String schemaName, Integer numberOfLinesPerPrimaryKey, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException {
    FileSchema schema = getSchema(schemaName);
    determineUniqueFields(schema);
    CoreFileGenerator cfg = new CoreFileGenerator();
    cfg.createFile(schema, numberOfLinesPerPrimaryKey, leadJurisdiction, institution, tumourType, platform);
  }

  public static void createTemplateFile(String schemaName, Integer numberOfLinesPerDonor, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException {
    FileSchema schema = getSchema(schemaName);
    TemplateFileGenerator tfg = new TemplateFileGenerator();
    tfg.createFile(schema, numberOfLinesPerDonor, leadJurisdiction, institution, tumourType, platform);
  }

  public static void createMetaFile(String schemaName, Integer numberOfLinesPerPrimaryKey, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException {
    FileSchema schema = getSchema(schemaName);
    MetaFileGenerator mfg = new MetaFileGenerator();
    mfg.createFile(schema, numberOfLinesPerPrimaryKey, leadJurisdiction, institution, tumourType, platform);
  }

  public static void createPrimaryFile(String schemaName, Integer numberOfLinesPerPrimaryKey, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException {
    FileSchema schema = getSchema(schemaName);
    PrimaryFileGenerator pfg = new PrimaryFileGenerator();
    pfg.createFile(schema, numberOfLinesPerPrimaryKey, leadJurisdiction, institution, tumourType, platform);
  }

  public static void createSecondaryFile(String schemaName, Integer numberOfLinesPerPrimaryKey,
      String leadJurisdiction, String institution, String tumourType, String platform) throws IOException {
    FileSchema schema = getSchema(schemaName);
    SecondaryFileGenerator sfg = new SecondaryFileGenerator();
    sfg.createFile(schema, numberOfLinesPerPrimaryKey, leadJurisdiction, institution, tumourType, platform);
  }

  public static List<String> getPrimaryKey(String schemaName, String currentFieldName) {
    for(List<String> primaryKeyArrayList : listOfPrimaryKeys) {
      String primaryKeySchemaIdentifier = primaryKeyArrayList.get(0);
      String primaryKeyFieldIdentifier = primaryKeyArrayList.get(1);

      if(primaryKeySchemaIdentifier.equals(schemaName) && primaryKeyFieldIdentifier.equals(currentFieldName)) {
        return primaryKeyArrayList;
      }
    }
    return null;
  }

  // Go through all the fields that are populated by a forieng key once in each file, and then do an if statement before
  // calling getForeignKey, that would decrease the number of times this method is called
  public static List<String> getForeignKey(FileSchema schema, String fieldName) {
    for(Relation relation : schema.getRelations()) {
      int relatedFieldNameCounter = 0;// The name of the field from the foreign schema
      for(String linkedFieldName : relation.getFields()) {
        if(fieldName.equals(linkedFieldName)) {
          // Find list that carries primary keys of schema that relates to this fileschema
          for(List<String> primaryKeyArrayList : listOfPrimaryKeys) {
            String primaryKeySchemaIdentifier = primaryKeyArrayList.get(0);
            String primaryKeyFieldIdentifier = primaryKeyArrayList.get(1);

            if(primaryKeySchemaIdentifier.equals(relation.getOther())
                && primaryKeyFieldIdentifier.equals(relation.getOtherFields().get(relatedFieldNameCounter))) {
              return primaryKeyArrayList;
            }
          }
        }
        relatedFieldNameCounter++;
      }
    }
    return null;
  }

  public static void checkParameters(String leadJurisdiction, String tumourType, String institution, String platform)
      throws Exception {

    if(leadJurisdiction.length() != 2) {
      throw new Exception("The lead jurisdiction is invalid");
    }
    if(Integer.parseInt(tumourType) > 31 || Integer.parseInt(tumourType) < 1) {
      throw new Exception("The tumour type is invalid");
    }
    if(Integer.parseInt(institution) > 98 || Integer.parseInt(institution) < 1) {
      throw new Exception("The insitute is invalid");
    }
    if(Integer.parseInt(platform) > 75 || Integer.parseInt(institution) < 1) {
      throw new Exception("The platform is invalid");
    }
  }

  public static String generateFileName(String schemaName, String leadJurisdiction, String institution,
      String tumourType, String platform, boolean isCore) {
    if(isCore) {
      return String.format(DataGenerator.outputDirectory + leadJurisdiction + SEPERATOR + tumourType + SEPERATOR
          + institution + SEPERATOR + schemaName + SEPERATOR + CONSTANT_DATE + FILE_EXTENSION);
    } else {
      String fileType = schemaName.substring(0, schemaName.length() - 2);
      return String.format(outputDirectory + fileType + SEPERATOR + leadJurisdiction + SEPERATOR + tumourType
          + SEPERATOR + institution + SEPERATOR + schemaName.charAt(schemaName.length() - 1) + SEPERATOR + platform
          + SEPERATOR + CONSTANT_DATE + FILE_EXTENSION);
    }
  }

  public static void determineUniqueFields(FileSchema schema) {
    for(String uniqueField : schema.getUniqueFields()) {
      String primaryKeySchemaIdentifier = schema.getName();
      String primaryKeyFieldIdentifier = uniqueField;

      List<String> uniqueFieldArray = new ArrayList<String>();
      uniqueFieldArray.add(primaryKeySchemaIdentifier);
      uniqueFieldArray.add(primaryKeyFieldIdentifier);
      listOfPrimaryKeys.add(uniqueFieldArray);
    }
  }

  public static String getFieldValue(List<String> list, String schemaName, Field field, MutableLong uniqueId,
      MutableInt uniqueInteger, MutableDouble uniqueDecimal) {

    String output = null;
    String fieldName = field.getName();
    if(field.getValueType() == ValueType.TEXT) {
      if(isUniqueField(list, fieldName)) {
        uniqueId.increment();
        output = schemaName + String.valueOf(uniqueId);
      } else {
        output = Integer.toString(randomIntGenerator(0, UPPER_LIMIT_FOR_TEXT_FIELD_INTEGER));
      }
    } else if(field.getValueType() == ValueType.INTEGER) {
      if(isUniqueField(list, fieldName)) {
        uniqueInteger.increment();
        output = schemaName + String.valueOf(uniqueInteger);
      } else {
        output = Integer.toString(randomIntGenerator(0, 200));
      }
    } else if(field.getValueType() == ValueType.DECIMAL) {
      if(isUniqueField(list, fieldName)) {
        uniqueDecimal.add(0.1);
        output = schemaName + String.valueOf(uniqueDecimal);
      } else {
        output = DataGenerator.randomDecimalGenerator(50);
      }
    } else if(field.getValueType() == ValueType.DATETIME) {
      output = CONSTANT_DATE;
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
    return Double.toString(random.nextDouble() * end);
  }

}