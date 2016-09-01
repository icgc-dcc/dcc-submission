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

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.OPTIONAL_RELATION;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.RELATION;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.SURJECTION;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.UNIQUENESS;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.BIOMARKER;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.METH_ARRAY_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.METH_ARRAY_P;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.METH_ARRAY_PROBES;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SURGERY;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.CONDITIONAL_FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.OPTIONAL_FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.PK;

import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.util.Dictionaries;
import org.icgc.dcc.submission.test.AbstractDictionaryTest;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

@Slf4j
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
  public void testGetKeysIndices() throws Exception {
    val indices = kvDictionary.getKeysIndices(SSM_M);
    log.info("{}", indices);
    val keys = indices.getKeys();
    assertThat(keys).hasSize(4);
    assertKey(keys, PK, ImmutableMap.of(SSM_M, ImmutableList.of(0, 1)));
    assertKey(keys, FK, ImmutableMap.of(SAMPLE, ImmutableList.of(1)));
    assertKey(keys, OPTIONAL_FK, ImmutableMap.of(SAMPLE, ImmutableList.of(2)));
    assertKey(keys, CONDITIONAL_FK, ImmutableMap.of(SAMPLE, ImmutableList.of(2)));

    val conditions = indices.getConditionEvaluators();
    assertThat(conditions).hasSize(1);
    assertThat(conditions.get(SAMPLE)).isNotNull();
  }

  @Test
  public void testGetKeysIndices_surgery() {
    val indices = kvDictionary.getKeysIndices(SURGERY);
    log.info("{}", indices);
    val keys = indices.getKeys();
    assertThat(keys).hasSize(4);
    assertKey(keys, PK, ImmutableMap.of(SURGERY, ImmutableList.of(0, 5)));
    assertKey(keys, FK, ImmutableMap.of(DONOR, ImmutableList.of(0)));
    assertKey(keys, OPTIONAL_FK, ImmutableMap.of(SPECIMEN, ImmutableList.of(5)));
    assertKey(keys, CONDITIONAL_FK, emptyMap());
  }

  @Test
  public void testGetKeysIndices_biomarker() {
    val indices = kvDictionary.getKeysIndices(BIOMARKER);
    log.info("{}", indices);
    val keys = indices.getKeys();
    assertThat(keys).hasSize(4);
    assertKey(keys, PK, ImmutableMap.of(BIOMARKER, ImmutableList.of(2, 0, 1)));
    assertKey(keys, FK, ImmutableMap.of(DONOR, ImmutableList.of(0), SPECIMEN, ImmutableList.of(1)));
    assertKey(keys, OPTIONAL_FK, emptyMap());
    assertKey(keys, CONDITIONAL_FK, emptyMap());
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

  private static void assertKey(Map<KVKeyType, Multimap<KVFileType, Integer>> keys, KVKeyType keyType,
      Map<KVFileType, List<Integer>> expectedValues) {
    val actualKeys = keys.get(keyType);
    assertThat(actualKeys.keySet()).hasSize(expectedValues.size());
    expectedValues.entrySet().forEach(entry -> {
      KVFileType fileType = entry.getKey();
      List<Integer> expectedValue = entry.getValue();
      assertThat(actualKeys.get(fileType)).isEqualTo(expectedValue);
    });
  }

}
