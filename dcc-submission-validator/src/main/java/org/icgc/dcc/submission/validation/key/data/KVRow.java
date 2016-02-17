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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.submission.validation.key.core.KVSubmissionProcessor.ROW_CHECKS_ENABLED;
import lombok.Builder;
import lombok.Value;

import org.icgc.dcc.submission.validation.key.core.KVKeyType;

/**
 * Data relevant to the key validation for a given row.
 */
@Value
@Builder
public class KVRow {

  /**
   * Applicable for most file except for the leafs (see dictionary DAG).
   */
  private final KVKey pk;

  /**
   * Applicable for all files but 'donor'.
   */
  private final KVKey fk1;

  /**
   * Only applicable for the array types.
   */
  private final KVKey fk2;

  /**
   * Only applicable for some meta files. See {@link KVKeyType#OPTIONAL_FK}.
   */
  private final KVKey optionalFk;

  public boolean hasPk() {
    return pk != null;
  }

  public boolean hasFk1() {
    return fk1 != null;
  }

  public boolean hasFk2() {
    return fk2 != null;
  }

  public boolean hasOptionalFk() {
    return optionalFk != null;
  }

  /**
   * Only applicable for existing non-composite keys.
   */
  public boolean hasCheckeableOptionalFk() {
    if (ROW_CHECKS_ENABLED) {
      checkState(!checkNotNull(optionalFk,
          "Expecting an optional FK to exist")
          .isSingleEmptyValue(), "Expecting optional FK to be a single value, instead: '{}'", optionalFk);
    }
    return !optionalFk.isSingleMissingCode();
  }

}