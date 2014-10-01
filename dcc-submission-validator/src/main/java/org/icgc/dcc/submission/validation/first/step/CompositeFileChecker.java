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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.repeat;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.core.ReportContext;
import org.icgc.dcc.submission.validation.first.core.Checker;
import org.icgc.dcc.submission.validation.first.core.FileChecker;
import org.icgc.dcc.submission.validation.first.io.FPVFileSystem;

@Slf4j
public abstract class CompositeFileChecker implements FileChecker {

  @NonNull
  protected final String name;
  @NonNull
  protected final FileChecker delegate;
  protected final boolean failFast;

  /**
   * Count for the errors of a given {@link Checker}.
   */
  protected long checkErrorCount = 0;

  public CompositeFileChecker(FileChecker delegate, boolean failFast) {
    this.delegate = delegate;
    this.failFast = failFast;
    this.name = this.getClass().getSimpleName();
  }

  public CompositeFileChecker(FileChecker delegate) {
    this(delegate, false);
  }

  @Override
  public void checkFile(String fileName) {
    delegate.checkFile(fileName);
    log.info(banner());
    if (delegate.canContinue()) {
      log.info("Start performing {} validation...", name);
      performSelfCheck(fileName);
      log.info("End performing {} validation. Number of errors found: '{}'",
          name,
          checkErrorCount);
    }
  }

  public abstract void performSelfCheck(String fileName);

  /**
   * Must always increment when reporting an error (TODO: address this).
   */
  protected void incrementCheckErrorCount() {
    checkErrorCount++;
  }

  @Override
  public boolean canContinue() {
    return (delegate.canContinue() && (checkErrorCount == 0 || !failFast));
  }

  @Override
  public boolean isValid() {
    return (delegate.isValid() && !getReportContext().hasErrors());
  }

  @Override
  public boolean isFailFast() {
    return failFast;
  }

  @Override
  public Dictionary getDictionary() {
    return delegate.getDictionary();
  }

  @Override
  public FPVFileSystem getFileSystem() {
    return delegate.getFileSystem();
  }

  @Override
  public ReportContext getReportContext() {
    return delegate.getReportContext();
  }

  protected FileSchema getFileSchema(String fileName) {
    val optional = getDictionary().getFileSchemaByFileName(fileName);
    checkState(optional.isPresent(), "At this stage, there should be a file schema matching '%s'", fileName);
    return optional.get();
  }

  protected String banner() {
    return repeat("-", 75);
  }
}
