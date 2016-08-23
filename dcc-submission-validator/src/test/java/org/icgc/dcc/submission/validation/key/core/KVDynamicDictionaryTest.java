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

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.OPTIONAL_RELATION;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.RELATION;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.SURJECTION;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.UNIQUENESS;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.METH_ARRAY_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.METH_ARRAY_P;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.METH_ARRAY_PROBES;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SSM_M;
import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.util.Dictionaries;
import org.icgc.dcc.submission.test.AbstractDictionaryTest;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class KVDynamicDictionaryTest extends AbstractDictionaryTest {

  private KVDynamicDictionary kvDictionary;
  Dictionary dictionary;

  @Before
  @Override
  @SneakyThrows
  public void setUp() {
    super.setUp();
    dictionary = Dictionaries.readDictionary(dictionaryFile.toURI().toURL());
    kvDictionary = new KVDynamicDictionary(dictionary);
  }

  @Test
  public void testGetExperimentalDataTypes() throws Exception {
    val dataTypes = kvDictionary.getExperimentalDataTypes();
    assertThat(dataTypes).containsOnly(KVExperimentalDataType.values());
  }

  @Test
  public void testGetExperimentalFileTypes() throws Exception {
    val methFileTypes = kvDictionary.getExperimentalFileTypes(KVExperimentalDataType.METH_ARRAY);
    assertThat(methFileTypes).containsExactly(METH_ARRAY_M, METH_ARRAY_PROBES, METH_ARRAY_P);
  }

  @Test
  public void testGetKeysIndices() throws Exception {
    val ssmKeys = kvDictionary.getKeysIndices(KVFileType.SSM_M);

    assertThat(ssmKeys.getPk()).containsExactly(0, 1);

    val fks = ssmKeys.getFks();
    assertThat(fks.size()).isEqualTo(1);
    assertThat(fks.get(KVFileType.SAMPLE)).containsExactly(1);

    val optionalFks = ssmKeys.getOptionalFks();
    assertThat(optionalFks.size()).isEqualTo(1);
    assertThat(optionalFks.get(KVFileType.SAMPLE)).containsExactly(2);

    // Surgery
    val surgeryKeys = kvDictionary.getKeysIndices(KVFileType.SURGERY);

    assertThat(surgeryKeys.getPk()).containsExactly(0, 5);

    val surgeryFks = surgeryKeys.getFks();
    assertThat(surgeryFks.size()).isEqualTo(1);
    assertThat(surgeryFks.get(KVFileType.DONOR)).containsExactly(0);

    val surgeryOptionalFks = surgeryKeys.getOptionalFks();
    assertThat(surgeryOptionalFks.size()).isEqualTo(1);
    assertThat(surgeryOptionalFks.get(KVFileType.SPECIMEN)).containsExactly(5);
  }

  @Test
  public void testGetTopologicallyOrderedFileTypes() throws Exception {
    ImmutableList.copyOf(kvDictionary.getTopologicallyOrderedFileTypes());
    // TODO: Test for order
  }

  @Test
  public void testGetParents() throws Exception {
    assertThat(kvDictionary.getParents(METH_ARRAY_P)).containsOnly(METH_ARRAY_M, METH_ARRAY_PROBES);
    assertThat(kvDictionary.getParents(CNSM_M)).containsExactly(SAMPLE);
  }

  @Test
  public void testHasChildren() throws Exception {
    assertThat(kvDictionary.hasChildren(KVFileType.DONOR)).isTrue();
    assertThat(kvDictionary.hasChildren(KVFileType.SURGERY)).isFalse();
    assertThat(kvDictionary.hasChildren(KVFileType.METH_ARRAY_PROBES)).isTrue();
    assertThat(kvDictionary.hasChildren(KVFileType.SSM_M)).isTrue();
    assertThat(kvDictionary.hasChildren(KVFileType.SSM_P)).isFalse();
  }

  @Test
  public void testGetErrorFieldNames() throws Exception {
    assertThat(kvDictionary.getErrorFieldNames(SSM_M, OPTIONAL_RELATION, Optional.of(SAMPLE)))
        .containsExactly("matched_sample_id");
    assertThat(kvDictionary.getErrorFieldNames(SSM_M, RELATION, Optional.of(SAMPLE)))
        .containsExactly("analyzed_sample_id");
    assertThat(kvDictionary.getErrorFieldNames(SSM_M, UNIQUENESS, Optional.of(SAMPLE)))
        .containsExactly("analysis_id", "analyzed_sample_id");
    assertThat(kvDictionary.getErrorFieldNames(SAMPLE, SURJECTION, Optional.of(SPECIMEN)))
        .containsExactly("specimen_id");
  }

}
