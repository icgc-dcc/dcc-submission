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
package org.icgc.dcc.submission.fs;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.submission.fs.ReleaseFileSystem.SYSTEM_FILES_DIR_NAME;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Optional;

public class ReleaseFileSystemTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  @SneakyThrows
  public void testCopyFrom() {

    //
    // Setup: Environment
    //

    // Once upon a time there was a file system...
    val rootDir = tmp.newFolder();
    val dccFileSystem = mock(DccFileSystem.class);

    // And in that file system lived a simple project with two clinical files:
    val projectKey = "project1";
    val submissionDonorFileName = "donor.txt";
    val submissionSampleFileName = "sample.txt";

    //
    // Setup: Previous release
    //

    // But there was a dark past:
    val previousReleaseName = "ICGC14";
    val previousRelease = mock(Release.class);
    val previousReleaseFileSystem = mock(ReleaseFileSystem.class);
    val previousReleaseDir = new File(rootDir, previousReleaseName);

    val previousSubmission = mock(Submission.class);
    val previousSubmissionDirectory = new SubmissionDirectory(
        dccFileSystem, previousReleaseFileSystem, previousRelease, projectKey, previousSubmission);
    val previousSubmissionDir = new File(previousReleaseDir, projectKey);
    val previousSubmissionPath = previousSubmissionDir.getAbsolutePath();
    val previousSubmissionSampleFile = new File(previousSubmissionPath, submissionSampleFileName);
    val previousSubmissionDonorFile = new File(previousSubmissionPath, submissionDonorFileName);

    val previousSubmissionValidationDir = new File(previousReleaseDir, projectKey + "/.validation");
    val previousSubmissionValidationDirPath = previousSubmissionValidationDir.getAbsolutePath();
    val previousSubmissionDonorErrorFile = new File(previousSubmissionValidationDirPath, "donor--errors.json");
    val previousSubmissionSampleErrorFile = new File(previousSubmissionValidationDirPath, "sample--errors.json");

    val previousSystemDir = new File(previousReleaseDir, SYSTEM_FILES_DIR_NAME);
    val previousSystemPath = new Path(previousSystemDir.getAbsolutePath());
    val previousSystemFile = new File(previousSystemDir, "system.txt");

    // Create files and directories
    previousReleaseDir.mkdirs();
    previousSubmissionDir.mkdirs();
    previousSubmissionDonorFile.createNewFile();
    previousSubmissionSampleFile.createNewFile();

    previousSubmissionValidationDir.mkdirs();
    previousSubmissionDonorErrorFile.createNewFile();
    previousSubmissionSampleErrorFile.createNewFile();

    previousSystemDir.mkdirs();
    previousSystemFile.createNewFile();

    // Mock
    when(previousRelease.getName()).thenReturn(previousReleaseName);
    when(previousRelease.getSubmission(anyString())).thenReturn(
        Optional.<Submission> of(previousSubmission));
    when(previousReleaseFileSystem.getSubmissionDirectory(projectKey)).thenReturn(previousSubmissionDirectory);
    when(previousReleaseFileSystem.getSystemDirPath()).thenReturn(previousSystemPath);
    when(previousSubmissionDirectory.getValidationDirPath()).thenReturn(previousSubmissionValidationDirPath);
    when(previousSubmissionDirectory.getDataFilePath(previousSubmissionDonorFile.getName())).thenReturn(
        previousSubmissionDonorFile.getAbsolutePath());
    when(previousSubmissionDirectory.getDataFilePath(previousSubmissionSampleFile.getName())).thenReturn(
        previousSubmissionSampleFile.getAbsolutePath());
    when(previousSubmissionDirectory.getDataFilePath(previousSubmissionDonorErrorFile.getName())).thenReturn(
        previousSubmissionDonorErrorFile.getAbsolutePath());
    when(previousSubmissionDirectory.getDataFilePath(previousSubmissionSampleErrorFile.getName())).thenReturn(
        previousSubmissionSampleErrorFile.getAbsolutePath());

    //
    // Setup: Next release
    //

    // Which gave birth to the next heir
    val nextReleaseName = "ICGC15";
    val nextRelease = mock(Release.class);
    val nextReleaseDir = new File(rootDir, nextReleaseName);
    val nextReleaseFileSystem = new ReleaseFileSystem(dccFileSystem, nextRelease);

    val nextSubmissionDir = new File(nextReleaseDir, projectKey);
    val nextSubmissionPath = nextSubmissionDir.getAbsolutePath();
    val nextSubmissionDonorFile = new File(nextSubmissionPath, submissionDonorFileName);
    val nextSubmissionSampleFile = new File(nextSubmissionPath, submissionSampleFileName);

    val nextSubmissionValidationDir = new File(nextReleaseDir, projectKey + "/.validation");
    val nextSubmissionDonorErrorFile = new File(nextSubmissionValidationDir, "donor--errors.json");
    val nextSubmissionSampleErrorFile = new File(nextSubmissionValidationDir, "sample--errors.json");

    val nextSystemDir = new File(nextReleaseDir, SYSTEM_FILES_DIR_NAME);
    val nextSystemFile = new File(nextSystemDir, "system.txt");

    when(nextRelease.getName()).thenReturn(nextReleaseName);
    when(nextRelease.getSubmission(anyString())).thenReturn(
        Optional.<Submission> of(mock(Submission.class)));
    when(dccFileSystem.buildFileStringPath(nextReleaseName, projectKey, submissionDonorFileName)).thenReturn(
        nextSubmissionDonorFile.getAbsolutePath());
    when(dccFileSystem.buildFileStringPath(nextReleaseName, projectKey, submissionSampleFileName)).thenReturn(
        nextSubmissionSampleFile.getAbsolutePath());
    when(dccFileSystem.buildValidationDirStringPath(nextReleaseName, projectKey)).thenReturn(
        nextSubmissionValidationDir.getAbsolutePath());

    //
    // Setup: File system
    //

    // Boot the file system
    when(dccFileSystem.getFileSystem()).thenReturn(createFileSystem());
    when(dccFileSystem.getRootStringPath()).thenReturn(rootDir.getAbsolutePath());
    when(dccFileSystem.buildProjectStringPath(previousReleaseName, projectKey)).thenReturn(previousSubmissionPath);
    when(dccFileSystem.buildProjectStringPath(nextReleaseName, projectKey)).thenReturn(nextSubmissionPath);

    //
    // Exercise
    //

    val projectKeys = projectKeys(projectKey);
    nextReleaseFileSystem.setUpNewReleaseFileSystem(nextReleaseName, previousReleaseFileSystem, projectKeys);

    //
    // Verify
    //

    // The "moveFrom" validation folder moved
    assertThat(previousSubmissionDir).exists();

    assertThat(previousSubmissionValidationDir).exists();
    assertThat(previousSubmissionDonorFile).exists();
    assertThat(previousSubmissionSampleFile).exists();

    assertThat(previousSystemDir).exists();
    assertThat(previousSystemFile).exists();

    // The "moveTo" is fully populated
    assertThat(nextSubmissionDir).exists();
    assertThat(nextSubmissionDonorFile).exists();
    assertThat(nextSubmissionSampleFile).exists();

    assertThat(nextSubmissionValidationDir).exists();
    assertThat(nextSubmissionDonorErrorFile).exists();
    assertThat(nextSubmissionSampleErrorFile).exists();

    assertThat(nextSystemDir).exists();
    assertThat(nextSystemFile).exists();
  }

  private static List<String> projectKeys(String projectKey) {
    return newArrayList(projectKey);
  }

  @SneakyThrows
  private static LocalFileSystem createFileSystem() {
    return FileSystem.getLocal(new Configuration());
  }

}
