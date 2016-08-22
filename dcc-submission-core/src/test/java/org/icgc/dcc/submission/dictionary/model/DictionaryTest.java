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
package org.icgc.dcc.submission.dictionary.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.model.FeatureTypes.FeatureType.CNGV_TYPE;
import static org.icgc.dcc.common.core.model.FeatureTypes.FeatureType.METH_ARRAY_TYPE;
import static org.icgc.dcc.common.core.model.FeatureTypes.FeatureType.STGV_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.CNSM_M_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.METH_ARRAY_M_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.METH_ARRAY_PROBES_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.METH_ARRAY_P_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SAMPLE_TYPE;
import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.common.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.submission.dictionary.util.Dictionaries;
import org.icgc.dcc.submission.test.AbstractDictionaryTest;
import org.junit.Test;

import com.google.common.collect.Lists;

public class DictionaryTest extends AbstractDictionaryTest {

  Dictionary dictionary;

  @Override
  @SneakyThrows
  public void setUp() {
    super.setUp();
    dictionary = Dictionaries.readDictionary(dictionaryFile.toURI().toURL());
  }

  @Test
  public void testGetFeatureTypes() throws Exception {
    val featureTypes = dictionary.getFeatureTypes();
    val expectedFeatures = Lists.<FeatureType> newArrayList(FeatureType.values());
    expectedFeatures.remove(STGV_TYPE);
    expectedFeatures.remove(CNGV_TYPE);
    assertThat(featureTypes).containsAll(expectedFeatures);
  }

  @Test
  public void testGetFileTypesReferencedBranch() throws Exception {
    val methTypes = dictionary.getFileTypesReferencedBranch(METH_ARRAY_TYPE);
    assertThat(methTypes).containsExactly(METH_ARRAY_M_TYPE, METH_ARRAY_PROBES_TYPE, METH_ARRAY_P_TYPE);
  }

  @Test
  public void testGetParents() throws Exception {
    assertThat(dictionary.getParents(METH_ARRAY_P_TYPE)).containsOnly(METH_ARRAY_M_TYPE, METH_ARRAY_PROBES_TYPE);
    assertThat(dictionary.getParents(CNSM_M_TYPE)).containsExactly(SAMPLE_TYPE);
  }

}
