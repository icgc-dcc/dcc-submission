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

import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.core.report.ErrorType.FILE_HEADER_ERROR;

import java.util.List;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.first.core.FileChecker;

@Slf4j
public class FileHeaderChecker extends CompositeFileChecker {

  public FileHeaderChecker(FileChecker fileChecker, boolean failFast) {
    super(fileChecker, failFast);
  }

  public FileHeaderChecker(FileChecker fileChecker) {
    this(fileChecker, true);
  }

  @Override
  public void performSelfCheck(String fileName) {
    val expectedHeader = retrieveExpectedHeader(fileName);
    val actualHeader = getFileSystem().peekFileHeader(fileName);
    if (isExactMatch(expectedHeader, actualHeader)) {
      log.info("Correct header in '{}': '{}'", fileName, expectedHeader);
    } else {
      log.info(
          "Different from the expected header in '{}': '{}', actual header: '{}'",
          new Object[] { fileName, expectedHeader, actualHeader });

      incrementCheckErrorCount();

      getReportContext().reportError(
          error()
              .fileName(fileName)
              .type(FILE_HEADER_ERROR)
              .params(expectedHeader, actualHeader)
              .build());
    }
  }

  private final List<String> retrieveExpectedHeader(String filename) {
    return getFileSchema(filename).getFieldNames();
  }

  private boolean isExactMatch(List<String> expectedHeader, List<String> actualHeader) {
    return actualHeader.equals(expectedHeader);
  }
}
