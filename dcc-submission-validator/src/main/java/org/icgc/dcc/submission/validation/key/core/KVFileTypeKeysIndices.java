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
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.CONDITIONAL_FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.PK;
import static org.icgc.dcc.submission.validation.key.core.KVSubmissionProcessor.ROW_CHECKS_ENABLED;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.Value;
import lombok.val;

import org.icgc.dcc.common.core.util.stream.Streams;
import org.icgc.dcc.submission.validation.key.data.KVKey;
import org.icgc.dcc.submission.validation.key.data.KVRow;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

/**
 * Represents the indices for the keys that are relevant to a particular {@link KVFileType}.
 */
@Value
public class KVFileTypeKeysIndices {

  /**
   * Key types values for which should be resolved independent on any conditions.
   */
  private final static Collection<KVKeyType> UNCONDITIONAL_KEYS = defineUnconditionalKeys();

  private final Map<KVKeyType, Multimap<KVFileType, Integer>> keys;
  private final Map<KVFileType, RowConditionEvaluator> conditionEvaluators;

  public KVRow getRow(List<String> row) {
    if (ROW_CHECKS_ENABLED) {
      checkState(hasKeys(keys.get(PK)) || hasKeys(keys.get(FK)), "Invalid row: '%s'", row);
    }

    val rowTable = HashBasedTable.<KVKeyType, KVFileType, KVKey> create();
    for (val keyType : UNCONDITIONAL_KEYS) {
      val key = keys.get(keyType);
      if (hasKeys(key)) {
        addKeys(rowTable, keyType, getRowKeys(row, key));
      }
    }

    if (hasConditionalKeys()) {
      for (val evaluatorEntry : conditionEvaluators.entrySet()) {
        val referencedType = evaluatorEntry.getKey();
        val evaluator = evaluatorEntry.getValue();
        if (evaluator.evaluate(row)) {
          addConditionalKey(rowTable, row, referencedType);
        }
      }
    }

    return new KVRow(rowTable);
  }

  private void addConditionalKey(Table<KVKeyType, KVFileType, KVKey> table, List<String> row,
      KVFileType referencedType) {
    // The Multimap is backed by ArrayListMultimap
    val typeKeys = (List<Integer>) keys.get(CONDITIONAL_FK).get(referencedType);
    val key = KVKey.from(row, typeKeys);
    table.put(CONDITIONAL_FK, referencedType, key);
  }

  private boolean hasConditionalKeys() {
    return !conditionEvaluators.isEmpty();
  }

  private static void addKeys(Table<KVKeyType, KVFileType, KVKey> table, KVKeyType keyType,
      Map<KVFileType, KVKey> values) {
    for (val entry : values.entrySet()) {
      table.put(keyType, entry.getKey(), entry.getValue());
    }
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

  private static boolean hasKeys(Multimap<KVFileType, Integer> keys) {
    return keys != null && !keys.isEmpty();
  }

  private static Collection<KVKeyType> defineUnconditionalKeys() {
    return Streams.stream(KVKeyType.values())
        .filter(keyType -> keyType != KVKeyType.CONDITIONAL_FK)
        .collect(toImmutableList());
  }

}
