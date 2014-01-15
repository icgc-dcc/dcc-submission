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

import com.google.common.base.Predicate;

public enum KVFileType {
  DONOR,
  SPECIMEN,
  SAMPLE,

  BIOMARKER,
  EXPOSURE,
  FAMILY,
  SURGERY,
  THERAPY,

  SSM_M,
  SSM_P,

  CNSM_M,
  CNSM_P,
  CNSM_S,

  STSM_M,
  STSM_P,
  STSM_S,

  MIRNA_M,
  MIRNA_P,
  MIRNA_S,

  METH_M,
  METH_P,
  METH_S,

  EXP_M,
  EXP_G, // Naming exception..

  PEXP_M,
  PEXP_P,

  JCN_M,
  JCN_P,

  SGV_M,
  SGV_P;

  public boolean isReplaceAll() {
    return this == DONOR || this == SPECIMEN || this == SAMPLE;
  }

  // TODO: get from dictionary
  public static final Predicate<KVFileType> SIMPLE_SURJECTION_RELATION = new Predicate<KVFileType>() {

    @Override
    public boolean apply(KVFileType fileType) {
      return fileType == SPECIMEN
          || fileType == SAMPLE

          || fileType == SSM_P
          || fileType == CNSM_P
          || fileType == STSM_P
          || fileType == MIRNA_P
          || fileType == METH_P
          || fileType == EXP_G
          || fileType == PEXP_P
          || fileType == JCN_P;
    }
  };

  public boolean hasOutgoingComplexSurjectiveRelation() {
    return this.name().toLowerCase().endsWith("_m"); // TODO: improve
  }

  /**
   * Simple as opposd to TODO
   */
  public boolean hasOutgoingSimpleSurjectiveRelation() {
    return SIMPLE_SURJECTION_RELATION.apply(this);
  }

  public boolean hasOutgoingSurjectiveRelation() {
    return hasOutgoingSimpleSurjectiveRelation()
        || hasOutgoingComplexSurjectiveRelation();
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