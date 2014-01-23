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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.submission.validation.key.core.KVValidator.TUPLE_CHECKS_ENABLED;
import static org.icgc.dcc.submission.validation.key.data.KVKey.KEYS_NOT_APPLICABLE;
import static org.icgc.dcc.submission.validation.key.data.KVKey.from;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.BIOMARKER;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXPOSURE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_G;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.FAMILY;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.JCN_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.JCN_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.PEXP_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.PEXP_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SGV_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SGV_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.STSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.STSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.STSM_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SURGERY;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.THERAPY;

import java.util.List;
import java.util.Map;

import lombok.NoArgsConstructor;

import org.icgc.dcc.submission.validation.key.data.KVFileTypeErrorFields;
import org.icgc.dcc.submission.validation.key.data.KVKey;
import org.icgc.dcc.submission.validation.key.data.KVTuple;
import org.icgc.dcc.submission.validation.key.enumeration.KVErrorType;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;

import com.google.common.collect.ImmutableMap;

@NoArgsConstructor(access = PRIVATE)
public final class KVDictionary {

  // TODO: translate to Strings rather? + make map per file type/submission type?
  public static final List<Integer> CNSM_M_FKS1 = newArrayList(1);
  public static final List<Integer> CNSM_M_FKS2 = newArrayList(2);
  public static final List<Integer> CNSM_M_PKS = newArrayList(0, 1);
  public static final List<Integer> CNSM_P_FKS = newArrayList(0, 1);
  public static final List<Integer> CNSM_P_PKS = newArrayList(0, 1, 2);
  public static final List<Integer> CNSM_S_FKS = newArrayList(0, 1, 2);
  public static final List<Integer> DONOR_PKS = newArrayList(0);
  public static final List<Integer> EXP_G_FKS = newArrayList(0, 1);
  public static final List<Integer> EXP_M_FKS = newArrayList(1);
  public static final List<Integer> EXP_M_PKS = newArrayList(0, 1);
  public static final List<Integer> JCN_M_FKS = newArrayList(1);
  public static final List<Integer> JCN_M_PKS = newArrayList(0, 1);
  public static final List<Integer> JCN_P_FKS = newArrayList(0, 1);
  public static final List<Integer> MIRNA_M_FKS = newArrayList(1);
  public static final List<Integer> MIRNA_M_PKS = newArrayList(0, 1);
  public static final List<Integer> MIRNA_P_FKS = newArrayList(0, 1);
  public static final List<Integer> MIRNA_P_PKS = newArrayList(3); // Special case
  public static final List<Integer> MIRNA_S_FKS = newArrayList(0);
  public static final List<Integer> MIRNA_S_PKS = newArrayList(0, 1, 2, 3, 4);
  public static final List<Integer> PEXP_M_FKS = newArrayList(1);
  public static final List<Integer> PEXP_M_PKS = newArrayList(0, 1);
  public static final List<Integer> PEXP_P_FKS = newArrayList(0, 1);
  public static final List<Integer> SAMPLE_FKS = newArrayList(1);
  public static final List<Integer> SAMPLE_PKS = newArrayList(0);
  public static final List<Integer> SGV_M_FKS = newArrayList(1);
  public static final List<Integer> SGV_M_PKS = newArrayList(0, 1);
  public static final List<Integer> SGV_P_FKS = newArrayList(0, 1);
  public static final List<Integer> SPECIMEN_FKS = newArrayList(0);
  public static final List<Integer> SPECIMEN_PKS = newArrayList(1);
  public static final List<Integer> SSM_M_FKS1 = newArrayList(1);
  public static final List<Integer> SSM_M_FKS2 = newArrayList(2);
  public static final List<Integer> SSM_M_PKS = newArrayList(0, 1);
  public static final List<Integer> SSM_P_FKS = newArrayList(0, 1);
  public static final List<Integer> STSM_M_FKS1 = newArrayList(1);
  public static final List<Integer> STSM_M_FKS2 = newArrayList(2);
  public static final List<Integer> STSM_M_PKS = newArrayList(0, 1);
  public static final List<Integer> STSM_P_FKS = newArrayList(0, 1);
  public static final List<Integer> STSM_P_PKS = newArrayList(0, 1, 2, 3);
  public static final List<Integer> STSM_S_FKS = newArrayList(0, 1, 2, 3);
  public static final List<Integer> METH_M_FKS1 = newArrayList(1);
  public static final List<Integer> METH_M_FKS2 = newArrayList(2);
  public static final List<Integer> METH_M_PKS = newArrayList(0, 1);
  public static final List<Integer> METH_P_FKS = newArrayList(0, 1);
  public static final List<Integer> METH_P_PKS = newArrayList(0, 1, 2);
  public static final List<Integer> METH_S_FKS = newArrayList(0, 1, 2);

