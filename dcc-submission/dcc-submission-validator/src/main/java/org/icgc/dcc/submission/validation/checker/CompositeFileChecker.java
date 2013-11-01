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

import static com.google.common.collect.Lists.newLinkedList;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.validation.checker.Util.CheckLevel;

import com.google.common.collect.ImmutableList;

@Slf4j
@RequiredArgsConstructor
public abstract class CompositeFileChecker implements FileChecker {

  protected final FileChecker compositeChecker;
  protected final boolean failFast;

  protected List<FirstPassValidationError> errors = newLinkedList();
  protected List<FirstPassValidationError> checkErrors = ImmutableList.of();

  public CompositeFileChecker(FileChecker nestedChecker) {
    this(nestedChecker, false);
  }

  @Override
  public List<FirstPassValidationError> check(String filename) {
    errors.clear();
    errors.addAll(compositeChecker.check(filename));
    if (compositeChecker.canContinue()) {
      log.info("Start performing {} validation...", this.getClass().getSimpleName());
      checkErrors = performSelfCheck(filename);
      errors.addAll(checkErrors);
      log.info("End performing {} validation. Number of errors found: {}", new Object[] { this.getClass()
          .getSimpleName(), checkErrors.size() });
    }
    return errors;
  }

  public abstract List<FirstPassValidationError> performSelfCheck(String filename);

  @Override
  public boolean canContinue() {
    return (compositeChecker.canContinue() && (checkErrors.isEmpty() || !failFast));
  }

  @Override
  public boolean isValid() {
    return (compositeChecker.isValid() && errors.isEmpty());
  }

  @Override
  public CheckLevel getCheckLevel() {
    return compositeChecker.getCheckLevel();
  }

  @Override
  public boolean isFailFast() {
    return failFast;
    // return compositeChecker.isFailFast() || failFast;
  }

  @Override
  public String getFileSchemaName(String filename) {
    return compositeChecker.getFileSchemaName(filename);
  }

  @Override
  public Dictionary getDictionary() {
    return compositeChecker.getDictionary();
  }

  @Override
  public SubmissionDirectory getSubmissionDirectory() {
    return compositeChecker.getSubmissionDirectory();
  }

  @Override
  public DccFileSystem getDccFileSystem() {
    return compositeChecker.getDccFileSystem();
  }
}
