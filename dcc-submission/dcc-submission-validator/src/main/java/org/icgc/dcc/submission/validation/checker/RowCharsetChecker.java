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
package org.icgc.dcc.submission.validation.checker;

import static com.google.common.base.CharMatcher.ASCII;
import static com.google.common.base.CharMatcher.JAVA_ISO_CONTROL;
import static com.google.common.base.CharMatcher.noneOf;

import java.util.List;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.core.ErrorCode;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class RowCharsetChecker extends CompositeRowChecker {

  private final static CharMatcher DEFAULT_INVALID_MATCHER =
      ASCII
          .negate()
          .or(JAVA_ISO_CONTROL).and(noneOf("\t"))
          .precomputed();

  public RowCharsetChecker(RowChecker rowChecker, boolean failFast) {
    super(rowChecker, failFast);
  }

  public RowCharsetChecker(RowChecker rowChecker) {
    this(rowChecker, false);
  }

  @Override
  public List<FirstPassValidationError> performSelfCheck(FileSchema fileSchema, String line, long lineNumber) {
    Builder<FirstPassValidationError> errors = ImmutableList.builder();
    if (DEFAULT_INVALID_MATCHER.matchesAnyOf(line)) {
      errors
          .add(new FirstPassValidationError(getCheckLevel(),
              "Invalid character found in the row: " + line, ErrorCode.INVALID_CHARSET_ROW_ERROR,
              new Object[] { Charsets.US_ASCII.name() }, lineNumber));
    }

    return errors.build();
  }
}
