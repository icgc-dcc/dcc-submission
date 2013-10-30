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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.cascading.TupleState.TupleError;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

@Slf4j
public class FirstPassChecker {

  final private Dictionary dict;
  final private SubmissionDirectory submissionDir;

  final private RowChecker rowChecker;
  final private FileChecker fileChecker;

  private final Map<String, List<FirstPassValidationError>> errorMap;

  public FirstPassChecker(Dictionary dict, SubmissionDirectory submissionDir,
      FileChecker fileChecker, RowChecker rowChecker) {
    this.dict = dict;
    this.submissionDir = submissionDir;
    this.fileChecker = fileChecker;
    this.rowChecker = rowChecker;
    this.errorMap = Maps.newHashMap();
  }

  public FirstPassChecker(DccFileSystem dccFileSystem, Dictionary dict, SubmissionDirectory submissionDir) {
    this(dict, submissionDir, getDefaultFileChecker(dccFileSystem, dict, submissionDir),
        getDefaultRowChecker(dccFileSystem, dict, submissionDir));
  }

  public static FileChecker getDefaultFileChecker(DccFileSystem fs, Dictionary dict, SubmissionDirectory submissionDir) {
    // chaining multiple file checker
    return new FileHeaderChecker( //
        new FileCorruptionChecker(//
            new FileCollisionChecker(//
                new ReferentialFileChecker( //
                    new BaseFileChecker(fs, dict, submissionDir)))));
  }

  public static RowChecker getDefaultRowChecker(DccFileSystem fs, Dictionary dict, SubmissionDirectory submissionDir) {
    // chaining multiple row checkers
    return new RowColumnChecker(new RowCharsetChecker(new BaseRowChecker(fs, dict, submissionDir)));

  }

  public boolean isValid() {
    errorMap.clear();
    for (String filename : submissionDir.listFile()) {
      String fileSchemaName = getFileSchemaName(filename);
      if (fileSchemaName != null) {
        Builder<FirstPassValidationError> errors = ImmutableList.<FirstPassValidationError> builder();
        log.info("Validate file level well-formness for file schema: {}", fileSchemaName);
        errors.addAll(fileChecker.check(filename));
        if (fileChecker.isValid() || !fileChecker.isFailFast()) {
          errors.addAll(rowChecker.check(filename));
        }
        errorMap.put(fileSchemaName, errors.build());
      }
    }
    List<FirstPassValidationError> flattenListOfErrors = ImmutableList.copyOf(Iterables.concat(errorMap.values()));
    return (flattenListOfErrors.size() == 0);
  }

  public Set<String> getFileSchemaNames() {
    return errorMap.keySet();
  }

  public List<TupleError> getTupleErrors(String fileSchemaName) {
    List<FirstPassValidationError> errors = errorMap.get(fileSchemaName);
    TupleState state = new TupleState();
    for (val error : errors) {
      state.reportError(error.getCode(), error.getLevel().toString(), error.toString(), error.getParam());
    }
    return ImmutableList.copyOf(state.getErrors());
  }

  private String getFileSchemaName(String filename) {
    for (FileSchema schema : dict.getFiles()) {
      if (Pattern.matches(schema.getPattern(), filename)) {
        return schema.getName();
      }
    }
    return null;
  }
}
