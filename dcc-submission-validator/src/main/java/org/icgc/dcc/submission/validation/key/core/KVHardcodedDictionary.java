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
package org.icgc.dcc.submission.validation.key.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableList.of;
import static org.icgc.dcc.submission.validation.key.core.KVExperimentalDataType.CNSM;
import static org.icgc.dcc.submission.validation.key.core.KVExperimentalDataType.EXP_ARRAY;
import static org.icgc.dcc.submission.validation.key.core.KVExperimentalDataType.EXP_SEQ;
import static org.icgc.dcc.submission.validation.key.core.KVExperimentalDataType.JCN;
import static org.icgc.dcc.submission.validation.key.core.KVExperimentalDataType.METH_ARRAY;
import static org.icgc.dcc.submission.validation.key.core.KVExperimentalDataType.METH_SEQ;
import static org.icgc.dcc.submission.validation.key.core.KVExperimentalDataType.MIRNA_SEQ;
import static org.icgc.dcc.submission.validation.key.core.KVExperimentalDataType.PEXP;
import static org.icgc.dcc.submission.validation.key.core.KVExperimentalDataType.SGV;
import static org.icgc.dcc.submission.validation.key.core.KVExperimentalDataType.SSM;
import static org.icgc.dcc.submission.validation.key.core.KVExperimentalDataType.STSM;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.BIOMARKER;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.EXPOSURE;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.EXP_ARRAY_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.EXP_ARRAY_P;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.EXP_SEQ_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.EXP_SEQ_P;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.FAMILY;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.JCN_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.JCN_P;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.METH_ARRAY_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.METH_ARRAY_P;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.METH_ARRAY_PROBES;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.METH_SEQ_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.METH_SEQ_P;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.MIRNA_SEQ_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.MIRNA_SEQ_P;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.PEXP_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.PEXP_P;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SGV_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SGV_P;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SSM_P;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.STSM_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.STSM_P;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.STSM_S;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SURGERY;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.THERAPY;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.submission.validation.key.data.KVFileTypeErrorFields;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;

import lombok.val;

public final class KVHardcodedDictionary implements KVDictionary {

  /**
   * Field ordinals
   */

  // CORE
  private static final List<Integer> DONOR_PKS = of(0);

  private static final List<Integer> SAMPLE_FKS = of(1);
  private static final List<Integer> SAMPLE_PKS = of(0);

  private static final List<Integer> SPECIMEN_FKS = of(0);
  private static final List<Integer> SPECIMEN_PKS = of(1);

  // SUPPLEMENTAL
  private static final List<Integer> BIOMARKER_FKS1 = of(0);
  private static final List<Integer> BIOMARKER_FKS2 = of(1);
  private static final List<Integer> BIOMARKER_PKS = of(0, 1, 2);

  private static final List<Integer> FAMILY_FKS = of(0);
  private static final List<Integer> FAMILY_PKS = of(0, 1, 2, 3, 4, 5, 6, 7);

  private static final List<Integer> EXPOSURE_FKS = of(0);
  private static final List<Integer> EXPOSURE_PKS = of(0);

  private static final List<Integer> SURGERY_FKS1 = of(0);
  private static final List<Integer> SURGERY_FKS2 = of(5);
  private static final List<Integer> SURGERY_PKS = of(0, 5);

  private static final List<Integer> THERAPY_FKS = of(0);
  private static final List<Integer> THERAPY_PKS = of(0);

  // EXPERIMENTAL
  private static final List<Integer> CNSM_M_FKS1 = of(1);
  private static final List<Integer> CNSM_M_OPTIONAL_FKS = of(2);
  private static final List<Integer> CNSM_M_PKS = of(0, 1);
  private static final List<Integer> CNSM_P_FKS = of(0, 1);
  private static final List<Integer> CNSM_P_PKS = of(0, 1, 2);
  private static final List<Integer> CNSM_S_FKS = of(0, 1, 2);

  private static final List<Integer> JCN_M_FKS = of(1);
  private static final List<Integer> JCN_M_PKS = of(0, 1);
  private static final List<Integer> JCN_P_FKS = of(0, 1);

  private static final List<Integer> PEXP_M_FKS = of(1);
  private static final List<Integer> PEXP_M_PKS = of(0, 1);
  private static final List<Integer> PEXP_P_FKS = of(0, 1);

