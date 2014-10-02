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
package org.icgc.dcc.submission.validation.first.file;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.core.report.ErrorType.TOO_MANY_FILES_ERROR;

import java.util.List;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.first.core.FileChecker;

@Slf4j
public class FileCollisionChecker extends DelegatingFileChecker {

  public FileCollisionChecker(FileChecker fileChecker) {
    this(fileChecker, true);
  }

  public FileCollisionChecker(FileChecker fileChecker, boolean failFast) {
    super(fileChecker, failFast);
  }

  @Override
  public void executeFileCheck(String fileName) {
    val fileSchema = getFileSchema(fileName);

    val pattern = fileSchema.getPattern();
    val fileNames = getFileSystem().getMatchingFileNames(pattern);
    log.info("Files: '{}'", fileNames);
    if (hasCollisions(fileNames)) {
      log.info("More than 1 file matching the file pattern: {}", pattern);

      incrementCheckErrorCount();

      getReportContext().reportError(
          error()
              .fileName(fileName)
              .type(TOO_MANY_FILES_ERROR)
              .params(fileSchema.getName(), copyOf(fileNames))
              .build());
    }
  }

  /**
   * Determines if a list of file names has collisions based on prefixes which would indicate either a poor choice in
   * naming or accidental re-submission.
   * <p>
   * e.g. {@code hasCollisions(of("donor.1.txt", "donor.1.txt.gz")) == true}
   * 
   * @param fileNames the file names to check
   * @return {@code true} if collisions exist, {@code false} otherwise
   */
  private boolean hasCollisions(@NonNull List<String> fileNames) {
    val size = fileNames.size();
    for (int i = 0; i < size; i++) {
      val a = fileNames.get(i);
      for (int j = i + 1; j < size; j++) {
        val b = fileNames.get(j);

        val prefix = a.startsWith(b) || b.startsWith(a);
        if (prefix) {
          return true;
        }
      }
    }

    return false;
  }

}
