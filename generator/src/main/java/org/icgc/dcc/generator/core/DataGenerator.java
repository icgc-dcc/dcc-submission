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

import static com.google.common.collect.Lists.newArrayList;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang.mutable.MutableDouble;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang.mutable.MutableLong;
import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.Relation;
import org.icgc.dcc.dictionary.model.Restriction;
import org.icgc.dcc.dictionary.model.ValueType;
import org.icgc.dcc.generator.model.CodeListTerm;
import org.icgc.dcc.generator.model.PrimaryKey;
import org.icgc.dcc.generator.utils.ResourceWrapper;

import com.google.common.base.Optional;

/**
 * This File contains a static methods used by CreateCore/Meta/Primary/SecondaryFile Classes It holds the CodeList,
 * FileSchemas and the static listOfPrimaryKeys From here the openFile methods of CreateCore/Meta/Primary/SecondaryFile
 * classes is called
 */
@Slf4j
public class DataGenerator {

  static final String CODELIST_RESTRICTION_NAME = "codelist";

  static final String CONSTANT_DATE = "20130313";

  private final List<PrimaryKey> primaryKeys = newArrayList();

  private Random random;

  private String outputDirectory;

  @SneakyThrows
  public DataGenerator(String outputDirectory, Long seed) {
    this.outputDirectory = outputDirectory;
    this.random = (seed == null) ? new Random() : new Random(seed);
  }

  public List<PrimaryKey> getPrimaryKeys() {
    return this.primaryKeys;
  }

  public int generateRandomInteger(int start, int end) {
    return this.random.nextInt(end) + start;
  }

  public double generateRandomDouble(double end) {
    return this.random.nextDouble() * end;
  }

  public String getOutputDirectory() {
    return this.outputDirectory;
  }

  public void buildPrimaryKey(FileSchema schema) {
    for(String uniqueFieldName : schema.getUniqueFields()) {
      String schemaIdentifier = schema.getName();
      String fieldIdentifier = uniqueFieldName;

      PrimaryKey pk = new PrimaryKey(schemaIdentifier, fieldIdentifier);
      this.primaryKeys.add(pk);
    }
  }

  void populateTermList(ResourceWrapper resourceWrapper, FileSchema schema, List<CodeListTerm> codeListTerms) {
    for(Field field : schema.getFields()) {
      Optional<Restriction> restriction = field.getRestriction(CODELIST_RESTRICTION_NAME);
      if(restriction.isPresent()) {
        String codeListName = restriction.get().getConfig().getString("name");
        List<CodeList> codeLists = resourceWrapper.getCodeLists();
        Iterator<CodeList> cl = codeLists.iterator();
        log.info(codeListName);
        while(cl.hasNext()) {
          CodeList codeList = cl.next();

          if(codeList.getName().equals(codeListName)) {
            CodeListTerm term = new CodeListTerm(field.getName(), codeList.getTerms());
            codeListTerms.add(term);
          }
        }
      }
    }
  }

  public static List<String> getPrimaryKeys(DataGenerator datagen, String schemaName, String fieldName) {
    for(PrimaryKey primaryKey : datagen.getPrimaryKeys()) {
      String primaryKeySchemaIdentifier = primaryKey.getSchemaIdentifier();
      String primaryKeyFieldIdentifier = primaryKey.getFieldIdentifier();

      if(primaryKeySchemaIdentifier.equals(schemaName) && primaryKeyFieldIdentifier.equals(fieldName)) {
        return primaryKey.getPrimaryKeys();
      }
    }
    return null;
  }

  // Go through all the fields that are populated by a foreign key once in each file, and then do an if statement before
  // calling getForeignKey, that would decrease the number of times this method is called
  public static List<String> getForeignKeys(DataGenerator datagen, FileSchema schema, String fieldName) {
    for(Relation relation : schema.getRelations()) {
      int k = 0;
      for(String primaryKeyFieldName : relation.getFields()) {
        if(primaryKeyFieldName.equals(fieldName)) {

          for(PrimaryKey primaryKey : datagen.getPrimaryKeys()) {
            String primaryKeySchemaIdentifier = primaryKey.getSchemaIdentifier();
            String primaryKeyFieldIdentifier = primaryKey.getFieldIdentifier();
            String relatedFileSchemaIdentifier = relation.getOther();
            String relatedFieldIdentifier = relation.getOtherFields().get(k);// foreignKeyFieldName;

            if(primaryKeySchemaIdentifier.equals(relatedFileSchemaIdentifier)
                && primaryKeyFieldIdentifier.equals(relatedFieldIdentifier)) {
              return primaryKey.getPrimaryKeys();
            }
          }
        }
        k++;
      }
    }
    return null;
  }

  public static String generateFieldValue(DataGenerator datagen, ResourceWrapper resourceWrapper, List<String> list,
      String schemaName, Field field, MutableLong uniqueId, MutableInt uniqueInteger, MutableDouble uniqueDecimal) {
    String fieldValue = null;
    String fieldName = field.getName();
    ValueType fieldValueType = field.getValueType();

    if(fieldValueType == ValueType.TEXT) {
      if(resourceWrapper.isUniqueField(list, fieldName)) {
        uniqueId.increment();
        fieldValue = schemaName + String.valueOf(uniqueId);
      } else {
        fieldValue = Integer.toString(datagen.generateRandomInteger(0, Integer.MAX_VALUE));
      }
    } else if(fieldValueType == ValueType.INTEGER) {
      if(resourceWrapper.isUniqueField(list, fieldName)) {
        uniqueInteger.increment();
        fieldValue = schemaName + String.valueOf(uniqueInteger);
      } else {
        fieldValue = Integer.toString(datagen.generateRandomInteger(0, 200));
      }
    } else if(fieldValueType == ValueType.DECIMAL) {
      if(resourceWrapper.isUniqueField(list, fieldName)) {
        uniqueDecimal.add(0.1);
        fieldValue = schemaName + String.valueOf(uniqueDecimal);
      } else {
        fieldValue = Double.toString(datagen.generateRandomDouble(50));
      }
    } else if(fieldValueType == ValueType.DATETIME) {
      fieldValue = CONSTANT_DATE;
    }

    return fieldValue;
  }

}