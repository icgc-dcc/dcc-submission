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
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.CONDITIONAL_FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.OPTIONAL_FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.PK;

import java.util.List;
import java.util.Map;

import lombok.val;

import org.junit.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

public class KVFileTypeKeysIndicesTest {

  KVFileTypeKeysIndices keysIndices;

  @Test
  public void testGetRow() throws Exception {
    keysIndices = new KVFileTypeKeysIndices(createSsmMIndexKeys(),
        ImmutableMap.of(SAMPLE, createSsmMRowEvaluator()));
    val row = keysIndices.getRow(ImmutableList.of("TARGET-1", "TARGET-1-1", "TARGET-1-2", "1", "p1", "proto2", "alg3",
        "alg4", "alg5", "alg6", "str7", "cov8", "repo9", "acc10"));
    assertThat(row.hasPk()).isTrue();
    assertThat(row.hasFk(SAMPLE)).isTrue();
    assertThat(row.hasFk(SPECIMEN)).isFalse();
    assertThat(row.hasOptionalFks()).isTrue();
    assertThat(row.hasConditionalFks()).isFalse();

    assertThat(row.getPk().getStringValues()).containsExactly("TARGET-1", "TARGET-1-1");

    val fks = row.getFks();
    assertThat(fks).hasSize(1);
    assertThat(fks.get(SAMPLE).getStringValues()).containsExactly("TARGET-1-1");
    assertThat(row.getFk(SAMPLE).getStringValues()).containsExactly("TARGET-1-1");
    assertThat(row.getFk(SPECIMEN)).isNull();

    val optionalFks = row.getOptionalFks();
    assertThat(optionalFks).hasSize(1);
    assertThat(optionalFks.get(SAMPLE).getStringValues()).containsExactly("TARGET-1-2");
  }

  @Test
  public void testGetRow_conditional() throws Exception {
    keysIndices = new KVFileTypeKeysIndices(createSsmMIndexKeys(),
        ImmutableMap.of(SAMPLE, createSsmMRowEvaluator()));
    val row = keysIndices.getRow(ImmutableList.of("TARGET-1", "TARGET-1-1", "TARGET-1-2", "1", "p1", "proto2", "alg3",
        "alg4", "alg5", "alg6", "str7", "cov8", "AWS", "acc10"));
    assertThat(row.hasConditionalFks()).isTrue();

    val keys = row.getConditionalKeys();
    assertThat(keys).hasSize(1);
    assertThat(keys.get(SAMPLE).getStringValues()).containsExactly("TARGET-1-2");
  }

  private static Map<KVKeyType, Multimap<KVFileType, Integer>> createSsmMIndexKeys() {
    return ImmutableMap.of(
        PK, createMultimap(SSM_M, ImmutableList.of(0, 1)),
        FK, createMultimap(SAMPLE, ImmutableList.of(1)),
        OPTIONAL_FK, createMultimap(SAMPLE, ImmutableList.of(2)),
        CONDITIONAL_FK, createMultimap(SAMPLE, ImmutableList.of(2)));
  }

  private static RowConditionEvaluator createSsmMRowEvaluator() {
    return new RowConditionEvaluator("['AWS','Collab'] contains raw_data_repository", ImmutableList.of("analysis_id",
        "analyzed_sample_id", "matched_sample_id", "assembly_version", "platform", "experimental_protocol",
        "base_calling_algorithm", "alignment_algorithm", "variation_calling_algorithm", "other_analysis_algorithm",
        "sequencing_strategy", "seq_coverage", "raw_data_repository", "raw_data_accession"));
  }

  private static Multimap<KVFileType, Integer> createMultimap(KVFileType key, List<Integer> indices) {
    val multimap = ArrayListMultimap.<KVFileType, Integer> create();
    multimap.putAll(key, indices);

    return multimap;
  }

}