  private static final List<Integer> SGV_M_FKS = of(1);
  private static final List<Integer> SGV_M_PKS = of(0, 1);
  private static final List<Integer> SGV_P_FKS = of(0, 1);

  private static final List<Integer> SSM_M_FKS1 = of(1);
  private static final List<Integer> SSM_M_OPTIONAL_FKS = of(2);
  private static final List<Integer> SSM_M_PKS = of(0, 1);
  private static final List<Integer> SSM_P_FKS = of(0, 1);

  private static final List<Integer> STSM_M_FKS1 = of(1);
  private static final List<Integer> STSM_M_OPTIONAL_FKS = of(2);
  private static final List<Integer> STSM_M_PKS = of(0, 1);
  private static final List<Integer> STSM_P_FKS = of(0, 1);
  private static final List<Integer> STSM_P_PKS = of(0, 1, 2, 3);
  private static final List<Integer> STSM_S_FKS = of(0, 1, 2, 3);

  // METH ARRAY
  private static final List<Integer> METH_ARRAY_M_PKS = of(0, 1);
  private static final List<Integer> METH_ARRAY_M_FKS = of(1);
  private static final List<Integer> METH_ARRAY_P_FKS1 = of(0, 1);
  private static final List<Integer> METH_ARRAY_P_FKS2 = of(2, 3);
  private static final List<Integer> METH_ARRAY_SYSTEM_PKS = of(0, 1);

  // METH SEQ
  private static final List<Integer> METH_SEQ_M_PKS = of(0, 1);
  private static final List<Integer> METH_SEQ_M_FKS = of(1);
  private static final List<Integer> METH_SEQ_P_FKS = of(0, 1);

  // EXP ARRAY
  private static final List<Integer> EXP_ARRAY_M_PKS = of(0, 1);
  private static final List<Integer> EXP_ARRAY_M_FKS = of(1);
  private static final List<Integer> EXP_ARRAY_P_PKS = of(0, 1, 2, 3);
  private static final List<Integer> EXP_ARRAY_P_FKS = of(0, 1);

  // EXP SEQ
  private static final List<Integer> EXP_SEQ_M_PKS = of(0, 1);
  private static final List<Integer> EXP_SEQ_M_FKS = of(1);
  private static final List<Integer> EXP_SEQ_P_PKS = of(0, 1, 2, 3);
  private static final List<Integer> EXP_SEQ_P_FKS = of(0, 1);

  // MIRNA SEQ
  private static final List<Integer> MIRNA_SEQ_M_PKS = of(0, 1);
  private static final List<Integer> MIRNA_SEQ_M_FKS = of(1);
  private static final List<Integer> MIRNA_SEQ_P_FKS = of(0, 1);

  /**
   * Field names.
   */

  private static final List<String> DONOR_PK_NAMES = of("donor_id");
  private static final List<String> SPECIMEN_FK_NAMES = of("donor_id");
  private static final List<String> SPECIMEN_PK_NAMES = of("specimen_id");
  private static final List<String> SAMPLE_FK_NAMES = of("specimen_id");
  private static final List<String> SAMPLE_PK_NAMES = of("analyzed_sample_id");

  private static final List<String> BIOMARKER_FK1_NAMES = of("donor_id");
  private static final List<String> BIOMARKER_FK2_NAMES = of("specimen_id");
  private static final List<String> BIOMARKER_PK_NAMES = of("donor_id", "specimen_id", "biomarker_name");

  private static final List<String> FAMILY_FK_NAMES = of("donor_id");
  private static final List<String> FAMILY_PK_NAMES = of(
      "donor_id",
      "donor_has_relative_with_cancer_history",
      "relationship_type",
      "relationship_type_other",
      "relationship_sex",
      "relationship_age",
      "relationship_disease_icd10",
      "relationship_disease");

  private static final List<String> EXPOSURE_FK_NAMES = of("donor_id");
  private static final List<String> EXPOSURE_PK_NAMES = of("donor_id");

  private static final List<String> SURGERY_FK1_NAMES = of("donor_id");
  private static final List<String> SURGERY_FK2_NAMES = of("specimen_id");
  private static final List<String> SURGERY_PK_NAMES = of("donor_id", "specimen_id");

  private static final List<String> THERAPY_FK_NAMES = of("donor_id");
  private static final List<String> THERAPY_PK_NAMES = of("donor_id");

