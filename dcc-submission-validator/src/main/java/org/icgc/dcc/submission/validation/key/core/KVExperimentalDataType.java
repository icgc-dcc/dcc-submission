/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static com.google.common.collect.Iterables.find;
import static java.util.Arrays.asList;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.common.core.model.FeatureTypes.FeatureType.CNSM_TYPE;
import static org.icgc.dcc.common.core.model.FeatureTypes.FeatureType.EXP_ARRAY_TYPE;
import static org.icgc.dcc.common.core.model.FeatureTypes.FeatureType.EXP_SEQ_TYPE;
import static org.icgc.dcc.common.core.model.FeatureTypes.FeatureType.JCN_TYPE;
import static org.icgc.dcc.common.core.model.FeatureTypes.FeatureType.METH_ARRAY_TYPE;
import static org.icgc.dcc.common.core.model.FeatureTypes.FeatureType.METH_SEQ_TYPE;
import static org.icgc.dcc.common.core.model.FeatureTypes.FeatureType.MIRNA_SEQ_TYPE;
import static org.icgc.dcc.common.core.model.FeatureTypes.FeatureType.PEXP_TYPE;
import static org.icgc.dcc.common.core.model.FeatureTypes.FeatureType.SGV_TYPE;
import static org.icgc.dcc.common.core.model.FeatureTypes.FeatureType.SSM_TYPE;
import static org.icgc.dcc.common.core.model.FeatureTypes.FeatureType.STSM_TYPE;

import org.icgc.dcc.common.core.model.FeatureTypes.FeatureType;

import com.google.common.base.Predicate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = PRIVATE)
public enum KVExperimentalDataType {

  SSM(SSM_TYPE),
  CNSM(CNSM_TYPE),
  STSM(STSM_TYPE),
  MIRNA_SEQ(MIRNA_SEQ_TYPE),
  METH_ARRAY(METH_ARRAY_TYPE),
  METH_SEQ(METH_SEQ_TYPE),
  EXP_ARRAY(EXP_ARRAY_TYPE),
  EXP_SEQ(EXP_SEQ_TYPE),
  PEXP(PEXP_TYPE),
  JCN(JCN_TYPE),
  SGV(SGV_TYPE);

  @Getter
  private final FeatureType featureType;

  public KVFileType getDataTypePresenceIndicator() {
    return KVFileType.from(featureType.getDataTypePresenceIndicator());
  }

  public static KVExperimentalDataType from(final FeatureType featureType) {
    return find(asList(values()), new Predicate<KVExperimentalDataType>() {

      @Override
      public boolean apply(KVExperimentalDataType dataType) {
        return dataType.featureType == featureType;
      }

    });
  }

}