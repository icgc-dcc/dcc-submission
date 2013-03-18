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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.Cleanup;

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

  private static final String MIRNA_MIRBASE_FILE_NAME = "Mirna_MirbaseSystemFile.txt";

  private static final String HSAPIENS_SYSTEM_FILE_NAME = "HsapSystemFile.txt";

  private static final String SECONDARY_MIRNA_SCHEMA_NAME = "mirna_s";

  private static final String MIRBASE_ID_FIELD_NAME = "xref_mirbase_id";

  private static final String MIRNA_SEQUENCE_ID_FIELD_NAME = "mirna_seq";

  private static final String HSAPIENS_GENE_FIELD_NAME = "gene_id_1020_key";

  private static final String HSAPIENS_TRANSCRIPT_FIELD_NAME = "transcript_id_1064_key";

  private static final String SECONDARY_GENE_FIELD_NAME = "gene_affected";

  private static final String SECONDARY_TRANSCRIPT_FIELD_NAME = "transcript_affected";

  public static Integer uniqueInteger = 0;

  public static Double uniqueDecimal = 0.0;

  private final List<CodeListTerm> codeListArrayList = new ArrayList<CodeListTerm>();

  public void populateMirnaFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, Writer writer)
      throws IOException {

    List<String> lines = Resources.readLines(Resources.getResource(MIRNA_MIRBASE_FILE_NAME), Charsets.UTF_8);
    Iterator<String> iterator = lines.iterator();

    String schemaName = schema.getName();
    List<Relation> relations = schema.getRelations();
    int numberOfIterations = DataGenerator.getForeignKey(schema, relations.get(0).getFields().get(0)).size() - 2;
    int numberOfLines = calculateNumberOfLines(schema, numberOfLinesPerPrimaryKey, relations);

    for(int i = 0; i < numberOfIterations; i++) {
      for(int j = 0; j < numberOfLines; j++) {
        int k = 0;
        for(Field currentField : schema.getFields()) {
          String output = null;
          String currentFieldName = currentField.getName();

          // Add system file fields
          if(iterator.hasNext()) {
            String line = iterator.next();
            output = getSystemFileOutput(currentField.getName(), line);
          } else { // Reset the reader to beginning of file and repeat
            iterator = lines.iterator();
            String line = iterator.next();
            output = getSystemFileOutput(currentField.getName(), line);
          }

          if(output == null) {
            List<String> foreignKeyArray = DataGenerator.getForeignKey(schema, currentFieldName);
            if(foreignKeyArray != null) {
              output = foreignKeyArray.get(i + 2);
            } else {
              output = getFieldValue(schema, schemaName, k, currentField, currentFieldName);
            }
          }

          writer.write(output + TAB);
        }
        writer.write(NEW_LINE);
      }
      numberOfLines = calculateNumberOfLines(schema, numberOfLinesPerPrimaryKey, relations);
    }
  }

  public void populateSecondaryFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, Writer writer)
      throws IOException {

    List<String> lines = Resources.readLines(Resources.getResource(HSAPIENS_SYSTEM_FILE_NAME), Charsets.UTF_8);
    Iterator<String> iterator = lines.iterator();

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

          // Add system file fields
          if(iterator.hasNext()) {
            String line = iterator.next();
            output = getSystemFileOutput(currentFieldName, line);
          } else {
            // End of file reached. Reset reader and readline
            iterator = lines.iterator();
            String line = iterator.next();
            output = getSystemFileOutput(currentField.getName(), line);
          }
          if(output == null) {
            List<String> foreignKeyArray = DataGenerator.getForeignKey(schema, currentFieldName);
            if(foreignKeyArray != null) {
              output = foreignKeyArray.get(i + 2);
            } else {
              output = getFieldValue(schema, schemaName, k, currentField, currentFieldName);
            }
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

  public int getGeneIndex(FileSchema schema, String[] fields) throws IOException {
    for(int indexOfGeneID = 0; indexOfGeneID < fields.length; indexOfGeneID++) {
      if(fields[indexOfGeneID].equals(HSAPIENS_GENE_FIELD_NAME)) {
        return indexOfGeneID;
      }
    }

    return -1;
  }

  public int getTranscriptIndex(FileSchema schema, String[] fields) throws IOException {
    for(int indexOfTranscriptID = 0; indexOfTranscriptID < fields.length; indexOfTranscriptID++) {
      if(fields[indexOfTranscriptID].equals(HSAPIENS_TRANSCRIPT_FIELD_NAME)) {
        return indexOfTranscriptID;
      }
    }

    return -1;
  }

  public int getMirbaseIdIndex(FileSchema schema, String[] fields) throws IOException {
    for(int indexOfMirbaseIdID = 0; indexOfMirbaseIdID < fields.length; indexOfMirbaseIdID++) {
      if(fields[indexOfMirbaseIdID].equals(MIRBASE_ID_FIELD_NAME)) {
        return indexOfMirbaseIdID;
      }
    }

    return -1;
  }

  public int getMirnaSequenceIndex(FileSchema schema, String[] fields) throws IOException {
    for(int indexOfMirnaSequence = 0; indexOfMirnaSequence < fields.length; indexOfMirnaSequence++) {
      if(fields[indexOfMirnaSequence].equals(MIRNA_SEQUENCE_ID_FIELD_NAME)) {
        return indexOfMirnaSequence;
      }
    }

    return -1;

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

    populateCodeListArray(schema);

    writer.write(NEW_LINE);

    if(schema.getName().equals(SECONDARY_MIRNA_SCHEMA_NAME)) {
      populateMirnaFile(schema, numberOfLinesPerPrimaryKey, writer);
    } else {
      populateSecondaryFile(schema, numberOfLinesPerPrimaryKey, writer);
    }

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