  private static final List<String> CNSM_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> CNSM_M_OPTIONAL_FK_NAMES = of("matched_sample_id");
  private static final List<String> CNSM_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> CNSM_P_FK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> CNSM_P_PK_NAMES = of("analysis_id", "analyzed_sample_id", "mutation_id");
  private static final List<String> CNSM_S_FK_NAMES = of("analysis_id", "analyzed_sample_id", "mutation_id");

  private static final List<String> EXP_ARRAY_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> EXP_ARRAY_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> EXP_ARRAY_P_FK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> EXP_ARRAY_P_PK_NAMES = of("analysis_id", "analyzed_sample_id", "gene_model",
      "gene_id");

  private static final List<String> EXP_SEQ_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> EXP_SEQ_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> EXP_SEQ_P_FK_NAMES = of("analyzed_sample_id", "analyzed_sample_id");
  private static final List<String> EXP_SEQ_P_PK_NAMES = of("analysis_id", "analyzed_sample_id", "gene_model",
      "gene_id");

  private static final List<String> JCN_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> JCN_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> JCN_P_FK_NAMES = of("analysis_id", "analyzed_sample_id");

  private static final List<String> MIRNA_SEQ_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> MIRNA_SEQ_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> MIRNA_SEQ_P_FK_NAMES = of("analysis_id", "analyzed_sample_id");

  private static final List<String> PEXP_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> PEXP_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> PEXP_P_FK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> SGV_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> SGV_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> SGV_P_FK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> SSM_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> SSM_M_OPTIONAL_FK_NAMES = of("matched_sample_id");
  private static final List<String> SSM_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> SSM_P_FK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> STSM_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> STSM_M_OPTIONAL_FK_NAMES = of("matched_sample_id");
  private static final List<String> STSM_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> STSM_P_FK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> STSM_P_PK_NAMES = of("analysis_id", "analyzed_sample_id", "placement", "sv_id");
  private static final List<String> STSM_S_FK_NAMES = of("analysis_id", "analyzed_sample_id", "sv_id", "placement");

  // METH ARRAY
  private static final List<String> METH_ARRAY_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> METH_ARRAY_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> METH_ARRAY_P_FK1_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> METH_ARRAY_P_FK2_NAMES = of("array_platform", "probe_id");
  private static final List<String> METH_ARRAY_SYSTEM_PK_NAMES = of("array_platform", "probe_id");

  // METH SEQ
  private static final List<String> METH_SEQ_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> METH_SEQ_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> METH_SEQ_P_FK_NAMES = of("analysis_id", "analyzed_sample_id");

  // TODO: merge the two following into one data structure (as well as what's above)
  private static final Map<KVFileType, KVFileType> RELATIONS1 = // TODO: all necessary?
      new ImmutableMap.Builder<KVFileType, KVFileType>()
          .put(SPECIMEN, DONOR)
          .put(SAMPLE, SPECIMEN)
          .put(THERAPY, DONOR)
          .put(SURGERY, DONOR)
          .put(FAMILY, DONOR)
          .put(EXPOSURE, DONOR)
          .put(BIOMARKER, DONOR)
          .put(SSM_M, SAMPLE)
          .put(SSM_P, SSM_M)
          .put(CNSM_M, SAMPLE)
          .put(CNSM_P, CNSM_M)
          .put(CNSM_S, CNSM_P)
          .put(STSM_M, SAMPLE)
          .put(STSM_P, STSM_M)
          .put(STSM_S, STSM_P)
          .put(METH_ARRAY_M, SAMPLE)
          .put(METH_ARRAY_P, METH_ARRAY_M)
          .put(METH_SEQ_M, SAMPLE)
          .put(METH_SEQ_P, METH_SEQ_M)
          .put(EXP_ARRAY_M, SAMPLE)
          .put(EXP_ARRAY_P, EXP_ARRAY_M)
          .put(EXP_SEQ_M, SAMPLE)
          .put(EXP_SEQ_P, EXP_SEQ_M)
          .put(MIRNA_SEQ_M, SAMPLE)
          .put(MIRNA_SEQ_P, MIRNA_SEQ_M)
          .put(PEXP_M, SAMPLE)
          .put(PEXP_P, PEXP_M)
          .put(JCN_M, SAMPLE)
          .put(JCN_P, JCN_M)
          .put(SGV_M, SAMPLE)
          .put(SGV_P, SGV_M)
          .build();

