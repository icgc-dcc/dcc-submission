/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.sample.processor;

import static org.icgc.dcc.core.model.FileTypes.FileType.CNSM_M_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.EXP_ARRAY_M_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.EXP_SEQ_M_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.MIRNA_SEQ_M_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SGV_M_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SSM_M_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.STSM_M_TYPE;
import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.core.report.ErrorType.REFERENCE_SAMPLE_TYPE_MISMATCH;
import static org.icgc.dcc.submission.core.report.ErrorType.SAMPLE_TYPE_MISMATCH;
import static org.icgc.dcc.submission.validation.sample.core.ReferenceSampleTypeCategory.MATCHED;
import static org.icgc.dcc.submission.validation.sample.core.SpecimenTypeCategory.NON_NORMAL;
import static org.icgc.dcc.submission.validation.sample.core.SpecimenTypeCategory.NORMAL;
import static org.icgc.dcc.submission.validation.sample.util.SampleTypeFieldNames.ANALYZED_SAMPLE_ID_FIELD_NAME;
import static org.icgc.dcc.submission.validation.sample.util.SampleTypeFieldNames.MATCHED_SAMPLE_ID_FIELD_NAME;
import static org.icgc.dcc.submission.validation.sample.util.SampleTypeFieldNames.REFERENCE_SAMPLE_TYPE_FIELD_NAME;

import java.io.IOException;
import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.core.model.SpecialValue;
import org.icgc.dcc.hadoop.parser.FileRecordProcessor;
import org.icgc.dcc.submission.validation.core.ReportContext;
import org.icgc.dcc.submission.validation.sample.core.ReferenceSampleTypeCategory;
import org.icgc.dcc.submission.validation.sample.core.Samples;
import org.icgc.dcc.submission.validation.sample.core.SpecimenTypeCategory;

/**
 * {@link FileRecordProcessor} implementation that performs the actual row-level validation logic when parsing feature
 * type files.
 *
 * @see https://wiki.oicr.on.ca/display/DCCBIO/Consistent+sample+types+across+rows+in+experimental+files
 */
@RequiredArgsConstructor
public class MetaFileSampleTypeProcessor implements FileRecordProcessor<Map<String, String>> {

  /**
   * Metadata.
   */
  @NonNull
  private final FileType metaFileType;
  @NonNull
  private final Path metaFile;

  /**
   * Data.
   */
  @NonNull
  private final Samples samples;

  /**
   * State.
   */
  @NonNull
  private final ReportContext context;

  /**
   * Main verification method that verifies the semantic consistency of the supplied record's feature type defined
   * sample type with that of the associated clinical data.
   */
  @Override
  public void process(long lineNumber, Map<String, String> record) throws IOException {

    /*
     * Resolve
     */

    val analyzedSampleId = record.get(ANALYZED_SAMPLE_ID_FIELD_NAME);
    val analyzedSpecimenType = samples.getSpecimenTypeBySampleId(analyzedSampleId);
    val analyzedSpecimenTypeCategory = SpecimenTypeCategory.fromSpecimenType(analyzedSpecimenType);

    val matchedSampleId = record.get(MATCHED_SAMPLE_ID_FIELD_NAME);
    val matchedSpecimenType = samples.getSpecimenTypeBySampleId(matchedSampleId);
    val matchedSpecimenTypeCategory = SpecimenTypeCategory.fromSpecimenType(matchedSpecimenType);

    val referenceSampleType = record.get(REFERENCE_SAMPLE_TYPE_FIELD_NAME);
    val referenceSampleTypeCategory = ReferenceSampleTypeCategory.fromReferenceSampleType(referenceSampleType);

    /*
     * Verify
     */

    if (isMutationFileType(metaFileType) && !isNotApplicable(matchedSampleId)) {

      /*
       * Mutation sample type validation (analyzed)
       */

      if (analyzedSpecimenTypeCategory != NON_NORMAL) {
        context.reportError(
            error()
                .fileName(metaFile.getName())
                .fieldNames(ANALYZED_SAMPLE_ID_FIELD_NAME)
                .type(SAMPLE_TYPE_MISMATCH)
                .lineNumber(lineNumber)
                .value(analyzedSampleId)
                .params(analyzedSpecimenType, NON_NORMAL.getDescription())
                .build());
      }

      /*
       * Mutation sample type validation (matched)
       */

      if (matchedSpecimenTypeCategory != NORMAL) {
        context.reportError(
            error()
                .fileName(metaFile.getName())
                .fieldNames(MATCHED_SAMPLE_ID_FIELD_NAME)
                .type(SAMPLE_TYPE_MISMATCH)
                .lineNumber(lineNumber)
                .value(matchedSampleId)
                .params(matchedSpecimenType, NORMAL.getDescription())
                .build());
      }
    } else if (isVariationFileType(metaFileType)) {

      /*
       * SGV sample type validation (analyzed)
       */

      if (analyzedSpecimenTypeCategory != NORMAL) {
        context.reportError(
            error()
                .fileName(metaFile.getName())
                .fieldNames(ANALYZED_SAMPLE_ID_FIELD_NAME)
                .type(SAMPLE_TYPE_MISMATCH)
                .lineNumber(lineNumber)
                .value(analyzedSampleId)
                .params(analyzedSpecimenType, NORMAL.getDescription())
                .build());
      }
    } else if (isReferringSurveyFileType(metaFileType)) {

      /*
       * Reference sample type validation (analyzed)
       */

      if (analyzedSpecimenTypeCategory == NORMAL && referenceSampleTypeCategory == MATCHED) {
        context.reportError(
            error()
                .fileName(metaFile.getName())
                .fieldNames(REFERENCE_SAMPLE_TYPE_FIELD_NAME)
                .type(REFERENCE_SAMPLE_TYPE_MISMATCH)
                .lineNumber(lineNumber)
                .value(analyzedSampleId)
                .params(referenceSampleType, MATCHED.getDescription())
                .build());
      } else {
        // Nothing to validate
      }
    }
  }

  /**
   * Is a mutation file type.
   * <p>
   * "Mutation" is different from "variation".
   */
  private static boolean isMutationFileType(FileType metaFileType) {
    return metaFileType == SSM_M_TYPE || metaFileType == CNSM_M_TYPE || metaFileType == STSM_M_TYPE;
  }

  /**
   * Is a variation file type.
   * <p>
   * "Variation" is different from "mutation".
   */
  private static boolean isVariationFileType(FileType metaFileType) {
    return metaFileType == SGV_M_TYPE;
  }

  /**
   * Is a so-called "survey file type" with a {@code reference_sample_type} field.
   */
  private static boolean isReferringSurveyFileType(FileType metaFileType) {
    return metaFileType == EXP_ARRAY_M_TYPE || metaFileType == EXP_SEQ_M_TYPE || metaFileType == MIRNA_SEQ_M_TYPE;
  }

  /**
   * Is {@code -888}.
   */
  private static boolean isNotApplicable(String analyzedSampleId) {
    return SpecialValue.NOT_APPLICABLE_CODE.equals(analyzedSampleId);
  }

}