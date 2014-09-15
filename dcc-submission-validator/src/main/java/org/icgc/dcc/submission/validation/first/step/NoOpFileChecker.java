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
package org.icgc.dcc.submission.validation.first.step;

import lombok.Getter;
import lombok.NonNull;

import org.icgc.dcc.submission.core.report.ErrorType.ErrorLevel;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.validation.core.ReportContext;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.first.core.FileChecker;
import org.icgc.dcc.submission.validation.first.io.FPVFileSystem;

public class NoOpFileChecker implements FileChecker {

  @Getter
  @NonNull
  private final Dictionary dictionary;
  @Getter
  @NonNull
  private final ReportContext reportContext;
  @Getter
  @NonNull
  private final FPVFileSystem fs;
  @Getter
  private final boolean failFast;

  public NoOpFileChecker(ValidationContext validationContext, FPVFileSystem fs) {
    this(validationContext, fs, false);
  }

  public NoOpFileChecker(ValidationContext validationContext, FPVFileSystem fs, boolean failFast) {
    this.dictionary = validationContext.getDictionary();
    this.reportContext = validationContext;
    this.fs = fs;
    this.failFast = false;
  }

  // TODO: Could be used to determine if submission directory is well-formed
  // before the beginning of the other checks
  @Override
  public void check(String filePathname) {
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public ErrorLevel getCheckLevel() {
    return ErrorLevel.FILE_LEVEL;
  }

  @Override
  public boolean canContinue() {
    return true;
  }
}
