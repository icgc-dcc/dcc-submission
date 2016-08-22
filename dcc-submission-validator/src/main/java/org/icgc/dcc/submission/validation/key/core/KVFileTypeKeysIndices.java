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
import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Collections.emptyMap;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableMap;
import static org.icgc.dcc.submission.validation.key.core.KVSubmissionProcessor.ROW_CHECKS_ENABLED;

import java.util.List;

import lombok.Builder;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.data.KVKey;
import org.icgc.dcc.submission.validation.key.data.KVRow;

import com.google.common.collect.Multimap;

/**
 * Represents the indices for the keys that are relevant to a particular {@link KVFileType}.
 */
@Value
@Builder
@Slf4j
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

    log.info("FKs: {}", fks);
    if (!fks.isEmpty()) {
      val rowFks = fks.keySet().stream()
          .collect(toImmutableMap(fileType -> fileType, fileType -> KVKey.from(row, copyOf(fks.get(fileType)))));
      log.info("Row fks: {}", rowFks);
      builder.fks(rowFks);
    } else {
      builder.fks(emptyMap());
    }

    if (optionalFks != null && !optionalFks.isEmpty()) {
      val rowOptionalFks = optionalFks.keySet().stream()
          .collect(toImmutableMap(
              fileType -> fileType,
              fileType -> KVKey.from(row, copyOf(optionalFks.get(fileType)))));
      builder.optionalFks(rowOptionalFks);
    } else {
      builder.optionalFks(emptyMap());
    }

    return builder.build();
  }

}
