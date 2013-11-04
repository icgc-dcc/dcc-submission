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
package org.icgc.dcc.submission.validation.checker.step;

import static com.google.common.base.Charsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.validation.checker.RowChecker;
import org.icgc.dcc.submission.validation.checker.Util;
import org.icgc.dcc.submission.validation.core.ErrorType.ErrorLevel;

import com.google.common.base.Optional;

@Slf4j
public abstract class CompositeRowChecker extends CompositeFileChecker implements RowChecker {

  private static final Charset DEFAULT_CHARSET = UTF_8;
  protected RowChecker delegate;

  public CompositeRowChecker(RowChecker delegate, boolean failFast) {
    super(delegate, failFast);
    this.delegate = delegate;
  }

  public CompositeRowChecker(RowChecker nestedChecker) {
    this(nestedChecker, false);
  }

  @Override
  public void check(String filename) {
    // check all rows in the file
    performSelfCheck(filename);
  }

  @Override
  public void performSelfCheck(String filename) {
    String filePathname = getSubmissionDirectory().getDataFilePath(filename);
    FileSchema fileSchema = getFileSchema(filename);
    try {
      @Cleanup
      BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(
                  Util.createInputStream(
                      getDccFileSystem(),
                      filePathname),
                  DEFAULT_CHARSET));
      String line;
      long lineNumber = 1;
      while ((line = reader.readLine()) != null) {
        checkRow(filename, fileSchema, line, lineNumber);
        ++lineNumber;
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to check the file: " + filename, e);
    }
  }

  @Override
  public void checkRow(String filename, FileSchema fileSchema, String row, long lineNumber) {
    delegate.checkRow(filename, fileSchema, row, lineNumber);
    if (delegate.canContinue()) {
      log.info("Start performing {} validation...", this.getClass().getSimpleName());
      performSelfCheck(filename, fileSchema, row, lineNumber);
      log.info("End performing {} validation. Number of errors found: '{}'", this.getClass(), checkErrorCount);
    }
  }

  public abstract void performSelfCheck(String filename, FileSchema fileSchema, String row, long lineNumber);

  @Override
  public boolean isValid() {
    return !getValidationContext().hasErrors();
  }

  @Override
  public ErrorLevel getCheckLevel() {
    return delegate.getCheckLevel();
  }

  @Override
  public boolean isFailFast() {
    return failFast;
  }

  @Override
  public DccFileSystem getDccFileSystem() {
    return delegate.getDccFileSystem();
  }

  /**
   * TODO: remove
   */
  private FileSchema getFileSchema(String filename) {
    Optional<FileSchema> option = getDictionary().fileSchema(getFileSchemaName(filename));
    if (option.isPresent()) return option.get();
    else
      throw new RuntimeException("File Schema for filename: " + filename + " does not exist.");
  }
}