  private static final Map<KVFileType, KVFileType> RELATIONS2 =
      new ImmutableMap.Builder<KVFileType, KVFileType>()
          .put(METH_ARRAY_P, METH_ARRAY_PROBES)
          .put(SURGERY, SPECIMEN)
          .put(BIOMARKER, SPECIMEN)
          .build();

  /**
   * Redundant with {@link #ERROR_TYPE_DESCRIPTIONS}?
   */
  private static final Map<KVFileType, List<String>> PKS =
      new ImmutableMap.Builder<KVFileType, List<String>>()
          .put(DONOR, DONOR_PK_NAMES)
          .put(SPECIMEN, SPECIMEN_PK_NAMES)
          .put(SAMPLE, SAMPLE_PK_NAMES)
          .put(FAMILY, FAMILY_PK_NAMES)
          .put(SSM_M, SSM_M_PK_NAMES)
          .put(CNSM_M, CNSM_M_PK_NAMES)
          .put(STSM_M, STSM_M_PK_NAMES)
          .put(JCN_M, JCN_M_PK_NAMES)
          .put(SGV_M, SGV_M_PK_NAMES)
          .put(PEXP_M, PEXP_M_PK_NAMES)
          .put(METH_ARRAY_M, METH_ARRAY_M_PK_NAMES)
          .put(METH_SEQ_M, METH_SEQ_M_PK_NAMES)
          .put(EXP_ARRAY_M, EXP_ARRAY_M_PK_NAMES)
          .put(EXP_SEQ_M, EXP_SEQ_M_PK_NAMES)
          .put(MIRNA_SEQ_M, MIRNA_SEQ_M_PK_NAMES)
          .put(CNSM_P, CNSM_P_PK_NAMES)
          .put(STSM_P, STSM_P_PK_NAMES)
          .put(EXP_ARRAY_P, EXP_ARRAY_P_PK_NAMES)
          .put(EXP_SEQ_P, EXP_SEQ_P_PK_NAMES)

  .put(METH_ARRAY_PROBES, METH_ARRAY_SYSTEM_PK_NAMES)

  .build();

  /**
   * Redundant with {@link #ERROR_TYPE_DESCRIPTIONS}?
   */
  private static final Map<KVFileType, List<String>> SURJECTION_FKS =
      new ImmutableMap.Builder<KVFileType, List<String>>()
          .put(SPECIMEN, SPECIMEN_FK_NAMES)
          .put(SAMPLE, SAMPLE_FK_NAMES)
          .put(SSM_P, SSM_P_FK_NAMES)
          .put(CNSM_P, CNSM_P_FK_NAMES)
          .put(STSM_P, STSM_P_FK_NAMES)
          .put(JCN_P, JCN_P_FK_NAMES)
          .put(SGV_P, SGV_P_FK_NAMES)
          .put(PEXP_P, PEXP_P_FK_NAMES)
          .put(METH_ARRAY_P, METH_ARRAY_P_FK1_NAMES)
          .put(METH_SEQ_P, METH_SEQ_P_FK_NAMES)
          .put(EXP_ARRAY_P, EXP_ARRAY_P_FK_NAMES)
          .put(EXP_SEQ_P, EXP_SEQ_P_FK_NAMES)
          .put(MIRNA_SEQ_P, MIRNA_SEQ_P_FK_NAMES)
          .put(CNSM_S, CNSM_S_FK_NAMES)
          .put(STSM_S, STSM_S_FK_NAMES)

  .build();

  private static final Predicate<KVFileType> SURJECTION_RELATION = new Predicate<KVFileType>() {

    @Override
    public boolean apply(KVFileType fileType) {
      return fileType == SPECIMEN
          || fileType == SAMPLE
          || fileType == SSM_P
          || fileType == CNSM_P
          || fileType == STSM_P
          || fileType == JCN_P
          || fileType == METH_ARRAY_P
          || fileType == METH_SEQ_P
          || fileType == EXP_ARRAY_P
          || fileType == EXP_SEQ_P
          || fileType == MIRNA_SEQ_P
          || fileType == PEXP_P
          || fileType == SGV_P;
    }
  };

  private static final Map<KVFileType, KVFileTypeErrorFields> ERROR_TYPE_DESCRIPTIONS =
      new ImmutableMap.Builder<KVFileType, KVFileTypeErrorFields>()

