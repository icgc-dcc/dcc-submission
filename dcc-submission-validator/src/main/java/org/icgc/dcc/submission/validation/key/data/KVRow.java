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
package org.icgc.dcc.submission.validation.key.data;

import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.CONDITIONAL_FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.OPTIONAL_FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.PK;

import java.util.Map;

import lombok.NonNull;
import lombok.Value;
import lombok.val;

import org.icgc.dcc.submission.validation.key.core.KVFileType;
import org.icgc.dcc.submission.validation.key.core.KVKeyType;

import com.google.common.collect.Table;

/**
 * Data relevant to the key validation for a given row.
 */
@Value
public class KVRow {

  @NonNull
  private final Table<KVKeyType, KVFileType, KVKey> keys;

  public KVKey getPk() {
    val pks = keys.row(PK);
    checkState(pks.size() == 1, "Expected pks size to be equal to 1. Pks: %s", pks);

    return pks.entrySet().iterator().next().getValue();
  }

  public boolean hasPk() {
    return hasKey(PK);
  }

  public Map<KVFileType, KVKey> getFks() {
    return keys.row(FK);
  }

  public Map<KVFileType, KVKey> getOptionalFks() {
    return keys.row(OPTIONAL_FK);
  }

  public boolean hasFk(KVFileType fileType) {
    return keys.get(FK, fileType) != null;
  }

  public boolean hasOptionalFks() {
    return hasKey(OPTIONAL_FK);
  }

  public boolean hasConditionalFks() {
    return hasKey(CONDITIONAL_FK);
  }

  public KVKey getFk(KVFileType fileType) {
    return keys.get(FK, fileType);
  }

  public Map<KVFileType, KVKey> getConditionalKeys() {
    return keys.row(CONDITIONAL_FK);
  }

  private boolean hasKey(KVKeyType keyType) {
    return !keys.row(keyType).isEmpty();
  }

}