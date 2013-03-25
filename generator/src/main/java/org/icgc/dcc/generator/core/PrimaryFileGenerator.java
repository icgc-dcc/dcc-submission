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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

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
import org.icgc.dcc.generator.utils.ResourceWrapper;
import org.icgc.dcc.generator.utils.SubmissionUtils;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Resources;

@Slf4j
public class PrimaryFileGenerator {

  private static final String TAB = "\t";

  private static final String NEW_LINE = "\n";

  private static final String CODELIST_RESTRICTION_NAME = "codelist";

  private static final String SSM_SCHEMA_NAME = "ssm_p";

  private static final String SIMULATED_DATA_FILE_URL = "org/icgc/dcc/generator/ssmp_simulated.txt";

  private DataGenerator datagen;

  private final List<CodeListTerm> codeListArrayList = new ArrayList<CodeListTerm>();

  private final Set<String> simulatedData = new HashSet<String>(Arrays.asList("mutation_type", "chromosome",
      "chromosome_start", "chromosome_end", "reference_genome_allele", "control_genotype", "tumour_genotype mutation"));

  private final MutableLong uniqueId = new MutableLong(0L);

  private final MutableInt uniqueInteger = new MutableInt(0);

  private final MutableDouble uniqueDecimal = new MutableDouble(0.0);

  public void createFile(DataGenerator datagen, FileSchema schema, Integer numberOfLinesPerPrimaryKey,
      String leadJurisdiction, String institution, String tumourType, String platform) throws IOException {

    this.datagen = datagen;

    String fileUrl =
        SubmissionUtils.generateExperimentalFileUrl(datagen.getOutputDirectory(), schema.getName(), leadJurisdiction,
            institution, tumourType, platform);
    File outputFile = new File(fileUrl);
    if(!outputFile.createNewFile()) {
      throw new FileAlreadyExistsException("A File with the name: " + fileUrl + " already exists");
    }

    FileOutputStream fos = new FileOutputStream(outputFile);
    OutputStreamWriter osw = new OutputStreamWriter(fos, Charsets.UTF_8);
    @Cleanup
    BufferedWriter writer = new BufferedWriter(osw);

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

    log.info("Populating " + schema.getName() + " file");
    populateFile(schema, numberOfLinesPerPrimaryKey, writer);
    log.info("Finished populating " + schema.getName() + " file");

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
        for(CodeList codelist : ResourceWrapper.getCodeLists()) {
          if(codelist.getName().equals(codeListName)) {
            CodeListTerm term = new CodeListTerm(field.getName(), codelist.getTerms());
            codeListArrayList.add(term);
          }
        }
      }
    }
  }

  public void populateFile(FileSchema schema, Integer numberOfLinesPerPrimaryKey, Writer writer) throws IOException {
    String schemaName = schema.getName();
    List<Relation> relations = schema.getRelations();

    List<String> lines = null;
    if(schemaName.equals(SSM_SCHEMA_NAME)) {
      lines = Resources.readLines(Resources.getResource(SIMULATED_DATA_FILE_URL), Charsets.UTF_8);
    }

    int numberOfLines = calculateNumberOfLines(schema, numberOfLinesPerPrimaryKey, relations);
    int numberOfIterations =
        DataGenerator.getForeignKey(datagen, schema, relations.get(0).getFields().get(0)).size() - 2;

    for(int i = 0; i < numberOfIterations; i++) {
      for(int j = 0; j < numberOfLines; j++) {
        int counterForFields = 0;
        int nextTabIndex = 0;
        String line = lines.get(datagen.randomIntGenerator(0, lines.size() - 1));// This read in the file
        for(Field field : schema.getFields()) {
          String output = null;
          String fieldName = field.getName();
          List<String> foreignKeyArray = DataGenerator.getForeignKey(datagen, schema, fieldName);

          if(foreignKeyArray != null) {

            output = foreignKeyArray.get(i);

          } else {
            if(schemaName.equals(SSM_SCHEMA_NAME) && simulatedData.contains(fieldName)) {// This prints out if true
              output = line.substring(nextTabIndex, line.indexOf(TAB, nextTabIndex));
              nextTabIndex += output.length() + 1;
            } else {
              output = getFieldValue(schema, schemaName, field, fieldName);
            }
          }

          if(ResourceWrapper.isUniqueField(schema.getUniqueFields(), fieldName)) {
            DataGenerator.getPrimaryKey(datagen, schemaName, fieldName).add(output);
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
    if(relations.size() > 0 && relations.get(0).isBidirectional()) {
      return datagen.randomIntGenerator(1, numberOfLinesPerPrimaryKey);
    } else {
      return datagen.randomIntGenerator(0, numberOfLinesPerPrimaryKey);
    }
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
          output = terms.get(datagen.randomIntGenerator(0, terms.size() - 1)).getCode();

        }
      }
    }
    if(output == null) {
      output =
          DataGenerator.generateFieldValue(datagen, schema.getUniqueFields(), schemaName, currentField, uniqueId,
              uniqueInteger, uniqueDecimal);
    }
    return output;
  }
}
