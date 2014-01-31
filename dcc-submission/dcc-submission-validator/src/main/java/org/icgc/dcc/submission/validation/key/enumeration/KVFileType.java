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
package org.icgc.dcc.submission.validation.key.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;

@RequiredArgsConstructor
public enum KVFileType {
  DONOR(SubmissionFileType.DONOR_TYPE),
  SPECIMEN(SubmissionFileType.SPECIMEN_TYPE),
  SAMPLE(SubmissionFileType.SAMPLE_TYPE),

  BIOMARKER(SubmissionFileType.BIOMARKER_TYPE),
  EXPOSURE(SubmissionFileType.EXPOSURE_TYPE),
  FAMILY(SubmissionFileType.FAMILY_TYPE),
  SURGERY(SubmissionFileType.SURGERY_TYPE),
  THERAPY(SubmissionFileType.THERAPY_TYPE),

  SSM_M(SubmissionFileType.SSM_M_TYPE),
  SSM_P(SubmissionFileType.SSM_P_TYPE),

  CNSM_M(SubmissionFileType.CNSM_M_TYPE),
  CNSM_P(SubmissionFileType.CNSM_P_TYPE),
  CNSM_S(SubmissionFileType.CNSM_S_TYPE),

  STSM_M(SubmissionFileType.STSM_M_TYPE),
  STSM_P(SubmissionFileType.STSM_P_TYPE),
  STSM_S(SubmissionFileType.STSM_S_TYPE),

  MIRNA_M(SubmissionFileType.MIRNA_M_TYPE),
  MIRNA_P(SubmissionFileType.MIRNA_P_TYPE), // Does NOT have a PK (unusual)
  MIRNA_S(SubmissionFileType.MIRNA_S_TYPE), // Does have a PK (unusual)

  METH_M(SubmissionFileType.METH_M_TYPE),
  METH_P(SubmissionFileType.METH_P_TYPE),
  METH_S(SubmissionFileType.METH_S_TYPE),

  EXP_M(SubmissionFileType.EXP_M_TYPE),
  EXP_G(SubmissionFileType.EXP_G_TYPE), // Naming exception ('g' instead of 'p')

  PEXP_M(SubmissionFileType.PEXP_M_TYPE),
  PEXP_P(SubmissionFileType.PEXP_P_TYPE),

  JCN_M(SubmissionFileType.JCN_M_TYPE),
  JCN_P(SubmissionFileType.JCN_P_TYPE),

  SGV_M(SubmissionFileType.SGV_M_TYPE),
  SGV_P(SubmissionFileType.SGV_P_TYPE);

  @Getter
  private final SubmissionFileType submissionFileType;

  public boolean isReplaceAll() {
    return this == DONOR || this == SPECIMEN || this == SAMPLE;
  }

  /**
   * TODO: get from dictionary
   */
  public boolean hasPk() {
    return this != SSM_P
        && this != CNSM_S
        && this != STSM_S
        && this != MIRNA_P // MIRNA_S is the one that does (atypical)
        && this != METH_S
        && this != EXP_G
        && this != PEXP_P
        && this != JCN_P
        && this != SGV_P;
  }
}