  public static final List<String> KEY_NAMES_NOT_APPLICABLE = null;
  public static final List<String> CNSM_M_FK1_NAMES = newArrayList("analyzed_sample_id");
  public static final List<String> CNSM_M_FK2_NAMES = newArrayList("matched_sample_id");
  public static final List<String> CNSM_M_PK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> CNSM_P_FK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> CNSM_P_PK_NAMES = newArrayList("analysis_id", "analyzed_sample_id", "mutation_id");
  public static final List<String> CNSM_S_FK_NAMES = newArrayList("analysis_id", "analyzed_sample_id", "mutation_id");
  public static final List<String> DONOR_PK_NAMES = newArrayList("donor_id");
  public static final List<String> EXP_G_FK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> EXP_M_FK_NAMES = newArrayList("analyzed_sample_id");
  public static final List<String> EXP_M_PK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> JCN_M_FK_NAMES = newArrayList("analyzed_sample_id");
  public static final List<String> JCN_M_PK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> JCN_P_FK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> MIRNA_M_FK_NAMES = newArrayList("analyzed_sample_id");
  public static final List<String> MIRNA_M_PK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> MIRNA_P_FK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> MIRNA_P_PK_NAMES = newArrayList("mirna_seq"); // Special case
  public static final List<String> MIRNA_S_FK_NAMES = newArrayList("mirna_seq");
  public static final List<String> MIRNA_S_PK_NAMES = newArrayList("mirna_seq", "chromosome", "chromosome_start",
      "chromosome_end", "chromosome_strand");
  public static final List<String> PEXP_M_FK_NAMES = newArrayList("analyzed_sample_id");
  public static final List<String> PEXP_M_PK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> PEXP_P_FK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> SAMPLE_FK_NAMES = newArrayList("specimen_id");
  public static final List<String> SAMPLE_PK_NAMES = newArrayList("analyzed_sample_id");
  public static final List<String> SGV_M_FK_NAMES = newArrayList("analyzed_sample_id");
  public static final List<String> SGV_M_PK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> SGV_P_FK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> SPECIMEN_FK_NAMES = newArrayList("donor_id");
  public static final List<String> SPECIMEN_PK_NAMES = newArrayList("specimen_id");
  public static final List<String> SSM_M_FK1_NAMES = newArrayList("analyzed_sample_id");
  public static final List<String> SSM_M_FK2_NAMES = newArrayList("matched_sample_id");
  public static final List<String> SSM_M_PK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> SSM_P_FK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> STSM_M_FK1_NAMES = newArrayList("analyzed_sample_id");
  public static final List<String> STSM_M_FK2_NAMES = newArrayList("matched_sample_id");
  public static final List<String> STSM_M_PK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> STSM_P_FK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> STSM_P_PK_NAMES = newArrayList("analysis_id", "analyzed_sample_id", "placement",
      "sv_id");
  public static final List<String> STSM_S_FK_NAMES = newArrayList("analysis_id", "analyzed_sample_id", "sv_id",
      "placement");
  public static final List<String> METH_M_FK1_NAMES = newArrayList("analyzed_sample_id");
  public static final List<String> METH_M_FK2_NAMES = newArrayList("matched_sample_id");
  public static final List<String> METH_M_PK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> METH_P_FK_NAMES = newArrayList("analysis_id", "analyzed_sample_id");
  public static final List<String> METH_P_PK_NAMES = newArrayList("analysis_id", "analyzed_sample_id",
      "methylated_fragment_id");
  public static final List<String> METH_S_FK_NAMES = newArrayList("analysis_id", "analyzed_sample_id",
      "methylated_fragment_id");

  // TODO: merge the two following into one data structure (as well as what's above)
  public static final Map<KVFileType, KVFileType> RELATIONS = // TODO: all necessary?
      new ImmutableMap.Builder<KVFileType, KVFileType>()
          .put(SPECIMEN, DONOR)
          .put(SAMPLE, SPECIMEN)

