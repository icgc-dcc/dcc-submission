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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.Getter;

import org.icgc.dcc.submission.checker.Util.CheckLevel;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class BaseFileChecker implements FileChecker {

  @Getter
  private final DccFileSystem dccFileSystem;

  @Getter
  private final Dictionary dictionary;
  @Getter
  private final SubmissionDirectory submissionDirectory;

  private final List<FirstPassValidationError> errors;
  private Map<String, String> cachedFileNames;
  private final boolean failFast;

  public BaseFileChecker(DccFileSystem fs, Dictionary dict, SubmissionDirectory submissionDir) {
    this(fs, dict, submissionDir, false);
  }

  public BaseFileChecker(DccFileSystem fs, Dictionary dict, SubmissionDirectory submissionDir, boolean failFast) {
    this.dccFileSystem = fs;
    this.dictionary = dict;
    this.submissionDirectory = submissionDir;
    this.errors = ImmutableList.of();
    this.failFast = false;
    cacheFileSchemaNames();
  }

  @Override
  public String getFileSchemaName(String filename) {
    return cachedFileNames.get(filename);
  }

  // TODO: could be used to determine if submission directory is well-formed
  // before the beginning of the other checks
  @Override
  public List<FirstPassValidationError> check(String filePathname) {
    return errors;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public CheckLevel getCheckLevel() {
    return CheckLevel.FILE_LEVEL;
  }

  @Override
  public boolean isFailFast() {
    return failFast;
  }

  private void cacheFileSchemaNames() {
    cachedFileNames = Maps.newHashMap();
    for (String filename : submissionDirectory.listFile()) {
      for (FileSchema schema : dictionary.getFiles()) {
        if (Pattern.matches(schema.getPattern(), filename)) {
          cachedFileNames.put(filename, schema.getName());
        }
      }
    }
  }
}
