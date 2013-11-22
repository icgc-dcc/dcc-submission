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
package org.icgc.dcc.submission.normalization.steps;

import static org.icgc.dcc.core.model.FieldNames.NormalizerFieldNames.NORMALIZER_MARKING;
import lombok.NonNull;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

import com.google.common.base.Optional;

/**
 * Enum representing the states of an observation with regard to sensitive information.
 */
public enum Masking {
  CONTROLLED, OPEN, MASKED;

  static final Fields NORMALIZER_MARKING_FIELD = new Fields(NORMALIZER_MARKING);

  /**
   * Returns the value to be used in the context of a {@link Tuple} (to avoid serialization issues).
   */
  public String getTupleValue() {
    return name();
  }

  /**
   * Optionally returns a {@link Masking} from a given {@link String}.
   */
  public static Optional<Masking> getMasking(@NonNull String value) {
    try {
      return Optional.<Masking> of(Masking.valueOf(value));
    } catch (IllegalArgumentException e) {
      return Optional.absent();
    }
  }
}