  // CLINICAL
          .put(
              DONOR,
              new KVFileTypeErrorFields.Builder(DONOR)
                  .pkFieldNames(DONOR_PK_NAMES)
                  .build())
          .put(
              SPECIMEN,
              new KVFileTypeErrorFields.Builder(SPECIMEN)
                  .pkFieldNames(SPECIMEN_PK_NAMES)
                  .fk1FieldNames(SPECIMEN_FK_NAMES)
                  .build())
          .put(
              SAMPLE,
              new KVFileTypeErrorFields.Builder(SAMPLE)
                  .pkFieldNames(SAMPLE_PK_NAMES)
                  .fk1FieldNames(SAMPLE_FK_NAMES)
                  .build())

  // SUPPLEMENTAL
          .put(
              BIOMARKER,
              new KVFileTypeErrorFields.Builder(BIOMARKER)
                  .pkFieldNames(BIOMARKER_PK_NAMES)
                  .fk1FieldNames(BIOMARKER_FK1_NAMES)
                  .fk2FieldNames(BIOMARKER_FK2_NAMES)
                  .build())
          .put(
              FAMILY,
              new KVFileTypeErrorFields.Builder(FAMILY)
                  .pkFieldNames(FAMILY_PK_NAMES)
                  .fk1FieldNames(FAMILY_FK_NAMES)
                  .build())
          .put(
              EXPOSURE,
              new KVFileTypeErrorFields.Builder(EXPOSURE)
                  .pkFieldNames(EXPOSURE_PK_NAMES)
                  .fk1FieldNames(EXPOSURE_FK_NAMES)
                  .build())
          .put(
              SURGERY,
              new KVFileTypeErrorFields.Builder(SURGERY)
                  .pkFieldNames(SURGERY_PK_NAMES)
                  .fk1FieldNames(SURGERY_FK1_NAMES)
                  .fk2FieldNames(SURGERY_FK2_NAMES)
                  .build())
          .put(
              THERAPY,
              new KVFileTypeErrorFields.Builder(THERAPY)
                  .pkFieldNames(THERAPY_PK_NAMES)
                  .fk1FieldNames(THERAPY_FK_NAMES)
                  .build())

  // SSM
          .put(
              SSM_M,
              new KVFileTypeErrorFields.Builder(SSM_M)
                  .pkFieldNames(SSM_M_PK_NAMES)
                  .fk1FieldNames(SSM_M_FK_NAMES)
                  .optionalFkFieldNames(SSM_M_OPTIONAL_FK_NAMES)
                  .build())
          .put(
              SSM_P,
              new KVFileTypeErrorFields.Builder(SSM_P)
                  .fk1FieldNames(SSM_P_FK_NAMES)
                  .build())

  // CNSM
          .put(
              CNSM_M,
              new KVFileTypeErrorFields.Builder(CNSM_M)
                  .pkFieldNames(CNSM_M_PK_NAMES)
                  .fk1FieldNames(CNSM_M_FK_NAMES)
                  .optionalFkFieldNames(CNSM_M_OPTIONAL_FK_NAMES)
                  .build())
          .put(
              CNSM_P,
              new KVFileTypeErrorFields.Builder(CNSM_P)
                  .pkFieldNames(CNSM_P_PK_NAMES)
                  .fk1FieldNames(CNSM_P_FK_NAMES)
                  .build())
          .put(
              CNSM_S,
              new KVFileTypeErrorFields.Builder(CNSM_S)
                  .fk1FieldNames(CNSM_S_FK_NAMES)
                  .build())

  // STSM
          .put(
              STSM_M,
              new KVFileTypeErrorFields.Builder(STSM_M)
                  .pkFieldNames(STSM_M_PK_NAMES)
                  .fk1FieldNames(STSM_M_FK_NAMES)
                  .optionalFkFieldNames(STSM_M_OPTIONAL_FK_NAMES)
                  .build())
          .put(
              STSM_P,
              new KVFileTypeErrorFields.Builder(STSM_P)
                  .pkFieldNames(STSM_P_PK_NAMES)
                  .fk1FieldNames(STSM_P_FK_NAMES)
                  .build())
          .put(
              STSM_S,
              new KVFileTypeErrorFields.Builder(STSM_S)
                  .fk1FieldNames(STSM_S_FK_NAMES)
                  .build())

