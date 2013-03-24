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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.Cleanup;

import org.apache.commons.lang.mutable.MutableDouble;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang.mutable.MutableLong;
import org.apache.hadoop.fs.FileAlreadyExistsException;
import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.Relation;
import org.icgc.dcc.dictionary.model.Restriction;
import org.icgc.dcc.dictionary.model.Term;
import org.icgc.dcc.generator.model.CodeListTerm;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Resources;

/**
 * 
 */
public class SecondaryFileGenerator {

  private static final String TAB = DataGenerator.TAB;

  private static final String NEW_LINE = DataGenerator.NEW_LINE;

  private static final String CODELIST_RESTRICTION_NAME = "codelist";

  private static final String MIRNA_MIRBASE_FILE_NAME = "org/icgc/dcc/generator/Mirna_MirbaseSystemFile.txt";

  private static final String HSAPIENS_SYSTEM_FILE_NAME = "org/icgc/dcc/generator/HsapSystemFile.txt";

  private static final String SECONDARY_MIRNA_SCHEMA_NAME = "mirna_s";

  private static final String MIRBASE_ID_FIELD_NAME = "xref_mirbase_id";

  private static final String MIRNA_SEQUENCE_ID_FIELD_NAME = "mirna_seq";

  private static final String SECONDARY_GENE_FIELD_NAME = "gene_affected";

  private static final String SECONDARY_TRANSCRIPT_FIELD_NAME = "transcript_affected";

  private final List<CodeListTerm> codeListArrayList = new ArrayList<CodeListTerm>();

  private final MutableLong uniqueId = new MutableLong(0L);

  private final MutableInt uniqueInteger = new MutableInt(0);

  private final MutableDouble uniqueDecimal = new MutableDouble(0.0);

  public void createFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, String leadJurisdiction,
      String institution, String tumourType, String platform) throws IOException {

    boolean isCore = false;

    String fileUrl =
        DataGenerator.generateFileName(schema.getName(), leadJurisdiction, institution, tumourType, platform, isCore);
    File outputFile = new File(fileUrl);
    if(!outputFile.createNewFile()) {
      throw new FileAlreadyExistsException("A File with the name: " + fileUrl + " already exists");
    }
    @Cleanup
    BufferedWriter writer =
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), Charsets.UTF_8));

    int counterForFieldNames = 0;
    for(String fieldName : schema.getFieldNames()) {
      if(counterForFieldNames == schema.getFields().size() - 1) {
        writer.write(fieldName);
      } else {
        writer.write(fieldName + TAB);
      }
      counterForFieldNames++;
    }

    populateCodeListArray(schema);

    writer.write(NEW_LINE);

    populateSecondaryFile(schema, numberOfLinesPerPrimaryKey, writer);

    writer.close();
  }

  /**
   * @param schema
   */
  private void populateCodeListArray(FileSchema schema) {
    for(Field field : schema.getFields()) {
      Optional<Restriction> restriction = field.getRestriction(CODELIST_RESTRICTION_NAME);
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

  public void populateSecondaryFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, Writer writer)
      throws IOException {
    String schemaName = schema.getName();
    List<Relation> relations = schema.getRelations();

    List<String> lines = null;
    if(schemaName.equals(SECONDARY_MIRNA_SCHEMA_NAME)) {
      lines = Resources.readLines(Resources.getResource(MIRNA_MIRBASE_FILE_NAME), Charsets.UTF_8);
    } else {
      lines = Resources.readLines(Resources.getResource(HSAPIENS_SYSTEM_FILE_NAME), Charsets.UTF_8);
    }
    Iterator<String> iterator = lines.iterator();

    int numberOfLines = calculateNumberOfLines(schema, numberOfLinesPerPrimaryKey, relations);
    int numberOfIterations = DataGenerator.getForeignKey(schema, relations.get(0).getFields().get(0)).size() - 2;

    for(int i = 0; i < numberOfIterations; i++) {
      for(int j = 0; j < numberOfLines; j++) {
        int counterForFields = 0;

        // Get net line, cycling around if needed
        if(!iterator.hasNext()) {
          iterator = lines.iterator();
        }
        String line = iterator.next();

        for(Field field : schema.getFields()) {
          String fieldName = field.getName();
          String output = getSystemFileOutput(fieldName, line);

          if(output == null) {
            List<String> foreignKeyArray = DataGenerator.getForeignKey(schema, fieldName);
            if(foreignKeyArray != null) {
              output = foreignKeyArray.get(i + 2);
            } else {
              output = getFieldValue(schema, schemaName, field, fieldName);
            }
          }

          if(schema.getFields().size() - 1 == counterForFields) {
            writer.write(output);
          } else {
            writer.write(output + TAB);
          }
          counterForFields++;
        }
        writer.write(NEW_LINE);
      }
      numberOfLines = calculateNumberOfLines(schema, numberOfLinesPerPrimaryKey, relations);
    }
  }

  /**
   * @param schema
   * @param numberOfLinesPerPrimaryKey
   * @param relations
   * @return
   */
  private int calculateNumberOfLines(FileSchema schema, Integer numberOfLinesPerPrimaryKey, List<Relation> relations) {
    boolean hasRelations = relations.size() > 0;
    boolean isBidirectional = relations.get(0).isBidirectional();

    if(hasRelations && isBidirectional) {
      return DataGenerator.randomIntGenerator(1, numberOfLinesPerPrimaryKey);
    } else {
      return DataGenerator.randomIntGenerator(0, numberOfLinesPerPrimaryKey);
    }
  }

  public String getSystemFileOutput(String currentFieldName, String line) {
    if(currentFieldName.equals(MIRBASE_ID_FIELD_NAME)) {
      return line.substring(0, line.indexOf(TAB));
    } else if(currentFieldName.equals(MIRNA_SEQUENCE_ID_FIELD_NAME)) {
      return line.substring(line.indexOf(TAB) + 1, line.length());
    } else if(currentFieldName.equals(SECONDARY_GENE_FIELD_NAME)) {
      return line.substring(0, line.indexOf(TAB));
    } else if(currentFieldName.equals(SECONDARY_TRANSCRIPT_FIELD_NAME)) {
      return line.substring(line.indexOf(TAB) + 1, line.length());
    }
    return null;
  }

  /**
   * @param schema
   * @param schemaName
   * @param indexOfCodeListArray
   * @param currentField
   * @param currentFieldName
   * @return
   */
  private String getFieldValue(FileSchema schema, String schemaName, Field currentField, String currentFieldName) {
    String output = null;
    if(codeListArrayList.size() > 0) {
      for(CodeListTerm codeListTerm : codeListArrayList) {
        if(codeListTerm.getFieldName().equals(currentFieldName)) {
          List<Term> terms = codeListTerm.getTerms();
          output = terms.get(DataGenerator.randomIntGenerator(0, terms.size() - 1)).getCode();
        }
      }
    }
    if(output == null) {
      output =
          DataGenerator.getFieldValue(schema.getUniqueFields(), schemaName, currentField, uniqueId, uniqueInteger,
              uniqueDecimal);
    }
    return output;
  }

}
