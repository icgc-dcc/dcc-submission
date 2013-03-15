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

import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.Relation;
import org.icgc.dcc.dictionary.model.Restriction;
import org.icgc.dcc.dictionary.model.Term;
import org.icgc.dcc.generator.model.CodeListTerm;

import com.google.common.base.Optional;

/**
 * 
 */
public class PrimaryFileGenerator {

  public static final String TAB = DataGenerator.TAB;

  public static final String NEW_LINE = DataGenerator.NEW_LINE;

  public Long uniqueId = 0L;

  public Integer uniqueInteger = 0;

  public Double uniqueDecimal = 0.0;

  private final List<CodeListTerm> codeListArrayList = new ArrayList<CodeListTerm>();

  public void populateFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, Writer writer) throws IOException {
    List<Relation> relations = schema.getRelations();

    int numberOfLines = calculateNumberOfLines(schema, numberOfLinesPerPrimaryKey, relations);

    int numberOfIterations = DataGenerator.getForeignKey(schema, relations.get(0).getFields().get(0)).size() - 2;

    String schemaName = schema.getName();
    for(int i = 0; i < numberOfIterations; i++) {
      for(int j = 0; j < numberOfLines; j++) {
        int k = 0;
        for(Field currentField : schema.getFields()) {
          String output = null;
          String currentFieldName = currentField.getName();
          List<String> foreignKeyArray = DataGenerator.getForeignKey(schema, currentFieldName);

          if(foreignKeyArray != null) {
            output = foreignKeyArray.get(i + 2);
          } else {
            output = getFieldValue(schema, schemaName, k, currentField, currentFieldName);
          }

          if(DataGenerator.isUniqueField(schema.getUniqueFields(), currentFieldName)) {
            DataGenerator.getPrimaryKey(schemaName, currentFieldName).add(output);
          }

          writer.write(output + TAB);
        }
        writer.write(NEW_LINE);
      }
      numberOfLines = calculateNumberOfLines(schema, numberOfLinesPerPrimaryKey, relations);
    }
  }

  /**
   * @param schema
   * @param schemaName
   * @param k
   * @param currentField
   * @param currentFieldName
   * @return
   */
  private String getFieldValue(FileSchema schema, String schemaName, int k, Field currentField, String currentFieldName) {
    String output = null;
    if(codeListArrayList.size() > 0 && k < codeListArrayList.size()) {
      for(CodeListTerm codeListTerm : codeListArrayList) {
        if(codeListTerm.getFieldName().equals(currentFieldName)) {
          List<Term> terms = codeListTerm.getTerms();
          output = terms.get(DataGenerator.randomIntGenerator(0, terms.size() - 1)).getCode();

        }
      }
    }
    if(output == null) {
      output =
          DataGenerator.getFieldValue(schema.getUniqueFields(), schemaName, currentField, uniqueInteger, uniqueDecimal);
    }
    return output;
  }

  /**
   * @param schema
   * @param numberOfLinesPerPrimaryKey
   * @param relations
   * @return
   */
  private int calculateNumberOfLines(FileSchema schema, Integer numberOfLinesPerPrimaryKey, List<Relation> relations) {
    if(relations.size() > 0 && relations.get(0).isBidirectional()) {
      return DataGenerator.randomIntGenerator(1, numberOfLinesPerPrimaryKey);
    } else {
      return DataGenerator.randomIntGenerator(0, numberOfLinesPerPrimaryKey);
    }
  }

  public void createFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException {
    boolean isCore = false;
    String fileUrl =
        DataGenerator.generateFileName(schema.getName(), leadJurisdiction, institution, tumourType, platform, isCore);
    File outputFile = new File(fileUrl);
    outputFile.createNewFile();
    @Cleanup
    BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

    for(String fieldName : schema.getFieldNames()) {
      writer.write(fieldName + TAB);
    }

    populateCodeListArray(schema);

    writer.write(NEW_LINE);

    populateFile(schema, numberOfLinesPerPrimaryKey, writer);

    writer.close();
  }

  /**
   * @param schema
   */
  private void populateCodeListArray(FileSchema schema) {
    for(Field field : schema.getFields()) {
      Optional<Restriction> restriction = field.getRestriction("codelist");
      if(restriction.isPresent()) {
        String codeListName = restriction.get().getConfig().getString("name");
        for(CodeList codelist : DataGenerator.codeList) {
          if(codelist.getName().equals(codeListName)) {
            CodeListTerm term = new CodeListTerm(field.getName(), codelist.getTerms());
            codeListArrayList.add(term);
          }
        }
      }
    }
  }
}
