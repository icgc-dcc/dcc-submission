/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
import static org.icgc.dcc.submission.validation.key.core.KVSubmissionProcessor.ROW_CHECKS_ENABLED;

import java.util.List;

import lombok.Value;
import lombok.val;
import lombok.Builder;

import org.icgc.dcc.submission.validation.key.data.KVKey;
import org.icgc.dcc.submission.validation.key.data.KVRow;

/**
 * Represents the indices for the keys that are relevant to a particular {@link KVFileType}.
 */
@Value
@Builder
public class KVFileTypeKeysIndices {

  private final List<Integer> pk;
  private final List<Integer> fk1;
  private final List<Integer> fk2;
  private final List<Integer> optionalFk;

  public KVRow getRow(List<String> row) {
    if (ROW_CHECKS_ENABLED) checkState(
        pk != null || fk1 != null, "Invalid row: '%s'", row);
    val builder = KVRow.builder();

    if (pk != null) {
      builder.pk(KVKey.from(row, pk));
    }
    if (fk1 != null) {
      builder.fk1(KVKey.from(row, fk1));
    }
    if (fk2 != null) {
      builder.fk2(KVKey.from(row, fk2));
    }
    if (optionalFk != null) {
      builder.optionalFk(KVKey.from(row, optionalFk));
    }

    return builder.build();
  }
}
