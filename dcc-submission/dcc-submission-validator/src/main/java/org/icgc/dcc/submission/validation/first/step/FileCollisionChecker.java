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

import static org.icgc.dcc.submission.validation.core.ErrorType.TOO_MANY_FILES_ERROR;

import java.util.List;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.validation.first.FileChecker;

import com.google.common.collect.ImmutableList;

@Slf4j
public class FileCollisionChecker extends CompositeFileChecker {

  public FileCollisionChecker(FileChecker fileChecker) {
    this(fileChecker, true);
  }

  public FileCollisionChecker(FileChecker fileChecker, boolean failFast) {
    super(fileChecker, failFast);
  }

  @Override
  public void performSelfCheck(String filename) {
    FileSchema fileSchema = getFileSchema(filename);

    // more than 1 file that match the same pattern
    String pattern = fileSchema.getPattern();
    List<String> fileNames = listMatchingFiles(pattern);
    if (collisions(fileNames)) {
      log.info("More than 1 file matching the file pattern: " + pattern);

      incrementCheckErrorCount();
      getValidationContext().reportError(
          filename,
          TOO_MANY_FILES_ERROR,
          fileSchema.getName(),
          ImmutableList.of(fileNames));
    }
  }

  /**
   * TODO: move to {@link SubmissionDirectory}.
   */
  private List<String> listMatchingFiles(String pattern) {
    return ImmutableList.copyOf(getSubmissionDirectory().listFile(Pattern.compile(pattern)));
  }

  private boolean collisions(List<String> files) {
    return files.size() > 1;
  }
}
