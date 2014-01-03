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
package org.icgc.dcc.submission.validation.key.error;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Maps.newTreeMap;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.PRIMARY_RELATION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.SECONDARY_RELATION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.SURJECTION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.INCREMENTAL_UNIQUE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.EXISTING_UNIQUE;
import static org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator.SURJECTION_ERROR_LINE_NUMBER;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.submission.validation.key.KVFileDescription;
import org.icgc.dcc.submission.validation.key.data.KVKeyValues;
import org.icgc.dcc.submission.validation.key.enumeration.KVErrorType;

/**
 * 
 */
public class KVFileErrors {

  private final Map<KVErrorType, List<Integer>> fieldIndicesPerErrorType = newLinkedHashMap();
  private final Map<Long, KVRowError> rowErrors = newTreeMap();

  public KVFileErrors(@NonNull List<Integer> pkIndices) {
    this.fieldIndicesPerErrorType.put(EXISTING_UNIQUE, pkIndices);
    this.fieldIndicesPerErrorType.put(INCREMENTAL_UNIQUE, pkIndices);
    this.fieldIndicesPerErrorType.put(SURJECTION, pkIndices); // FIXME
  }

  public KVFileErrors(List<Integer> pkIndices, List<Integer> fkIndices) {
    this(pkIndices);
    fieldIndicesPerErrorType.put(PRIMARY_RELATION, fkIndices);
  }

  public KVFileErrors(
      @NonNull List<Integer> pkIndices,
      @NonNull List<Integer> fkIndices,
      @NonNull List<Integer> secondaryFkIndices) {
    this(pkIndices, fkIndices);
    fieldIndicesPerErrorType.put(SECONDARY_RELATION, secondaryFkIndices);
  }

  // TODO: factory instead of constructors
  public KVFileErrors(
      @NonNull Object ignoreMe,
      @NonNull List<Integer> fkIndices) {
    fieldIndicesPerErrorType.put(PRIMARY_RELATION, fkIndices);
  }

  public boolean hasError(long lineNumber) {
    return rowErrors.containsKey(lineNumber);
  }

  public void addSurjectionError(KVKeyValues keys) {
    addError(SURJECTION_ERROR_LINE_NUMBER, SURJECTION, keys);
  }

  /**
   * TODO: create other wrappers like the surjection one
   */
  public void addError(long lineNumber, KVErrorType type, KVKeyValues keys) {
    rowErrors.put(lineNumber, new KVRowError(type, keys)); // FIXME
  }

  public boolean describe(KVFileDescription kvFileDescription) {
    if (rowErrors.isEmpty()) {
      return true;
    } else {
      for (val entry : rowErrors.entrySet()) {
        val lineNumber = entry.getKey();
        val rowError = entry.getValue();
        val fieldIndices = checkNotNull(
            fieldIndicesPerErrorType
                .get(rowError.getType()),
            "TODO: %s", entry);
        rowError.describe(kvFileDescription, lineNumber, fieldIndices);
      }
      return false;
    }
  }

}
