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
package org.icgc.dcc.submission.validation.key.enumeration;

import static com.google.common.collect.ImmutableList.of;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_S;
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
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SGV_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SGV_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.STSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.STSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.STSM_S;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * TODO: Should be relation driven instead.
 */
@RequiredArgsConstructor(access = PRIVATE)
public enum KVExperimentalDataType {

  SSM(SSM_M, of(SSM_M, SSM_P)),
  CNSM(CNSM_M, of(CNSM_M, CNSM_P, CNSM_S)),
  STSM(STSM_M, of(STSM_M, STSM_P, STSM_S)),
  MIRNA(MIRNA_M, of(MIRNA_M, MIRNA_P, MIRNA_S)),
  MIRNA_SEQ(MIRNA_SEQ_M, of(MIRNA_SEQ_M, MIRNA_SEQ_P)),
  METH(METH_M, of(METH_M, METH_P, METH_S)),
  METH_ARRAY(METH_ARRAY_M, of(METH_ARRAY_M, METH_ARRAY_PROBES, METH_ARRAY_P)),
  METH_SEQ(METH_SEQ_M, of(METH_SEQ_M, METH_SEQ_P)),
  EXP(EXP_M, of(EXP_M, EXP_G)),
  EXP_ARRAY(EXP_ARRAY_M, of(EXP_ARRAY_M, EXP_ARRAY_P)),
  EXP_SEQ(EXP_SEQ_M, of(EXP_SEQ_M, EXP_SEQ_P)),
  PEXP(PEXP_M, of(PEXP_M, PEXP_P)),
  JCN(JCN_M, of(JCN_M, JCN_P)),
  SGV(SGV_M, of(SGV_M, SGV_P));

  @Getter
  private final KVFileType dataTypePresenceIndicator;

  /**
   * Order matters (referenced files first).
   */
  @Getter
  private final List<KVFileType> fileTypes;

}
