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
package org.icgc.dcc.filesystem;

import java.io.InputStream;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;

import static com.google.common.base.Preconditions.checkArgument;

public class SubmissionDirectory {

  private final DccFileSystem dccFileSystem;

  private final Release release;

  private final String projectKey;

  private final Submission submission;

  public SubmissionDirectory(DccFileSystem dccFileSystem, Release release, String projectKey, Submission submission) {
    super();

    checkArgument(dccFileSystem != null);
    checkArgument(release != null);
    checkArgument(projectKey != null);
    checkArgument(submission != null);

    this.dccFileSystem = dccFileSystem;
    this.release = release;
    this.projectKey = projectKey;
    this.submission = submission;
  }

  /**
   * (non-recursive) TODO: confirm
   */
  public Iterable<String> listFile(Pattern pattern) {
    List<Path> pathList = HadoopUtils.lsFile(this.dccFileSystem.getFileSystem(), getSubmissionDirPath(), pattern);
    return HadoopUtils.toFilenameList(pathList);
  }

  public Iterable<String> listFile() {
    return this.listFile(null);
  }

  public String addFile(String filename, InputStream data) {
    String filepath = this.dccFileSystem.buildFileStringPath(this.release, this.projectKey, filename);
    HadoopUtils.touch(this.dccFileSystem.getFileSystem(), filepath, data);
    return filepath;
  }

  public String deleteFile(String filename) {
    String filepath = this.dccFileSystem.buildFileStringPath(this.release, this.projectKey, filename);
    HadoopUtils.rm(this.dccFileSystem.getFileSystem(), filepath);
    return filepath;
  }

  public boolean isReadOnly() {
    SubmissionState state = this.submission.getState();

    return (state.isReadOnly() || this.release.getState() == ReleaseState.COMPLETED);
  }

  public String getProjectKey() {
    return this.projectKey;
  }

  public Path getSubmissionDirPath() {
    String projectStringPath = dccFileSystem.buildProjectStringPath(release, projectKey);
    return new Path(projectStringPath);
  }

  public String getValidationDirPath() {
    return dccFileSystem.buildValidationDirStringPath(release, projectKey);
  }

  public String getDataFilePath(String filename) {
    return dccFileSystem.buildFileStringPath(release, projectKey, filename);
  }

  public Submission getSubmission() {
    return this.submission;
  }

  public void removeSubmissionDir() {
    HadoopUtils.rmr(this.dccFileSystem.getFileSystem(), getValidationDirPath());
  }
}
