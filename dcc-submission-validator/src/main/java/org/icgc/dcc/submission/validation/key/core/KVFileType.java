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
package org.icgc.dcc.submission.validation.key.core;

import static java.util.Arrays.asList;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import org.icgc.dcc.common.core.model.FileTypes.FileType;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

@RequiredArgsConstructor
public enum KVFileType {

  DONOR(FileType.DONOR_TYPE),
  SPECIMEN(FileType.SPECIMEN_TYPE),
  SAMPLE(FileType.SAMPLE_TYPE),

  SSM_M(FileType.SSM_M_TYPE),
  SSM_P(FileType.SSM_P_TYPE),

  CNSM_M(FileType.CNSM_M_TYPE),
  CNSM_P(FileType.CNSM_P_TYPE),
  CNSM_S(FileType.CNSM_S_TYPE),

  STSM_M(FileType.STSM_M_TYPE),
  STSM_P(FileType.STSM_P_TYPE),
  STSM_S(FileType.STSM_S_TYPE),

  MIRNA_SEQ_M(FileType.MIRNA_SEQ_M_TYPE),
  MIRNA_SEQ_P(FileType.MIRNA_SEQ_P_TYPE),

  METH_ARRAY_M(FileType.METH_ARRAY_M_TYPE),
  METH_ARRAY_P(FileType.METH_ARRAY_P_TYPE),
  METH_ARRAY_PROBES(FileType.METH_ARRAY_PROBES_TYPE),

  METH_SEQ_M(FileType.METH_SEQ_M_TYPE),
  METH_SEQ_P(FileType.METH_SEQ_P_TYPE),

  EXP_ARRAY_M(FileType.EXP_ARRAY_M_TYPE),
  EXP_ARRAY_P(FileType.EXP_ARRAY_P_TYPE),

  EXP_SEQ_M(FileType.EXP_SEQ_M_TYPE),
  EXP_SEQ_P(FileType.EXP_SEQ_P_TYPE),

  PEXP_M(FileType.PEXP_M_TYPE),
  PEXP_P(FileType.PEXP_P_TYPE),

  JCN_M(FileType.JCN_M_TYPE),
  JCN_P(FileType.JCN_P_TYPE),

  SGV_M(FileType.SGV_M_TYPE),
  SGV_P(FileType.SGV_P_TYPE);

  @Getter
  private final FileType fileType;

  public boolean isSystem() {
    return fileType.getSubType().isSystemSubType();
  }

  public boolean isReplaceAll() {
    return this == DONOR || this == SPECIMEN || this == SAMPLE;
  }

  public static KVFileType from(final FileType fileType) {
    return Iterables.find(asList(values()), new Predicate<KVFileType>() {

      @Override
      public boolean apply(KVFileType input) {
        return input.fileType == fileType;
      }

    });
  }

}