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

import org.icgc.dcc.core.model.FileTypes.FileType;

@RequiredArgsConstructor
public enum KVFileType {
  DONOR(FileType.DONOR_TYPE),
  SPECIMEN(FileType.SPECIMEN_TYPE),
  SAMPLE(FileType.SAMPLE_TYPE),

  BIOMARKER(FileType.BIOMARKER_TYPE),
  EXPOSURE(FileType.EXPOSURE_TYPE),
  FAMILY(FileType.FAMILY_TYPE),
  SURGERY(FileType.SURGERY_TYPE),
  THERAPY(FileType.THERAPY_TYPE),

  SSM_M(FileType.SSM_M_TYPE),
  SSM_P(FileType.SSM_P_TYPE),

  CNSM_M(FileType.CNSM_M_TYPE),
  CNSM_P(FileType.CNSM_P_TYPE),
  CNSM_S(FileType.CNSM_S_TYPE),

  STSM_M(FileType.STSM_M_TYPE),
  STSM_P(FileType.STSM_P_TYPE),
  STSM_S(FileType.STSM_S_TYPE),

  MIRNA_M(FileType.MIRNA_M_TYPE),
  MIRNA_P(FileType.MIRNA_P_TYPE), // Does NOT have a PK (unusual)
  MIRNA_S(FileType.MIRNA_S_TYPE), // Does have a PK (unusual)

  METH_M(FileType.METH_M_TYPE),
  METH_P(FileType.METH_P_TYPE),
  METH_S(FileType.METH_S_TYPE),

  EXP_M(FileType.EXP_M_TYPE),
  EXP_G(FileType.EXP_G_TYPE), // Naming exception ('g' instead of 'p')

  PEXP_M(FileType.PEXP_M_TYPE),
  PEXP_P(FileType.PEXP_P_TYPE),

  JCN_M(FileType.JCN_M_TYPE),
  JCN_P(FileType.JCN_P_TYPE),

  SGV_M(FileType.SGV_M_TYPE),
  SGV_P(FileType.SGV_P_TYPE);

  @Getter
  private final FileType submissionFileType;

  public boolean isReplaceAll() {
    return this == DONOR || this == SPECIMEN || this == SAMPLE;
  }

}