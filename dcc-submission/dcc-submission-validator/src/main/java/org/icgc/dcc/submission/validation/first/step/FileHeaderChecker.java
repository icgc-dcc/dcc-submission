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

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.submission.validation.core.ErrorType.FILE_HEADER_ERROR;
import static org.icgc.dcc.submission.validation.platform.PlatformStrategy.FIELD_SPLITTER;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.fs.SubmissionDirectory.SubmissionDirectoryFile;
import org.icgc.dcc.submission.validation.first.FileChecker;
import org.icgc.dcc.submission.validation.first.Util;

@Slf4j
public class FileHeaderChecker extends CompositeFileChecker {

  public FileHeaderChecker(FileChecker fileChecker, boolean failFast) {
    super(fileChecker, failFast);
  }

  public FileHeaderChecker(FileChecker fileChecker) {
    this(fileChecker, true);
  }

  @Override
  public void performSelfCheck(String filename) {
    val expectedHeader = retrieveExpectedHeader(filename);
    val actualHeader = peekFileHeader(filename);
    if (isExactMatch(expectedHeader, actualHeader)) {
      log.info("Different from the expected header: '{}', actual header: '{}'", expectedHeader, actualHeader);

      incrementCheckErrorCount();
      getValidationContext().reportError(
          filename,
          FILE_HEADER_ERROR,
          expectedHeader, actualHeader);
    }
  }

  private final List<String> retrieveExpectedHeader(String filename) {
    return newArrayList(getFileSchema(filename)
        .getFieldNames());
  }

  /**
   * TODO: move to {@link SubmissionDirectoryFile}.
   * <p>
   * Files are expected to be present and uncorrupted at this stage.
   */
  @SneakyThrows
  private final List<String> peekFileHeader(String filename) {
    InputStream is = Util.createInputStream(getDccFileSystem(), getSubmissionDirectory().getDataFilePath(filename));
    @Cleanup
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String header = reader.readLine();
    return copyOf(FIELD_SPLITTER.split(header));
  }

  private boolean isExactMatch(List<String> expectedHeader, List<String> actualHeader) {
    return !actualHeader.equals(expectedHeader);
  }
}
