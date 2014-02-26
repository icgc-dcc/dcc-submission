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
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.submission.validation.key.core.KVSubmissionProcessor.ROW_CHECKS_ENABLED;
import static org.icgc.dcc.submission.validation.key.data.KVKey.KEY_NOT_APPLICABLE;
import static org.icgc.dcc.submission.validation.key.data.KVKey.from;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_ARRAY_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_ARRAY_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_G;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_SEQ_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_SEQ_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.JCN_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.JCN_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_ARRAY_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_ARRAY_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_ARRAY_PROBES;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_SEQ_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_SEQ_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_SEQ_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_SEQ_P;
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

import java.util.List;
import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.val;

import org.icgc.dcc.submission.validation.key.data.KVFileTypeErrorFields;
import org.icgc.dcc.submission.validation.key.data.KVKey;
import org.icgc.dcc.submission.validation.key.data.KVRow;
import org.icgc.dcc.submission.validation.key.enumeration.KVErrorType;
import org.icgc.dcc.submission.validation.key.enumeration.KVExperimentalDataType;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;

/**
 * TODO:
 * <p>
 * - make non-static + parse dictionary rather than hardcoding<br/>
 * - MUST also incorporate {@link KVExperimentalDataType} (which should be relation-driven instead)
 */
@NoArgsConstructor(access = PRIVATE)
public final class KVDictionary {

  private static final List<Integer> DONOR_PKS = of(0);

  // TODO: translate to Strings rather? + make map per file type/submission type?
  private static final List<Integer> CNSM_M_FKS1 = of(1);
  private static final List<Integer> CNSM_M_OPTIONAL_FKS = of(2);
  private static final List<Integer> CNSM_M_PKS = of(0, 1);
  private static final List<Integer> CNSM_P_FKS = of(0, 1);
  private static final List<Integer> CNSM_P_PKS = of(0, 1, 2);
  private static final List<Integer> CNSM_S_FKS = of(0, 1, 2);

  private static final List<Integer> EXP_G_FKS = of(0, 1);
  private static final List<Integer> EXP_M_FKS = of(1);
  private static final List<Integer> EXP_M_PKS = of(0, 1);

  private static final List<Integer> JCN_M_FKS = of(1);
  private static final List<Integer> JCN_M_PKS = of(0, 1);
  private static final List<Integer> JCN_P_FKS = of(0, 1);

  private static final List<Integer> PEXP_M_FKS = of(1);
  private static final List<Integer> PEXP_M_PKS = of(0, 1);
  private static final List<Integer> PEXP_P_FKS = of(0, 1);

  private static final List<Integer> SAMPLE_FKS = of(1);
  private static final List<Integer> SAMPLE_PKS = of(0);

  private static final List<Integer> SGV_M_FKS = of(1);
  private static final List<Integer> SGV_M_PKS = of(0, 1);
  private static final List<Integer> SGV_P_FKS = of(0, 1);

  private static final List<Integer> SPECIMEN_FKS = of(0);
  private static final List<Integer> SPECIMEN_PKS = of(1);

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

  // Old METH
  private static final List<Integer> METH_M_PKS = of(0, 1);
  private static final List<Integer> METH_M_FKS1 = of(1);
  private static final List<Integer> METH_M_OPTIONAL_FKS = of(2);
  private static final List<Integer> METH_P_FKS = of(0, 1);
  private static final List<Integer> METH_P_PKS = of(0, 1, 2);
  private static final List<Integer> METH_S_FKS = of(0, 1, 2);

  // Old MIRNA
  private static final List<Integer> MIRNA_M_PKS = of(0, 1);
  private static final List<Integer> MIRNA_M_FKS = of(1);
  // Special case: not a true PK (not unique), but referenced by MIRNA_S...
  private static final List<Integer> MIRNA_P_PSEUDO_PKS = of(2);
  private static final List<Integer> MIRNA_P_FKS = of(0, 1);
  private static final List<Integer> MIRNA_S_PKS = of(0, 1, 2, 3, 4);
  private static final List<Integer> MIRNA_S_FKS = of(0);

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