          .put(BIOMARKER, DONOR)
          .put(EXPOSURE, DONOR)
          .put(FAMILY, DONOR)
          .put(SURGERY, DONOR)
          .put(THERAPY, DONOR)

          .put(SSM_M, SAMPLE)
          .put(SSM_P, SSM_M)

          .put(CNSM_M, SAMPLE)
          .put(CNSM_P, CNSM_M)
          .put(CNSM_S, CNSM_P)

          .put(STSM_M, SAMPLE)
          .put(STSM_P, STSM_M)
          .put(STSM_S, STSM_P)

          .put(MIRNA_M, SAMPLE)
          .put(MIRNA_P, MIRNA_M)
          .put(MIRNA_S, MIRNA_P)

          .put(METH_M, SAMPLE)
          .put(METH_P, METH_M)
          .put(METH_S, METH_P)

          .put(EXP_M, SAMPLE)
          .put(EXP_G, EXP_M)

          .put(PEXP_M, SAMPLE)
          .put(PEXP_P, PEXP_M)

          .put(JCN_M, SAMPLE)
          .put(JCN_P, JCN_M)

          .put(SGV_M, SAMPLE)
          .put(SGV_P, SGV_M)

          .build();

  public static final Map<KVFileType, List<String>> PKS =
      new ImmutableMap.Builder<KVFileType, List<String>>()
          .put(DONOR, DONOR_PK_NAMES)
          .put(SPECIMEN, SPECIMEN_PK_NAMES)
          .put(SAMPLE, SAMPLE_PK_NAMES)

          .put(SSM_M, SSM_M_PK_NAMES)
          .put(CNSM_M, CNSM_M_PK_NAMES)
          .put(STSM_M, STSM_M_PK_NAMES)
          .put(MIRNA_M, MIRNA_M_PK_NAMES)
          .put(METH_M, METH_M_PK_NAMES)
          .put(EXP_M, EXP_M_PK_NAMES)
          .put(PEXP_M, PEXP_M_PK_NAMES)
          .put(JCN_M, JCN_M_PK_NAMES)
          .put(SGV_M, SGV_M_PK_NAMES)

          .put(CNSM_P, CNSM_P_PK_NAMES)
          .put(STSM_P, STSM_P_PK_NAMES)
          .put(MIRNA_P, MIRNA_P_PK_NAMES)
          .put(METH_P, METH_P_PK_NAMES)

          .build();

  public static final Map<KVFileType, List<String>> SIMPLE_SURJECTION_FKS =
      new ImmutableMap.Builder<KVFileType, List<String>>()
          .put(SPECIMEN, SPECIMEN_FK_NAMES)
          .put(SAMPLE, SAMPLE_FK_NAMES)

          .put(SSM_P, SSM_P_FK_NAMES)
          .put(CNSM_P, CNSM_P_FK_NAMES)
          .put(STSM_P, STSM_P_FK_NAMES)
          .put(MIRNA_P, MIRNA_P_FK_NAMES)
          .put(METH_P, METH_P_FK_NAMES)
          .put(EXP_G, EXP_G_FK_NAMES)
          .put(PEXP_P, PEXP_P_FK_NAMES)
          .put(JCN_P, JCN_P_FK_NAMES)
          .put(SGV_P, SGV_P_FK_NAMES)

          .put(CNSM_S, CNSM_S_FK_NAMES)
          .put(STSM_S, STSM_S_FK_NAMES)
          .put(MIRNA_S, MIRNA_S_FK_NAMES)
          .put(METH_S, METH_S_FK_NAMES)

          .build();

