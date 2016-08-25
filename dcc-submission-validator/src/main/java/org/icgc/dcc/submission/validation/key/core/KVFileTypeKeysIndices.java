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

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptyMap;
import static org.icgc.dcc.submission.validation.key.core.KVSubmissionProcessor.ROW_CHECKS_ENABLED;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Value;
import lombok.val;

import org.icgc.dcc.submission.validation.key.data.KVKey;
import org.icgc.dcc.submission.validation.key.data.KVRow;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

/**
 * Represents the indices for the keys that are relevant to a particular {@link KVFileType}.
 */
@Value
@Builder
public class KVFileTypeKeysIndices {

  private final List<Integer> pk;
  private Multimap<KVFileType, Integer> fks;
  private Multimap<KVFileType, Integer> optionalFks;

  public KVRow getRow(List<String> row) {
    if (ROW_CHECKS_ENABLED) {
      checkState(pk != null || !fks.isEmpty(), "Invalid row: '%s'", row);
    }

    val builder = KVRow.builder();

    if (pk != null) {
      builder.pk(KVKey.from(row, pk));
    }

    if (!fks.isEmpty()) {
      builder.fks(getRowKeys(row, fks));
    } else {
      builder.fks(emptyMap());
    }

    if (optionalFks != null && !optionalFks.isEmpty()) {
      builder.optionalFks(getRowKeys(row, optionalFks));
    } else {
      builder.optionalFks(emptyMap());
    }

    return builder.build();
  }

  private static Map<KVFileType, KVKey> getRowKeys(List<String> row, Multimap<KVFileType, Integer> fks) {
    val rowFks = ImmutableMap.<KVFileType, KVKey> builder();
    for (val fileType : fks.keySet()) {
      // The Multimap is backed by ArrayListMultimap
      val fileTypeFks = (List<Integer>) fks.get(fileType);
      val key = KVKey.from(row, fileTypeFks);
      rowFks.put(fileType, key);
    }

    return rowFks.build();
  }

}
