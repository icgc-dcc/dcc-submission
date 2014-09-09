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
package org.icgc.dcc.core.model;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.icgc.dcc.core.model.ClinicalType.CLINICAL_CORE_TYPE;
import static org.icgc.dcc.core.model.DataType.DataTypes.from;
import static org.icgc.dcc.core.model.DataType.DataTypes.isAggregatedType;
import static org.icgc.dcc.core.model.DataType.DataTypes.isMandatoryType;
import static org.icgc.dcc.core.model.DataType.DataTypes.values;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.SSM_TYPE;

import java.util.HashSet;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.junit.Test;

public class DataTypeTest {

  @Test
  public void test_DataTypes_valid() {
    assertThat(from("ssm")).isEqualTo(SSM_TYPE);
    assertThat(from("donor")).isEqualTo(CLINICAL_CORE_TYPE);

    assertThat(values().size()).isEqualTo(15); // 13 feature types + 1 clinical type + 1 optional clinical type
    assertThat(values().size()).isEqualTo( // Check no duplicates
        new HashSet<DataType>(values()).size());

    assertThat(isMandatoryType(ClinicalType.CLINICAL_CORE_TYPE)).isTrue();
    assertThat(isMandatoryType(FeatureType.SSM_TYPE)).isFalse();

    assertThat(isAggregatedType(FeatureType.SSM_TYPE)).isTrue();
    assertThat(isAggregatedType(FeatureType.METH_ARRAY_TYPE)).isFalse();
    assertThat(isAggregatedType(ClinicalType.CLINICAL_CORE_TYPE)).isFalse();
  }

  @Test(expected = IllegalArgumentException.class)
  public void test_DataTypes_invalid() {
    from("dummy");
  }

}
