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
package org.icgc.dcc.submission.normalization;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.io.Files.readLines;
import static org.icgc.dcc.core.model.FieldNames.NormalizerFieldNames.NORMALIZER_MARKING;
import static org.icgc.dcc.core.model.FieldNames.NormalizerFieldNames.NORMALIZER_MUTATION;
import static org.icgc.dcc.core.model.FieldNames.NormalizerFieldNames.NORMALIZER_OBSERVATION_ID;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CONTROL_GENOTYPE;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_MUTATED_FROM_ALLELE;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_MUTATED_TO_ALLELE;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_TUMOUR_GENOTYPE;

import java.io.File;
import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.submission.normalization.steps.Masking;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

/**
 * TODO: add sanity checks + split on a per case basis?
 */
public class EnforceableSpecConverter {

  private static final Splitter TAB_SPLITTER = Splitter.on("\t");

  private static final Joiner NEWLINE_JOINER = Joiner.on("\n");
  private static final Joiner TAB_JOINER = Joiner.on("\t");

  private static final String ROW_TYPE_FIELD_NAME = "type";

  private static final String REFERENCE_GENOME_ALLELE_SHORT_FIELD_NAME = "ref";
  private static final String CONTROL_GENOTYPE_SHORT_FIELD_NAME = "ctr";
  private static final String TUMOUR_GENOTYPE_SHORT_FIELD_NAME = "tmr";
  private static final String MUTATED_FROM_ALLELE_SHORT_FIELD_NAME = "frm";
  private static final String MUTATED_TO_ALLELE_SHORT_FIELD_NAME = "to";
  private static final String MUTATION_SHORT_FIELD_NAME = "mut";

  private static final String MARKING_FIELD_NAME = "marking";
  private static final String COMMENT_FIELD_NAME = "comment";

  private static final String DUMMY_VALUE = "dm";

  private static final String INPUT_TYPE = "input";
  private static final String RESULT_TYPE = "result";

  private static final BiMap<String, String> SHORT_TO_REAL_FIELD_NAMES = new ImmutableBiMap.Builder<String, String>()
      .put(REFERENCE_GENOME_ALLELE_SHORT_FIELD_NAME, SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE)
      .put(CONTROL_GENOTYPE_SHORT_FIELD_NAME, SUBMISSION_OBSERVATION_CONTROL_GENOTYPE)
      .put(TUMOUR_GENOTYPE_SHORT_FIELD_NAME, SUBMISSION_OBSERVATION_TUMOUR_GENOTYPE)
      .put(MUTATED_FROM_ALLELE_SHORT_FIELD_NAME, SUBMISSION_OBSERVATION_MUTATED_FROM_ALLELE)
      .put(MUTATED_TO_ALLELE_SHORT_FIELD_NAME, SUBMISSION_OBSERVATION_MUTATED_TO_ALLELE)
      .put(MUTATION_SHORT_FIELD_NAME, NORMALIZER_MUTATION)
      .put(MARKING_FIELD_NAME, NORMALIZER_MARKING)
      .build();

  private static final String HEADER_LINE = TAB_JOINER.join(new ImmutableList.Builder<String>()
      .add(ROW_TYPE_FIELD_NAME)
      .addAll(SHORT_TO_REAL_FIELD_NAMES.keySet())
      .add(COMMENT_FIELD_NAME)
      .build());

  @SneakyThrows
  public static void convert(String specfile, String specDerivedInputFile, String specDerivedReferenceFile) {
    List<String> lines = readLines(new File(specfile), UTF_8);
    checkState(lines.contains(HEADER_LINE));

    // Extract data from spec
    val specDerivedInputRows = formatData(
        getSubmissionFieldNames(),
        extractData(lines, true));

    val specDerivedReferenceRows = formatData(
        getNormalizationFieldNames(),
        extractData(lines, false));

    // Create dirs/files
    File input = new File(specDerivedInputFile);
    File reference = new File(specDerivedReferenceFile);
    input.delete();
    reference.delete();
    Files.createParentDirs(input);
    Files.createParentDirs(reference);
    Files.write(toTsvString(specDerivedInputRows).getBytes(), input);
    Files.write(toTsvString(specDerivedReferenceRows).getBytes(), reference);
  }

