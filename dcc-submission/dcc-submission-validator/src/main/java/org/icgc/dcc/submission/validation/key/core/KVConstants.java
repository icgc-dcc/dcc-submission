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

import static com.google.common.collect.Lists.newArrayList;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.BIOMARKER;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXPOSURE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_P;
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

import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

@NoArgsConstructor(access = PRIVATE)
public final class KVConstants {

  public static final Splitter TAB_SPLITTER = Splitter.on('\t');
  public static final ObjectMapper MAPPER = new ObjectMapper();

  // TODO: move elsewhere
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
          .put(EXP_P, EXP_M)

          .put(PEXP_M, SAMPLE)
          .put(PEXP_P, PEXP_M)

          .put(JCN_M, SAMPLE)
          .put(JCN_P, JCN_M)

          .put(SGV_M, SAMPLE)
          .put(SGV_P, SGV_M)

          .build();

  // TODO: translate to Strings rather? + make map per file type/submission type?
  public static final List<Integer> NOT_APPLICABLE = null;
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
}