  // MIRNA SEQ
          .put(
              MIRNA_SEQ_M,
              new KVFileTypeErrorFields.Builder(MIRNA_SEQ_M)
                  .pkFieldNames(MIRNA_SEQ_M_PK_NAMES)
                  .fk1FieldNames(MIRNA_SEQ_M_FK_NAMES)
                  .build())
          .put(
              MIRNA_SEQ_P,
              new KVFileTypeErrorFields.Builder(MIRNA_SEQ_P)
                  .fk1FieldNames(MIRNA_SEQ_P_FK_NAMES)
                  .build())

  // METH ARRAY
          .put(
              METH_ARRAY_M,
              new KVFileTypeErrorFields.Builder(METH_ARRAY_M)
                  .pkFieldNames(METH_ARRAY_M_PK_NAMES)
                  .fk1FieldNames(METH_ARRAY_M_FK_NAMES)
                  .build())
          .put(
              METH_ARRAY_P,
              new KVFileTypeErrorFields.Builder(METH_ARRAY_P)
                  .fk1FieldNames(METH_ARRAY_P_FK1_NAMES)
                  .fk2FieldNames(METH_ARRAY_P_FK2_NAMES)
                  .build())

  // METH SEQ
          .put(
              METH_SEQ_M,
              new KVFileTypeErrorFields.Builder(METH_SEQ_M)
                  .pkFieldNames(METH_SEQ_M_PK_NAMES)
                  .fk1FieldNames(METH_SEQ_M_FK_NAMES)
                  .build())
          .put(
              METH_SEQ_P,
              new KVFileTypeErrorFields.Builder(METH_SEQ_P)
                  .fk1FieldNames(METH_SEQ_P_FK_NAMES)
                  .build())

  // EXP ARRAY
          .put(
              EXP_ARRAY_M,
              new KVFileTypeErrorFields.Builder(EXP_ARRAY_M)
                  .pkFieldNames(EXP_ARRAY_M_PK_NAMES)
                  .fk1FieldNames(EXP_ARRAY_M_FK_NAMES)
                  .build())
          .put(
              EXP_ARRAY_P,
              new KVFileTypeErrorFields.Builder(EXP_ARRAY_P)
                  .pkFieldNames(EXP_ARRAY_P_PK_NAMES)
                  .fk1FieldNames(EXP_ARRAY_P_FK_NAMES)
                  .build())

  // EXP SEQ
          .put(
              EXP_SEQ_M,
              new KVFileTypeErrorFields.Builder(EXP_SEQ_M)
                  .pkFieldNames(EXP_SEQ_M_PK_NAMES)
                  .fk1FieldNames(EXP_SEQ_M_FK_NAMES)
                  .build())
          .put(
              EXP_SEQ_P,
              new KVFileTypeErrorFields.Builder(EXP_SEQ_P)
                  .pkFieldNames(EXP_SEQ_P_PK_NAMES)
                  .fk1FieldNames(EXP_SEQ_P_FK_NAMES)
                  .build())

  // PEXP
          .put(
              PEXP_M,
              new KVFileTypeErrorFields.Builder(PEXP_M)
                  .pkFieldNames(PEXP_M_PK_NAMES)
                  .fk1FieldNames(PEXP_M_FK_NAMES)
                  .build())
          .put(
              PEXP_P,
              new KVFileTypeErrorFields.Builder(PEXP_P)
                  .fk1FieldNames(PEXP_P_FK_NAMES)
                  .build())

  // JCN
          .put(
              JCN_M,
              new KVFileTypeErrorFields.Builder(JCN_M)
                  .pkFieldNames(JCN_M_PK_NAMES)
                  .fk1FieldNames(JCN_M_FK_NAMES)
                  .build())
          .put(
              JCN_P,
              new KVFileTypeErrorFields.Builder(JCN_P)
                  .fk1FieldNames(JCN_P_FK_NAMES)
                  .build())

  // SGV
          .put(
              SGV_M,
              new KVFileTypeErrorFields.Builder(SGV_M)
                  .pkFieldNames(SGV_M_PK_NAMES)
                  .fk1FieldNames(SGV_M_FK_NAMES)
                  .build())
          .put(
              SGV_P,
              new KVFileTypeErrorFields.Builder(SGV_P)
                  .fk1FieldNames(SGV_P_FK_NAMES)
                  .build())

  .build();

