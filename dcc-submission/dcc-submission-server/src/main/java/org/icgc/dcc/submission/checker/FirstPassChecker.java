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
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.Cleanup;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.elasticsearch.common.collect.Maps;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.cascading.TupleState.TupleError;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;

/**
 * 
 */
public class FirstPassChecker {

  final private Dictionary dict;
  final private SubmissionDirectory submissionDir;
  final private DccFileSystem dccFileSystem;

  final private RowChecker rowChecker;
  final private FileChecker fileChecker;

  private final Map<String, List<FirstPassValidationError>> errMap;

  public FirstPassChecker(DccFileSystem dccFileSystem, Dictionary dict, SubmissionDirectory submissionDir,
      FileChecker fileChecker, RowChecker rowChecker) {
    this.dccFileSystem = dccFileSystem;
    this.dict = dict;
    this.submissionDir = submissionDir;
    this.fileChecker = fileChecker;
    this.rowChecker = rowChecker;
    this.errMap = Maps.newHashMap();

  }

  public FirstPassChecker(DccFileSystem dccFileSystem, Dictionary dict, SubmissionDirectory submissionDir) {
    // TODO: create default checkers
    this(dccFileSystem, dict, submissionDir, null, null);
  }

  public boolean isValid() throws IOException {
    errMap.clear();
    for (String filename : submissionDir.listFile()) {
      String fileSchemaName = getFileSchemaName(filename);
      if (fileSchemaName != null) {
        Builder<FirstPassValidationError> errorsBuilder = ImmutableList.<FirstPassValidationError> builder();
        String filePathname = submissionDir.getDataFilePath(filename);
        Path filePath = new Path(filePathname);
        errorsBuilder.addAll(fileChecker.check(filePathname));
        if (fileChecker.isValid() || !fileChecker.isFailFast()) {
          @Cleanup
          BufferedReader reader = new BufferedReader(new InputStreamReader(dccFileSystem.open(filePath)));
          String line;
          while ((line = reader.readLine()) != null) {
            errorsBuilder.addAll(rowChecker.check(line));
          }
        }
        errMap.put(fileSchemaName, errorsBuilder.build());
      }
    }
    return !(Iterables.concat(errMap.values()).iterator().hasNext());
  }

  public Set<String> getFileSchemaNames() {
    return errMap.keySet();
  }

  public List<TupleError> getTupleErrors(String fileSchemaName) {
    List<FirstPassValidationError> errors = errMap.get(fileSchemaName);
    TupleState state = new TupleState();
    for (val error : errors) {
      state.reportError(error.getCode(), error.getLevel().toString(), error.toString());
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
