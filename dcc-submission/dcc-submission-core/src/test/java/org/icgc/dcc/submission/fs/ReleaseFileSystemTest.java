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
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;

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

public class ReleaseFileSystemTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  @SneakyThrows
  public void testMoveFrom() {

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
    val previousSubmissionDir = new SubmissionDirectory(dccFileSystem, previousRelease, projectKey, previousSubmission);
    val previousSubmissionPath = new File(previousReleaseDir, projectKey).getAbsolutePath();
    val previousSubmissionValidationDir = new File(previousReleaseDir, projectKey + "/.validation");
    val previousSubmissionValidationDirPath = previousSubmissionValidationDir.getAbsolutePath();
    val previousSubmissionDonorFile = new File(previousSubmissionPath, submissionDonorFileName);
    val previousSubmissionSampleFile = new File(previousSubmissionPath, submissionSampleFileName);
    val previousSubmissionDonorErrorFile = new File(previousSubmissionValidationDirPath, "donor--errors.json");
    val previousSubmissionSampleErrorFile = new File(previousSubmissionValidationDirPath, "sample--errors.json");

    val previousSystemDir = new File(previousReleaseDir, "SystemFiles");
    val previousSystemPath = new Path(previousSystemDir.getAbsolutePath());

    // Create files and directories
    previousReleaseDir.mkdirs();
    previousSubmissionValidationDir.mkdirs();
    previousSystemDir.mkdirs();
    previousSubmissionDonorFile.createNewFile();
    previousSubmissionSampleFile.createNewFile();
    previousSubmissionDonorErrorFile.createNewFile();
    previousSubmissionSampleErrorFile.createNewFile();

    when(previousRelease.getName()).thenReturn(previousReleaseName);
    when(previousRelease.getSubmission(anyString())).thenReturn(previousSubmission);
    when(previousReleaseFileSystem.getSubmissionDirectory(projectKey)).thenReturn(previousSubmissionDir);
    when(previousReleaseFileSystem.getSystemDirectory()).thenReturn(previousSystemPath);
    when(previousSubmissionDir.getValidationDirPath()).thenReturn(previousSubmissionValidationDirPath);
    when(previousSubmissionDir.getDataFilePath(previousSubmissionDonorFile.getName())).thenReturn(
        previousSubmissionDonorFile.getAbsolutePath());
    when(previousSubmissionDir.getDataFilePath(previousSubmissionSampleFile.getName())).thenReturn(
        previousSubmissionSampleFile.getAbsolutePath());
    when(previousSubmissionDir.getDataFilePath(previousSubmissionDonorErrorFile.getName())).thenReturn(
        previousSubmissionDonorErrorFile.getAbsolutePath());
    when(previousSubmissionDir.getDataFilePath(previousSubmissionSampleErrorFile.getName())).thenReturn(
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

    // Create files and directories
    nextSubmissionDir.mkdirs();

    when(nextRelease.getName()).thenReturn(nextReleaseName);
    when(nextRelease.getSubmission(anyString())).thenReturn(mock(Submission.class));
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

    val projectKeys = newArrayList(projectKey);
    nextReleaseFileSystem.moveFrom(previousReleaseFileSystem, projectKeys);

    //
    // Verify
    //

    // The "moveFrom" validation folder moved
    assertThat(previousSubmissionValidationDir).doesNotExist();
    assertThat(nextSubmissionValidationDir).exists();

    // The "moveTo" is fully populated
    assertThat(nextSubmissionDir).exists();
    assertThat(nextSubmissionDonorFile).exists();
    assertThat(nextSubmissionSampleFile).exists();
  }

  @SneakyThrows
  private static LocalFileSystem createFileSystem() {
    return FileSystem.getLocal(new Configuration());
  }

}