  /**
   * Order matters (referenced files first). TODO: get this from relations instead.
   */
  private static final Map<KVExperimentalDataType, List<KVFileType>> DATA_TYPE_FILE_TYPES =
      new ImmutableMap.Builder<KVExperimentalDataType, List<KVFileType>>()
          .put(SSM, of(SSM_M, SSM_P))
          .put(CNSM, of(CNSM_M, CNSM_P, CNSM_S))
          .put(STSM, of(STSM_M, STSM_P, STSM_S))
          .put(MIRNA_SEQ, of(MIRNA_SEQ_M, MIRNA_SEQ_P))
          .put(METH_ARRAY, of(METH_ARRAY_M, METH_ARRAY_PROBES, METH_ARRAY_P))
          .put(METH_SEQ, of(METH_SEQ_M, METH_SEQ_P))
          .put(EXP_ARRAY, of(EXP_ARRAY_M, EXP_ARRAY_P))
          .put(EXP_SEQ, of(EXP_SEQ_M, EXP_SEQ_P))
          .put(PEXP, of(PEXP_M, PEXP_P))
          .put(JCN, of(JCN_M, JCN_P))
          .put(SGV, of(SGV_M, SGV_P))
          .build();

  @Override
  public Iterable<KVExperimentalDataType> getExperimentalDataTypes() {
    return Arrays.asList(KVExperimentalDataType.values());
  }

  @Override
  public List<KVFileType> getExperimentalFileTypes(KVExperimentalDataType dataType) {
    return DATA_TYPE_FILE_TYPES.get(dataType);
  }

  @Override
  public boolean hasOutgoingSurjectiveRelation(KVFileType fileType) {
    val b = SURJECTION_RELATION.apply(fileType);
    checkState(!b || getOptionalReferencedFileType1(fileType).isPresent(),
        "Expecting a referenced type at this point for '{}'", fileType);
    return b;
  }

