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

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.ErrorType.ErrorLevel;
import org.icgc.dcc.submission.validation.first.FileChecker;

public class NoOpFileChecker implements FileChecker {

  @Getter
  @NonNull
  private final DccFileSystem dccFileSystem;
  @Getter
  @NonNull
  private final Dictionary dictionary;
  @Getter
  @NonNull
  private final SubmissionDirectory submissionDirectory;
  @Getter
  @NonNull
  private final ValidationContext validationContext;
  @Getter
  private final boolean failFast;

  public NoOpFileChecker(ValidationContext validationContext) {
    this(validationContext, false);
  }

  public NoOpFileChecker(ValidationContext validationContext, boolean failFast) {
    this.dccFileSystem = validationContext.getDccFileSystem();
    this.dictionary = validationContext.getDictionary();
    this.submissionDirectory = validationContext.getSubmissionDirectory();
    this.validationContext = validationContext;
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