  private static final List<String> CNSM_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> CNSM_M_OPTIONAL_FK_NAMES = of("matched_sample_id");
  private static final List<String> CNSM_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> CNSM_P_FK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> CNSM_P_PK_NAMES = of("analysis_id", "analyzed_sample_id", "mutation_id");
  private static final List<String> CNSM_S_FK_NAMES = of("analysis_id", "analyzed_sample_id", "mutation_id");
  private static final List<String> DONOR_PK_NAMES = of("donor_id");

  private static final List<String> EXP_G_FK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> EXP_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> EXP_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");

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

  // MIRNA
  private static final List<String> MIRNA_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> MIRNA_M_FK_NAMES = of("analyzed_sample_id");
  // Special case: not a true PK (not unique), but referenced by MIRNA_S...
  private static final List<String> MIRNA_P_PSEUDO_PK_NAMES = of("mirna_seq");
  private static final List<String> MIRNA_P_FK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> MIRNA_S_PK_NAMES = of("mirna_seq", "chromosome", "chromosome_start",
      "chromosome_end", "chromosome_strand");
  private static final List<String> MIRNA_S_FK_NAMES = of("mirna_seq");

  private static final List<String> MIRNA_SEQ_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> MIRNA_SEQ_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> MIRNA_SEQ_P_FK_NAMES = of("analysis_id", "analyzed_sample_id");

  private static final List<String> PEXP_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> PEXP_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> PEXP_P_FK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> SAMPLE_FK_NAMES = of("specimen_id");
  private static final List<String> SAMPLE_PK_NAMES = of("analyzed_sample_id");
  private static final List<String> SGV_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> SGV_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> SGV_P_FK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> SPECIMEN_FK_NAMES = of("donor_id");
  private static final List<String> SPECIMEN_PK_NAMES = of("specimen_id");
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

  // OLD ARRAY
  private static final List<String> METH_M_PK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> METH_M_FK_NAMES = of("analyzed_sample_id");
  private static final List<String> METH_M_OPTIONAL_FK_NAMES = of("matched_sample_id");
  private static final List<String> METH_P_FK_NAMES = of("analysis_id", "analyzed_sample_id");
  private static final List<String> METH_P_PK_NAMES = of("analysis_id", "analyzed_sample_id", "methylated_fragment_id");
  private static final List<String> METH_S_FK_NAMES = of("analysis_id", "analyzed_sample_id", "methylated_fragment_id");

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

          .put(EXP_M, SAMPLE)
          .put(EXP_G, EXP_M)

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

          .build();

  /**
   * Redundant with {@link #ERROR_TYPE_DESCRIPTIONS}?
   */
  private static final Map<KVFileType, List<String>> PKS =
      new ImmutableMap.Builder<KVFileType, List<String>>()
          .put(DONOR, DONOR_PK_NAMES)
          .put(SPECIMEN, SPECIMEN_PK_NAMES)
          .put(SAMPLE, SAMPLE_PK_NAMES)

          .put(SSM_M, SSM_M_PK_NAMES)
          .put(CNSM_M, CNSM_M_PK_NAMES)
          .put(STSM_M, STSM_M_PK_NAMES)
          .put(JCN_M, JCN_M_PK_NAMES)
          .put(SGV_M, SGV_M_PK_NAMES)
          .put(PEXP_M, PEXP_M_PK_NAMES)
          .put(METH_M, METH_M_PK_NAMES)
          .put(METH_ARRAY_M, METH_ARRAY_M_PK_NAMES)
          .put(METH_SEQ_M, METH_SEQ_M_PK_NAMES)
          .put(EXP_M, EXP_M_PK_NAMES)
          .put(EXP_ARRAY_M, EXP_ARRAY_M_PK_NAMES)
          .put(EXP_SEQ_M, EXP_SEQ_M_PK_NAMES)
          .put(MIRNA_M, MIRNA_M_PK_NAMES)
          .put(MIRNA_SEQ_M, MIRNA_SEQ_M_PK_NAMES)

