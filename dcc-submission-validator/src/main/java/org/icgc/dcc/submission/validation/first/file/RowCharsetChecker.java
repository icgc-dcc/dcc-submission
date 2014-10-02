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
package org.icgc.dcc.submission.validation.first.file;

import static com.google.common.base.CharMatcher.ASCII;
import static com.google.common.base.CharMatcher.JAVA_ISO_CONTROL;
import static com.google.common.base.CharMatcher.noneOf;
import static com.google.common.base.Charsets.US_ASCII;
import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.core.report.ErrorType.INVALID_CHARSET_ROW_ERROR;
import static org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy.FIELD_SEPARATOR;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.first.core.RowChecker;

import com.google.common.base.CharMatcher;

@Slf4j
public class RowCharsetChecker extends DelegatingFileRowChecker {

  private final static CharMatcher DEFAULT_INVALID_MATCHER =
      ASCII
          .negate()
          .or(JAVA_ISO_CONTROL)
          .and(
              noneOf(FIELD_SEPARATOR))
          .precomputed();

  public RowCharsetChecker(RowChecker rowChecker, boolean failFast) {
    super(rowChecker, failFast);
  }

  public RowCharsetChecker(RowChecker rowChecker) {
    this(rowChecker, false);
  }

  @Override
  void performSelfCheck(
      String fileName,
      FileSchema fileSchema,
      CharSequence line,
      long lineNumber) {

    if (containsInvalidCharacter(line)) {
      log.info("Invalid character found in the row: {}", line);

      reportError(error()
          .fileName(fileName)
          .lineNumber(lineNumber)
          .type(INVALID_CHARSET_ROW_ERROR)
          .params(US_ASCII.name()) // TODO: return actual list
          .build());
    }
  }

  @Override
  void performSelfFinish(String fileName, FileSchema fileSchema) {
    // No-op
  }

  private static boolean containsInvalidCharacter(CharSequence line) {
    return DEFAULT_INVALID_MATCHER.matchesAnyOf(line);
  }
}