  /**
   * TODO: encode in dictionary data structure rather (hardcoded elsewhere, at least the PKs)
   */
  public static KVTuple getTuple(KVFileType fileType, List<String> row) {
    KVKey pk = null, fk1 = null, fk2 = null;

    // Clinical
    if (fileType == DONOR) {
      pk = from(row, DONOR_PKS);
      fk1 = KEYS_NOT_APPLICABLE;
      fk2 = KEYS_NOT_APPLICABLE;
    } else if (fileType == SPECIMEN) {
      pk = from(row, SPECIMEN_PKS);
      fk1 = from(row, SPECIMEN_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    } else if (fileType == SAMPLE) {
      pk = from(row, SAMPLE_PKS);
      fk1 = from(row, SAMPLE_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    }

    // Ssm
    else if (fileType == SSM_M) {
      pk = from(row, SSM_M_PKS);
      fk1 = from(row, SSM_M_FKS1);
      fk2 = from(row, SSM_M_FKS2); // TODO: handle case where value is null or a missing code
    } else if (fileType == SSM_P) {
      pk = KEYS_NOT_APPLICABLE;
      fk1 = from(row, SSM_P_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    }

    // Cnsm
    else if (fileType == CNSM_M) {
      pk = from(row, CNSM_M_PKS);
      fk1 = from(row, CNSM_M_FKS1);
      fk2 = from(row, CNSM_M_FKS2);
    } else if (fileType == CNSM_P) {
      pk = from(row, CNSM_P_PKS);
      fk1 = from(row, CNSM_P_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    } else if (fileType == CNSM_S) {
      pk = KEYS_NOT_APPLICABLE;
      fk1 = from(row, CNSM_S_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    }

    // Stsm
    else if (fileType == STSM_M) {
      pk = from(row, STSM_M_PKS);
      fk1 = from(row, STSM_M_FKS1);
      fk2 = from(row, STSM_M_FKS2);
    } else if (fileType == STSM_P) {
      pk = from(row, STSM_P_PKS);
      fk1 = from(row, STSM_P_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    } else if (fileType == STSM_S) {
      pk = KEYS_NOT_APPLICABLE;
      fk1 = from(row, STSM_S_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    }

    // Mirna
    else if (fileType == MIRNA_M) {
      pk = from(row, MIRNA_M_PKS);
      fk1 = from(row, MIRNA_M_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    } else if (fileType == MIRNA_P) {
      pk = KEYS_NOT_APPLICABLE; // Special case: uniqueness is not enforced
      fk1 = from(row, MIRNA_P_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    } else if (fileType == MIRNA_S) {
      pk = from(row, MIRNA_S_PKS);
      fk1 = from(row, MIRNA_S_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    }

    // Meth
    else if (fileType == METH_M) {
      pk = from(row, METH_M_PKS);
      fk1 = from(row, METH_M_FKS1);
      fk1 = from(row, METH_M_FKS2);
    } else if (fileType == METH_P) {
      pk = from(row, METH_P_PKS);
      fk1 = from(row, METH_P_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    } else if (fileType == METH_S) {
      pk = KEYS_NOT_APPLICABLE;
      fk1 = from(row, METH_S_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    }

    // Exp
    else if (fileType == EXP_M) {
      pk = from(row, EXP_M_PKS);
      fk1 = from(row, EXP_M_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    } else if (fileType == EXP_G) {
      pk = KEYS_NOT_APPLICABLE;
      fk1 = from(row, EXP_G_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    }

    // Pexp
    else if (fileType == SSM_M) {
      pk = from(row, PEXP_M_PKS);
      fk1 = from(row, PEXP_M_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    } else if (fileType == PEXP_P) {
      pk = KEYS_NOT_APPLICABLE;
      fk1 = from(row, PEXP_P_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    }

    // Jcn
    else if (fileType == JCN_M) {
      pk = from(row, JCN_M_PKS);
      fk1 = from(row, JCN_M_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    } else if (fileType == JCN_P) {
      pk = KEYS_NOT_APPLICABLE;
      fk1 = from(row, JCN_P_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    }

    // Sgv
    else if (fileType == SGV_M) {
      pk = from(row, SGV_M_PKS);
      fk1 = from(row, SGV_M_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    } else if (fileType == SGV_P) {
      pk = KEYS_NOT_APPLICABLE;
      fk1 = from(row, SGV_P_FKS);
      fk2 = KEYS_NOT_APPLICABLE;
    }

    if (TUPLE_CHECKS_ENABLED) checkState(pk != null || fk1 != null, "TODO: '%s'", row);
    return new KVTuple(pk, fk1, fk2);
  }

  private static final Map<KVFileType, KVFileTypeErrorFields> ERROR_TYPE_DESCRIPTIONS =
      new ImmutableMap.Builder<KVFileType, KVFileTypeErrorFields>()
          .put(
              DONOR,
              new KVFileTypeErrorFields(DONOR, DONOR_PK_NAMES, KEY_NAMES_NOT_APPLICABLE, KEY_NAMES_NOT_APPLICABLE))
          .put(
              SPECIMEN,
              new KVFileTypeErrorFields(SPECIMEN, SPECIMEN_PK_NAMES, SPECIMEN_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))
          .put(
              SAMPLE,
              new KVFileTypeErrorFields(SAMPLE, SAMPLE_PK_NAMES, SAMPLE_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))

          // SSM
          .put(
              SSM_M,
              new KVFileTypeErrorFields(SSM_M, SSM_M_PK_NAMES, SSM_M_FK1_NAMES, SSM_M_FK2_NAMES))
          .put(
              SSM_P,
              new KVFileTypeErrorFields(SSM_P, KEY_NAMES_NOT_APPLICABLE, SSM_P_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))

          // CNSM
          .put(
              CNSM_M,
              new KVFileTypeErrorFields(CNSM_M, CNSM_M_PK_NAMES, CNSM_M_FK1_NAMES, CNSM_M_FK2_NAMES))
          .put(
              CNSM_P,
              new KVFileTypeErrorFields(CNSM_P, CNSM_P_PK_NAMES, CNSM_P_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))
          .put(
              CNSM_S,
              new KVFileTypeErrorFields(CNSM_S, KEY_NAMES_NOT_APPLICABLE, CNSM_S_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))

          // STSM
          .put(
              STSM_M,
              new KVFileTypeErrorFields(STSM_M, STSM_M_PK_NAMES, STSM_M_FK1_NAMES, STSM_M_FK2_NAMES))
          .put(
              STSM_P,
              new KVFileTypeErrorFields(STSM_P, STSM_P_PK_NAMES, STSM_P_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))
          .put(
              STSM_S,
              new KVFileTypeErrorFields(STSM_S, KEY_NAMES_NOT_APPLICABLE, STSM_S_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))

          // MIRNA
          .put(
              MIRNA_M,
              new KVFileTypeErrorFields(MIRNA_M, MIRNA_M_PK_NAMES, MIRNA_M_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))
          .put(
              MIRNA_P,
              new KVFileTypeErrorFields(MIRNA_P, KEY_NAMES_NOT_APPLICABLE, MIRNA_P_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))
          .put(
              MIRNA_S,
              new KVFileTypeErrorFields(MIRNA_S, MIRNA_S_PK_NAMES, MIRNA_S_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))

          // METH
          .put(
              METH_M,
              new KVFileTypeErrorFields(METH_M, METH_M_PK_NAMES, METH_M_FK1_NAMES, METH_M_FK2_NAMES))
          .put(
              METH_P,
              new KVFileTypeErrorFields(METH_P, METH_P_PK_NAMES, METH_P_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))
          .put(
              METH_S,
              new KVFileTypeErrorFields(METH_S, KEY_NAMES_NOT_APPLICABLE, METH_S_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))

          // EXP
          .put(
              EXP_M,
              new KVFileTypeErrorFields(EXP_M, EXP_M_PK_NAMES, EXP_M_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))
          .put(
              EXP_G,
              new KVFileTypeErrorFields(EXP_G, KEY_NAMES_NOT_APPLICABLE, EXP_G_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))

          // PEXP
          .put(
              PEXP_M,
              new KVFileTypeErrorFields(PEXP_M, PEXP_M_PK_NAMES, PEXP_M_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))
          .put(
              PEXP_P,
              new KVFileTypeErrorFields(PEXP_P, KEY_NAMES_NOT_APPLICABLE, PEXP_P_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))

          // JCN
          .put(
              JCN_M,
              new KVFileTypeErrorFields(JCN_M, JCN_M_PK_NAMES, JCN_M_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))
          .put(
              JCN_P,
              new KVFileTypeErrorFields(JCN_P, KEY_NAMES_NOT_APPLICABLE, JCN_P_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))

          // SGV
          .put(
              SGV_M,
              new KVFileTypeErrorFields(SGV_M, SGV_M_PK_NAMES, SGV_M_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))
          .put(
              SGV_P,
              new KVFileTypeErrorFields(SGV_P, KEY_NAMES_NOT_APPLICABLE, SGV_P_FK_NAMES, KEY_NAMES_NOT_APPLICABLE))

          .build();

  public static List<String> getErrorFieldNames(KVFileType fileType, KVErrorType errorType) {
    return ERROR_TYPE_DESCRIPTIONS
        .get(fileType)
        .getErrorFieldNames(errorType);
  }
}