          .put(CNSM_P, CNSM_P_PK_NAMES)
          .put(STSM_P, STSM_P_PK_NAMES)
          .put(MIRNA_P, MIRNA_P_PSEUDO_PK_NAMES)
          .put(METH_P, METH_P_PK_NAMES)
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
          .put(METH_P, METH_P_FK_NAMES)
          .put(METH_ARRAY_P, METH_ARRAY_P_FK1_NAMES)
          .put(METH_SEQ_P, METH_SEQ_P_FK_NAMES)
          .put(EXP_G, EXP_G_FK_NAMES)
          .put(EXP_ARRAY_P, EXP_ARRAY_P_FK_NAMES)
          .put(EXP_SEQ_P, EXP_SEQ_P_FK_NAMES)
          .put(MIRNA_P, MIRNA_P_FK_NAMES)
          .put(MIRNA_SEQ_P, MIRNA_SEQ_P_FK_NAMES)

          .put(CNSM_S, CNSM_S_FK_NAMES)
          .put(STSM_S, STSM_S_FK_NAMES)
          .put(MIRNA_S, MIRNA_S_FK_NAMES)
          .put(METH_S, METH_S_FK_NAMES)

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
          || fileType == METH_P
          || fileType == METH_ARRAY_P
          || fileType == METH_SEQ_P
          || fileType == EXP_G
          || fileType == EXP_ARRAY_P
          || fileType == EXP_SEQ_P
          || fileType == MIRNA_P
          || fileType == MIRNA_SEQ_P
          || fileType == PEXP_P
          || fileType == SGV_P
      ;
    }
  };

  public static boolean hasOutgoingSurjectiveRelation(KVFileType fileType) {
    val b = SURJECTION_RELATION.apply(fileType);
    checkState(!b || getOptionalReferencedFileType1(fileType).isPresent(),
        "Expecting a referenced type at this point for '{}'", fileType);
    return b;
  }

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

          // MIRNA
          .put(
              MIRNA_M,
              new KVFileTypeErrorFields.Builder(MIRNA_M)
                  .pkFieldNames(MIRNA_M_PK_NAMES)
                  .fk1FieldNames(MIRNA_M_FK_NAMES)
                  .build())
          .put(
              MIRNA_P,
              new KVFileTypeErrorFields.Builder(MIRNA_P)
                  .fk1FieldNames(MIRNA_P_FK_NAMES)
                  .build())
          .put(
              MIRNA_S,
              new KVFileTypeErrorFields.Builder(MIRNA_S)
                  .pkFieldNames(MIRNA_S_PK_NAMES)
                  .fk1FieldNames(MIRNA_S_FK_NAMES)
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

          // OLD METH
          .put(
              METH_M,
              new KVFileTypeErrorFields.Builder(METH_M)
                  .pkFieldNames(METH_M_PK_NAMES)
                  .fk1FieldNames(METH_M_FK_NAMES)
                  .optionalFkFieldNames(METH_M_OPTIONAL_FK_NAMES)
                  .build())
          .put(
              METH_P,
              new KVFileTypeErrorFields.Builder(METH_P)
                  .pkFieldNames(METH_P_PK_NAMES)
                  .fk1FieldNames(METH_P_FK_NAMES)
                  .build())
          .put(
              METH_S,
              new KVFileTypeErrorFields.Builder(METH_S)
                  .fk1FieldNames(METH_S_FK_NAMES)
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

          // EXP
          .put(
              EXP_M,
              new KVFileTypeErrorFields.Builder(EXP_M)
                  .pkFieldNames(EXP_M_PK_NAMES)
                  .fk1FieldNames(EXP_M_FK_NAMES)
                  .build())
          .put(
              EXP_G,
              new KVFileTypeErrorFields.Builder(EXP_G)
                  .fk1FieldNames(EXP_G_FK_NAMES)
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
   * TODO: encode in dictionary data structure rather
   */
  public static KVRow getRow(KVFileType fileType, List<String> row) {
    KVKey pk = KEY_NOT_APPLICABLE;
    KVKey fk1 = KEY_NOT_APPLICABLE;
    KVKey fk2 = KEY_NOT_APPLICABLE;
    KVKey optionalFk = KEY_NOT_APPLICABLE;

    // CLINICAL
    if (fileType == DONOR) {
      pk = from(row, DONOR_PKS);
    } else if (fileType == SPECIMEN) {
      pk = from(row, SPECIMEN_PKS);
      fk1 = from(row, SPECIMEN_FKS);
    } else if (fileType == SAMPLE) {
      pk = from(row, SAMPLE_PKS);
      fk1 = from(row, SAMPLE_FKS);
    }

    // SSM
    else if (fileType == SSM_M) {
      pk = from(row, SSM_M_PKS);
      fk1 = from(row, SSM_M_FKS1);
      optionalFk = from(row, SSM_M_OPTIONAL_FKS);
    } else if (fileType == SSM_P) {
      fk1 = from(row, SSM_P_FKS);
    }

    // CNSM
    else if (fileType == CNSM_M) {
      pk = from(row, CNSM_M_PKS);
      fk1 = from(row, CNSM_M_FKS1);
      optionalFk = from(row, CNSM_M_OPTIONAL_FKS);
    } else if (fileType == CNSM_P) {
      pk = from(row, CNSM_P_PKS);
      fk1 = from(row, CNSM_P_FKS);
    } else if (fileType == CNSM_S) {
      fk1 = from(row, CNSM_S_FKS);
    }

    // STSM
    else if (fileType == STSM_M) {
      pk = from(row, STSM_M_PKS);
      fk1 = from(row, STSM_M_FKS1);
      optionalFk = from(row, STSM_M_OPTIONAL_FKS);
    } else if (fileType == STSM_P) {
      pk = from(row, STSM_P_PKS);
      fk1 = from(row, STSM_P_FKS);
    } else if (fileType == STSM_S) {
      fk1 = from(row, STSM_S_FKS);
    }

    // MIRNA
    else if (fileType == MIRNA_M) {
      pk = from(row, MIRNA_M_PKS);
      fk1 = from(row, MIRNA_M_FKS);
    } else if (fileType == MIRNA_P) {
      pk = from(row, MIRNA_P_PSEUDO_PKS); // Special case: uniqueness is not actually enforced
      fk1 = from(row, MIRNA_P_FKS);
    } else if (fileType == MIRNA_S) {
      pk = from(row, MIRNA_S_PKS); // Special case: we don't usually have a PK on secondary
      fk1 = from(row, MIRNA_S_FKS);
    }

    // MIRNA SEQ
    else if (fileType == MIRNA_SEQ_M) {
      pk = from(row, MIRNA_SEQ_M_PKS);
      fk1 = from(row, MIRNA_SEQ_M_FKS);
    } else if (fileType == MIRNA_SEQ_P) {
      fk1 = from(row, MIRNA_SEQ_P_FKS);
    }

    // METH
    else if (fileType == METH_M) {
      pk = from(row, METH_M_PKS);
      fk1 = from(row, METH_M_FKS1);
      optionalFk = from(row, METH_M_OPTIONAL_FKS);
    } else if (fileType == METH_P) {
      pk = from(row, METH_P_PKS);
      fk1 = from(row, METH_P_FKS);
    } else if (fileType == METH_S) {
      fk1 = from(row, METH_S_FKS);
    }

    // METH ARRAY
    else if (fileType == METH_ARRAY_M) {
      pk = from(row, METH_ARRAY_M_PKS);
      fk1 = from(row, METH_ARRAY_M_FKS);
    } else if (fileType == METH_ARRAY_PROBES) {
      pk = from(row, METH_ARRAY_SYSTEM_PKS);
    } else if (fileType == METH_ARRAY_P) {
      // In theory there is a PK but it's not enforced

      fk1 = from(row, METH_ARRAY_P_FKS1);
      fk2 = from(row, METH_ARRAY_P_FKS2);
    }

    // METH SEQ
    else if (fileType == METH_SEQ_M) {
      pk = from(row, METH_SEQ_M_PKS);
      fk1 = from(row, METH_SEQ_M_FKS);
    } else if (fileType == METH_SEQ_P) {
      fk1 = from(row, METH_SEQ_P_FKS);
    }

    // EXP
    else if (fileType == EXP_M) {
      pk = from(row, EXP_M_PKS);
      fk1 = from(row, EXP_M_FKS);
    } else if (fileType == EXP_G) {
      fk1 = from(row, EXP_G_FKS);
    }

    // EXP SEQ
    else if (fileType == EXP_SEQ_M) {
      pk = from(row, EXP_SEQ_M_PKS);
      fk1 = from(row, EXP_SEQ_M_FKS);
    } else if (fileType == EXP_SEQ_P) {
      pk = from(row, EXP_SEQ_P_PKS);
      fk1 = from(row, EXP_SEQ_P_FKS);
    }

    // EXP ARRAY
    else if (fileType == EXP_ARRAY_M) {
      pk = from(row, EXP_ARRAY_M_PKS);
      fk1 = from(row, EXP_ARRAY_M_FKS);
    } else if (fileType == EXP_ARRAY_P) {
      pk = from(row, EXP_ARRAY_P_PKS);
      fk1 = from(row, EXP_ARRAY_P_FKS);
    }

    // PEXP
    else if (fileType == PEXP_M) {
      pk = from(row, PEXP_M_PKS);
      fk1 = from(row, PEXP_M_FKS);
    } else if (fileType == PEXP_P) {
      fk1 = from(row, PEXP_P_FKS);
    }

    // JCN
    else if (fileType == JCN_M) {
      pk = from(row, JCN_M_PKS);
      fk1 = from(row, JCN_M_FKS);
    } else if (fileType == JCN_P) {
      fk1 = from(row, JCN_P_FKS);
    }

    // SGV
    else if (fileType == SGV_M) {
      pk = from(row, SGV_M_PKS);
      fk1 = from(row, SGV_M_FKS);
    } else if (fileType == SGV_P) {
      fk1 = from(row, SGV_P_FKS);
    }

    if (ROW_CHECKS_ENABLED) checkState(pk != null || fk1 != null, "Invalid row: '%s'", row);
    return new KVRow(pk, fk1, fk2, optionalFk);
  }

  public static Optional<KVFileType> getOptionalReferencedFileType1(KVFileType fileType) {
    return Optional.<KVFileType> fromNullable(RELATIONS1.get(fileType));
  }

  public static Optional<KVFileType> getOptionalReferencedFileType2(KVFileType fileType) {
    return Optional.<KVFileType> fromNullable(RELATIONS2.get(fileType));
  }

  public static List<String> getErrorFieldNames(KVFileType fileType, KVErrorType errorType) {
    return ERROR_TYPE_DESCRIPTIONS
        .get(fileType)
        .getErrorFieldNames(errorType);
  }

  public static List<String> getPrimaryKeyNames(KVFileType fileType) {
    return PKS.get(fileType);
  }

  public static List<String> getSurjectionForeignKeyNames(KVFileType fileType) {
    return SURJECTION_FKS.get(fileType);
  }

  /**
   * Do *not* apply with {@link KVFileType#SAMPLE}.
   */
  public static KVFileType getReferencingFileType(KVFileType fileType) {
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
