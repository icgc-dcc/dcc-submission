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
package org.icgc.dcc.submission.validation.key.error;

import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_M_FKS1;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_M_FKS2;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_P_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_S_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.DONOR_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.EXP_G_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.EXP_M_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.EXP_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.JCN_M_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.JCN_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.JCN_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_M_FKS1;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_M_FKS2;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_P_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_S_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MIRNA_M_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MIRNA_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MIRNA_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MIRNA_S_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MIRNA_S_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.NOT_APPLICABLE;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.PEXP_M_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.PEXP_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.PEXP_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SAMPLE_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SAMPLE_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SGV_M_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SGV_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SGV_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SPECIMEN_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SPECIMEN_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_M_FKS1;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_M_FKS2;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_M_FKS1;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_M_FKS2;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_P_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_S_FKS;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_P;
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

import java.util.Map;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.core.KVFileDescription;
import org.icgc.dcc.submission.validation.key.data.KVSubmissionDataDigest;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;

import com.google.common.collect.ImmutableMap;

/**
 * 
 */
@Slf4j
public class KVSubmissionErrors {

  private final Map<KVFileType, KVFileErrors> errors = new ImmutableMap.Builder<KVFileType, KVFileErrors>()
      .put(
          DONOR,
          new KVFileErrors(DONOR_PKS, NOT_APPLICABLE, NOT_APPLICABLE))
      .put(
          SPECIMEN,
          new KVFileErrors(SPECIMEN_PKS, SPECIMEN_FKS, NOT_APPLICABLE))
      .put(
          SAMPLE,
          new KVFileErrors(SAMPLE_PKS, SAMPLE_FKS, NOT_APPLICABLE))

      // SSM
      .put(
          SSM_M,
          new KVFileErrors(SSM_M_PKS, SSM_M_FKS1, SSM_M_FKS2))
      .put(
          SSM_P,
          new KVFileErrors(NOT_APPLICABLE, SSM_P_FKS, NOT_APPLICABLE))

      // CNSM
      .put(
          CNSM_M,
          new KVFileErrors(CNSM_M_PKS, CNSM_M_FKS1, CNSM_M_FKS2))
      .put(
          CNSM_P,
          new KVFileErrors(
              NOT_APPLICABLE,
              CNSM_P_PKS,
              CNSM_P_FKS))
      .put(
          CNSM_S,
          new KVFileErrors(
              NOT_APPLICABLE,
              CNSM_S_FKS,
              NOT_APPLICABLE))

      // STSM
      .put(
          STSM_M,
          new KVFileErrors(
              STSM_M_PKS,
              STSM_M_FKS1,
              STSM_M_FKS2))
      .put(
          STSM_P,
          new KVFileErrors(
              STSM_P_PKS,
              STSM_P_FKS,
              NOT_APPLICABLE))
      .put(
          STSM_S,
          new KVFileErrors(
              NOT_APPLICABLE,
              STSM_S_FKS,
              NOT_APPLICABLE))

      // MIRNA
      .put(
          MIRNA_M,
          new KVFileErrors(
              MIRNA_M_PKS,
              MIRNA_M_FKS,
              NOT_APPLICABLE))
      .put(
          MIRNA_P,
          new KVFileErrors(
              NOT_APPLICABLE,
              MIRNA_P_FKS,
              NOT_APPLICABLE))
      .put(
          MIRNA_S,
          new KVFileErrors(
              MIRNA_S_PKS,
              MIRNA_S_FKS,
              NOT_APPLICABLE))

      // METH
      .put(
          METH_M,
          new KVFileErrors(
              METH_M_PKS,
              METH_M_FKS1,
              METH_M_FKS2))
      .put(
          METH_P,
          new KVFileErrors(
              METH_P_PKS,
              METH_P_FKS,
              NOT_APPLICABLE))
      .put(
          METH_S,
          new KVFileErrors(
              NOT_APPLICABLE,
              METH_S_FKS,
              NOT_APPLICABLE))

      // EXP
      .put(
          EXP_M,
          new KVFileErrors(
              EXP_M_PKS,
              EXP_M_FKS,
              NOT_APPLICABLE))
      .put(
          EXP_P,
          new KVFileErrors(
              NOT_APPLICABLE,
              EXP_G_FKS,
              NOT_APPLICABLE))

      // PEXP
      .put(
          PEXP_M,
          new KVFileErrors(
              PEXP_M_PKS,
              PEXP_M_FKS,
              NOT_APPLICABLE))
      .put(
          PEXP_P,
          new KVFileErrors(
              NOT_APPLICABLE,
              PEXP_P_FKS,
              NOT_APPLICABLE))

      // JCN
      .put(
          JCN_M,
          new KVFileErrors(
              JCN_M_PKS,
              JCN_M_FKS,
              NOT_APPLICABLE))
      .put(
          JCN_P,
          new KVFileErrors(
              NOT_APPLICABLE,
              JCN_P_FKS,
              NOT_APPLICABLE))

      // SGV
      .put(
          SGV_M,
          new KVFileErrors(
              SGV_M_PKS,
              SGV_M_FKS,
              NOT_APPLICABLE))
      .put(
          SGV_P,
          new KVFileErrors(
              NOT_APPLICABLE,
              SGV_P_FKS,
              NOT_APPLICABLE))

      .build();

  public KVFileErrors getFileErrors(KVFileType fileType) {
    return errors.get(fileType);
  }

  /**
   * TODO: PLK: we actually only need the KVFileDescription here, not the whole {@link KVSubmissionDataDigest}.
   */
  public boolean describe(Map<KVFileType, KVFileDescription> fileDescriptions) {
    boolean status = true;
    for (val entry : errors.entrySet()) {
      val fileType = entry.getKey();
      val fileErrors = entry.getValue();

      boolean fileStatus = fileErrors.describe(fileDescriptions.get(fileType));
      status &= fileStatus;
      log.info("{}: {}", fileType, fileStatus);
    }
    return status;
  }
}
