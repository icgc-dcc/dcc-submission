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
package org.icgc.dcc.submission.checker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import lombok.Cleanup;

import org.icgc.dcc.submission.checker.Util.CheckLevel;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public abstract class CompositeRowChecker extends CompositeFileChecker implements RowChecker {

  protected RowChecker compositeChecker;
  protected boolean failFast;

  public CompositeRowChecker(RowChecker nestedChecker, boolean failFast) {
    super(nestedChecker, failFast);
    this.compositeChecker = nestedChecker;
  }

  public CompositeRowChecker(RowChecker nestedChecker) {
    this(nestedChecker, false);
  }

  @Override
  public List<FirstPassValidationError> check(String filename) {
    return performSelfCheck(filename);
  }

  @Override
  public List<FirstPassValidationError> performSelfCheck(String filename) {
    errors.clear();

    String filePathname = getSubmissionDirectory().getDataFilePath(filename);
    FileSchema fileSchema = getFileSchema(filename);
    try {
      @Cleanup
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(Util.createInputStream(getDccFileSystem(), filePathname)));
      String line;
      while ((line = reader.readLine()) != null) {
        errors.addAll(this.checkRow(fileSchema, line));
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to check the file: " + filename, e);
    }
    return errors;
  }

  @Override
  public List<FirstPassValidationError> checkRow(FileSchema fileSchema, String row) {
    Builder<FirstPassValidationError> errors = ImmutableList.builder();
    errors.addAll(compositeChecker.checkRow(fileSchema, row));
    if (compositeChecker.isValid() || !compositeChecker.isFailFast()) errors.addAll(performSelfCheck(fileSchema, row));
    return errors.build();
  }

  public abstract List<FirstPassValidationError> performSelfCheck(FileSchema fileSchema, String row);

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
  }

  @Override
  public DccFileSystem getDccFileSystem() {
    return compositeChecker.getDccFileSystem();
  }

  private FileSchema getFileSchema(String filename) {
    Optional<FileSchema> option = getDictionary().fileSchema(getFileSchemaName(filename));
    if (option.isPresent()) return option.get();
    else
      throw new RuntimeException("File Schema for filename: " + filename + " does not exist.");
  }
}
