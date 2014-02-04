/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.core;

import static java.lang.String.format;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.SubmissionDataType;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.validation.cascading.TupleState.TupleError;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractValidationContext implements ValidationContext {

  protected static final String DICTIONARY_VERSION = "0.7e";

  @Override
  public PlatformStrategy getPlatformStrategy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getProjectKey() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<SubmissionDataType> getDataTypes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> getEmails() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Release getRelease() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Dictionary getDictionary() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileSystem getFileSystem() {
    throw new UnsupportedOperationException();
  }

  @Override
  public DccFileSystem getDccFileSystem() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ReleaseFileSystem getReleaseFileSystem() {
    return new ReleaseFileSystem(getDccFileSystem(), getRelease());
  }

  @Override
  public SubmissionDirectory getSubmissionDirectory() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Path> getSsmPrimaryFiles() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileSchema getSsmPrimaryFileSchema() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SubmissionReport getSubmissionReport() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasErrors() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getErrorCount() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void reportSummary(String fileName, String name, String value) {
    new UnsupportedOperationException();
  }

  @Override
  public void reportField(String fileName, FieldReport fieldReport) {
    new UnsupportedOperationException();
  }

  @Override
  public void reportError(String fileName, TupleError tupleError) {
    logError(fileName,
        tupleError.getLine(),
        tupleError.getColumnNames().toString(),
        tupleError.getValue(),
        tupleError.getType(),
        tupleError.getParameters().values().toArray());
  }

  @Override
  public void reportError(String fileName, long lineNumber, String columnName, Object value, ErrorType type,
      Object... params) {
    logError(fileName, lineNumber, columnName, value, type, params);
  }

  @Override
  public void reportError(String fileName, long lineNumber, Object value, ErrorType type, Object... params) {
    logError(fileName, lineNumber, null, value, type, params);
  }

  @Override
  public void reportError(String fileName, Object value, ErrorType type, Object... params) {
    logError(fileName, -1, null, value, type, params);
  }

  @Override
  public void reportError(String fileName, ErrorType type, Object... params) {
    logError(fileName, -1, null, null, type, params);
  }

  @Override
  public void reportError(String fileName, ErrorType type) {
    logError(fileName, -1, null, null, type, (Object[]) null);
  }

  @Override
  public void reportLineNumbers(Path path) {
    new UnsupportedOperationException();
  }

  private static void logError(String fileName, long lineNumber, String columnName, Object value, ErrorType type,
      Object... params) {
    val message =
        "[reportError] fileName = '%s', lineNumber = %s, columnName = %s, value = %s, type = %s, params = %s";
    val text = format(message, fileName, lineNumber, columnName, value, type, Arrays.toString(params));
    log.error("{}", text);
  }

}