  @Override
  public KVFileTypeKeysIndices getKeysIndices(KVFileType fileType) {
    KVFileTypeKeysIndices keysIndices = null;

    // CLINICAL
    if (fileType == DONOR) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(DONOR_PKS)
          .build();
    } else if (fileType == SPECIMEN) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(SPECIMEN_PKS)
          .fk1(SPECIMEN_FKS)
          .build();
    } else if (fileType == SAMPLE) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(SAMPLE_PKS)
          .fk1(SAMPLE_FKS)
          .build();
    }

    // SUPPLEMENTAL
    else if (fileType == BIOMARKER) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(BIOMARKER_PKS)
          .fk1(BIOMARKER_FKS1)
          .fk2(BIOMARKER_FKS2)
          .build();
    } else if (fileType == FAMILY) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(FAMILY_PKS)
          .fk1(FAMILY_FKS)
          .build();
    } else if (fileType == EXPOSURE) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(EXPOSURE_PKS)
          .fk1(EXPOSURE_FKS)
          .build();
    } else if (fileType == SURGERY) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(SURGERY_PKS)
          .fk1(SURGERY_FKS1)
          .fk2(SURGERY_FKS2)
          .build();
    } else if (fileType == THERAPY) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(THERAPY_PKS)
          .fk1(THERAPY_FKS)
          .build();
    }

    // SSM
    else if (fileType == SSM_M) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(SSM_M_PKS)
          .fk1(SSM_M_FKS1)
          .optionalFk(SSM_M_OPTIONAL_FKS)
          .build();
    } else if (fileType == SSM_P) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .fk1(SSM_P_FKS)
          .build();
    }

    // CNSM
    else if (fileType == CNSM_M) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(CNSM_M_PKS)
          .fk1(CNSM_M_FKS1)
          .optionalFk(CNSM_M_OPTIONAL_FKS)
          .build();
    } else if (fileType == CNSM_P) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(CNSM_P_PKS)
          .fk1(CNSM_P_FKS)
          .build();
    } else if (fileType == CNSM_S) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .fk1(CNSM_S_FKS)
          .build();
    }

    // STSM
    else if (fileType == STSM_M) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(STSM_M_PKS)
          .fk1(STSM_M_FKS1)
          .optionalFk(STSM_M_OPTIONAL_FKS)
          .build();
    } else if (fileType == STSM_P) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(STSM_P_PKS)
          .fk1(STSM_P_FKS)
          .build();
    } else if (fileType == STSM_S) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .fk1(STSM_S_FKS)
          .build();
    }

    // MIRNA SEQ
    else if (fileType == MIRNA_SEQ_M) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(MIRNA_SEQ_M_PKS)
          .fk1(MIRNA_SEQ_M_FKS)
          .build();
    } else if (fileType == MIRNA_SEQ_P) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .fk1(MIRNA_SEQ_P_FKS)
          .build();
    }

    // METH ARRAY
    else if (fileType == METH_ARRAY_M) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(METH_ARRAY_M_PKS)
          .fk1(METH_ARRAY_M_FKS)
          .build();
    } else if (fileType == METH_ARRAY_PROBES) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(METH_ARRAY_SYSTEM_PKS)
          .build();
    } else if (fileType == METH_ARRAY_P) {
      // In theory there is a PK but it's not enforced
      keysIndices = KVFileTypeKeysIndices.builder()
          .fk1(METH_ARRAY_P_FKS1)
          .fk2(METH_ARRAY_P_FKS2)
          .build();
    }

    // METH SEQ
    else if (fileType == METH_SEQ_M) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(METH_SEQ_M_PKS)
          .fk1(METH_SEQ_M_FKS)
          .build();
    } else if (fileType == METH_SEQ_P) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .fk1(METH_SEQ_P_FKS)
          .build();
    }

    // EXP SEQ
    else if (fileType == EXP_SEQ_M) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(EXP_SEQ_M_PKS)
          .fk1(EXP_SEQ_M_FKS)
          .build();
    } else if (fileType == EXP_SEQ_P) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(EXP_SEQ_P_PKS)
          .fk1(EXP_SEQ_P_FKS)
          .build();
    }

    // EXP ARRAY
    else if (fileType == EXP_ARRAY_M) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(EXP_ARRAY_M_PKS)
          .fk1(EXP_ARRAY_M_FKS)
          .build();
    } else if (fileType == EXP_ARRAY_P) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(EXP_ARRAY_P_PKS)
          .fk1(EXP_ARRAY_P_FKS)
          .build();
    }

    // PEXP
    else if (fileType == PEXP_M) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(PEXP_M_PKS)
          .fk1(PEXP_M_FKS)
          .build();
    } else if (fileType == PEXP_P) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .fk1(PEXP_P_FKS)
          .build();
    }

    // JCN
    else if (fileType == JCN_M) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(JCN_M_PKS)
          .fk1(JCN_M_FKS)
          .build();
    } else if (fileType == JCN_P) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .fk1(JCN_P_FKS)
          .build();
    }

    // SGV
    else if (fileType == SGV_M) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .pk(SGV_M_PKS)
          .fk1(SGV_M_FKS)
          .build();
    } else if (fileType == SGV_P) {
      keysIndices = KVFileTypeKeysIndices.builder()
          .fk1(SGV_P_FKS)
          .build();
    }

    return keysIndices;
  }

  @Override
  public Optional<KVFileType> getOptionalReferencedFileType1(KVFileType fileType) {
    return Optional.<KVFileType> fromNullable(RELATIONS1.get(fileType));
  }

  @Override
  public Optional<KVFileType> getOptionalReferencedFileType2(KVFileType fileType) {
    return Optional.<KVFileType> fromNullable(RELATIONS2.get(fileType));
  }

  @Override
  public List<String> getErrorFieldNames(KVFileType fileType, KVErrorType errorType) {
    return ERROR_TYPE_DESCRIPTIONS
        .get(fileType)
        .getErrorFieldNames(errorType);
  }

  @Override
  public List<String> getPrimaryKeyNames(KVFileType fileType) {
    return PKS.get(fileType);
  }

  @Override
  public List<String> getSurjectionForeignKeyNames(KVFileType fileType) {
    return SURJECTION_FKS.get(fileType);
  }

  /**
   * Do *not* apply with {@link KVFileType#SAMPLE}.
   */
  @Override
  public KVFileType getReferencingFileType(KVFileType fileType) {
    checkState(fileType != SAMPLE, "Not applicable for sample since it has multiple referencing types");
    KVFileType referencingFileType = null;
    for (val entry : RELATIONS1.entrySet()) {
      if (entry.getValue() == fileType) {
        checkState(referencingFileType == null,
            "There should be only one match for '%s'", fileType);
        referencingFileType = entry.getKey();
      }
    }
    return checkNotNull(referencingFileType,
        "There should be at least one match for '%s'", fileType);
  }

}