  private static List<Map<String, String>> extractData(List<String> lines, boolean input) {
    val data = Lists.<Map<String, String>> newArrayList();
    for (String line : lines) {
      if (isRelevantLine(line)) {
        Map<String, String> rowMap = characterizeFields(
            Lists.<String> newArrayList(TAB_SPLITTER.split(line)));

        String rowType = rowMap.remove(ROW_TYPE_FIELD_NAME);
        if (input && isInputRow(rowType)) {

          // Remove N/A fields
          checkNotNull(rowMap.remove(MUTATION_SHORT_FIELD_NAME), "TODO");
          checkNotNull(rowMap.remove(MARKING_FIELD_NAME), "TODO");
          data.add(rowMap);
        } else if (!input && isResultRow(rowType)) {

          rowMap.put( // Translate the marking field's abbreviations
              MARKING_FIELD_NAME,
              unabbreviate(checkNotNull(rowMap.remove(MARKING_FIELD_NAME), "TODO")));
          data.add(rowMap);
        } else {
          checkState((input && isResultRow(rowType)) || (!input && isInputRow(rowType)), rowType);
        }
      }
    }
    return data;
  }

  private static List<List<String>> formatData(List<String> targetFieldNames, List<Map<String, String>> rows) {
    val data = Lists.<List<String>> newArrayList();
    data.add(targetFieldNames);

    for (val row : rows) {
      val formattedRow = Lists.<String> newArrayList();
      for (String fieldName : targetFieldNames) {
        if (isSpecifiedField(fieldName)) {
          formattedRow.add(DUMMY_VALUE);
        } else {
          String shortFieldName = SHORT_TO_REAL_FIELD_NAMES.inverse().get(fieldName);
          formattedRow.add(row.get(shortFieldName));
        }
      }
      data.add(formattedRow);
    }
    return data;
  }

  private static List<String> getSubmissionFieldNames() {
    return NormalizationTestUtils.getFieldNames(NormalizationValidator.FOCUS_TYPE);
  }

  private static List<String> getNormalizationFieldNames() {
    List<String> normalizationFieldNames = getSubmissionFieldNames();
    normalizationFieldNames.add(NORMALIZER_MARKING);
    normalizationFieldNames.add(NORMALIZER_MUTATION);
    normalizationFieldNames.add(NORMALIZER_OBSERVATION_ID);
    return normalizationFieldNames;
  }

  private static Map<String, String> characterizeFields(List<String> row) {

    // +2 for type and comment
    checkState(row.size() == SHORT_TO_REAL_FIELD_NAMES.size() + 2);

    int i = 0;
    val rowMap = Maps.<String, String> newLinkedHashMap();

    rowMap.put(ROW_TYPE_FIELD_NAME, row.get(i++));
    rowMap.put(REFERENCE_GENOME_ALLELE_SHORT_FIELD_NAME, row.get(i++));
    rowMap.put(CONTROL_GENOTYPE_SHORT_FIELD_NAME, row.get(i++));
    rowMap.put(TUMOUR_GENOTYPE_SHORT_FIELD_NAME, row.get(i++));
    rowMap.put(MUTATED_FROM_ALLELE_SHORT_FIELD_NAME, row.get(i++));
    rowMap.put(MUTATED_TO_ALLELE_SHORT_FIELD_NAME, row.get(i++));
    rowMap.put(MUTATION_SHORT_FIELD_NAME, row.get(i++));
    rowMap.put(MARKING_FIELD_NAME, row.get(i++));
    rowMap.put(COMMENT_FIELD_NAME, row.get(i++));

    return rowMap;
  }

  private static boolean isRelevantLine(String line) {
    return !line.trim().isEmpty()
        && !line.trim().startsWith("#")
        && !line.equals(HEADER_LINE);
  }

  private static String toTsvString(List<List<String>> listOfList) {
    return NEWLINE_JOINER.join(Iterables.transform(
        listOfList,
        new Function<List<String>, String>() {

          @Override
          public String apply(List<String> input) {
            return TAB_JOINER.join(input);
          }
        }));
  }

  private static String unabbreviate(String abbrev) {
    Masking masking = null;
    if (abbrev.equalsIgnoreCase("OPEN")) {
      masking = Masking.OPEN;
    } else if (abbrev.equalsIgnoreCase("CTRL")) {
      masking = Masking.CONTROLLED;
    } else if (abbrev.equalsIgnoreCase("MASK")) {
      masking = Masking.MASKED;
    } else {
      checkState(false);
    }
    return masking.getTupleValue();
  }

  private static boolean isSpecifiedField(String fieldName) {
    return !SHORT_TO_REAL_FIELD_NAMES.values().contains(fieldName);
  }

  private static boolean isInputRow(String type) {
    return type.equals(INPUT_TYPE);
  }

  private static boolean isResultRow(String type) {
    return type.equals(RESULT_TYPE);
  }
}
