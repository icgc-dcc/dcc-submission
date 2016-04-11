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
package org.icgc.dcc.submission.validation.first.core;

import org.icgc.dcc.submission.core.report.Error;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.validation.core.ReportContext;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.first.io.FPVFileSystem;

import lombok.Getter;
import lombok.NonNull;

public class AbstractChecker implements Checker {

  /**
   * Dependencies.
   */
  @Getter
  @NonNull
  private final Dictionary dictionary;
  @Getter
  @NonNull
  private final ReportContext reportContext;
  @Getter
  @NonNull
  private final FPVFileSystem fileSystem;

  /**
   * Metadata.
   */
  @Getter
  private final boolean failFast;

  /**
   * Count for the errors of a given {@link Checker}.
   */
  protected long checkErrorCount = 0;

  public AbstractChecker(ValidationContext validationContext, FPVFileSystem fs) {
    this(validationContext, fs, false);
  }

  public AbstractChecker(ValidationContext validationContext, FPVFileSystem fileSystem, boolean failFast) {
    this.dictionary = validationContext.getDictionary();
    this.reportContext = validationContext;
    this.fileSystem = fileSystem;
    this.failFast = failFast;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public boolean canContinue() {
    return true;
  }

  protected void reportError(Error error) {
    checkErrorCount++;
    getReportContext().reportError(error);
  }

}
