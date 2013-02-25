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
package org.icgc.dcc.sftp;

import static com.google.common.base.Charsets.UTF_8;
import static java.lang.String.format;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.RawLocalFileSystem;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.filesystem.SubmissionDirectory;
import org.icgc.dcc.release.NextRelease;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.typesafe.config.Config;

@RunWith(MockitoJUnitRunner.class)
public class SftpServerServiceTest {

  private static final String RELEASE_NAME = "release1";

  private static final String PROJECT_NAME = "project1";

  // @formatter:off
  @Rule public TemporaryFolder tmp = new TemporaryFolder();
  @Rule public Sftp sftp = new Sftp("username", "password");

  @Mock Config config;
  @Mock Subject subject;
  @Mock UsernamePasswordAuthenticator passwordAuthenticator;
  
  @Mock Release release;
  @Mock Submission submission;
  @Mock Project project;
  @Mock NextRelease nextRelease;

  @Mock DccFileSystem fs;
  @Mock SubmissionDirectory submissionDirectory;
  @Mock ReleaseFileSystem releaseFileSystem;

  @Mock ProjectService projectService;
  @Mock ReleaseService releaseService;

  SftpServerService service;
  File root;
  // @formatter:on

  @Before
  public void setUp() throws IOException, JSchException {
    root = tmp.newFolder(RELEASE_NAME);

    // Mock configuration
    when(config.getInt("sftp.port")).thenReturn(sftp.getPort());
    when(config.getString("sftp.path")).thenReturn("");

    // Mock authentication
    when(passwordAuthenticator.authenticate(anyString(), (char[]) any(), anyString())).thenReturn(subject);
    when(passwordAuthenticator.getSubject()).thenReturn(subject);

    // Mock release / project
    when(nextRelease.getRelease()).thenReturn(release);
    when(releaseService.getNextRelease()).thenReturn(nextRelease);
    when(projectService.getProject(PROJECT_NAME)).thenReturn(project);

    // Mock file system
    when(fs.buildReleaseStringPath(release)).thenReturn(root.getAbsolutePath());
    when(fs.getReleaseFilesystem(release, subject)).thenReturn(releaseFileSystem);
    when(fs.getFileSystem()).thenReturn(fileSystem());
    when(releaseFileSystem.getDccFileSystem()).thenReturn(fs);
    when(releaseFileSystem.getRelease()).thenReturn(release);
    when(releaseFileSystem.getSubmissionDirectory(project)).thenReturn(submissionDirectory);
    when(submissionDirectory.isReadOnly()).thenReturn(false);
    when(submissionDirectory.getSubmission()).thenReturn(submission);

    // Create an start CUT
    service = new SftpServerService(config, passwordAuthenticator, fs, projectService, releaseService);
    service.startAndWait();

    sftp.connect();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testService() throws JSchException, SftpException {
    // Create the simulated project directory
    String projectDirectoryName = "/" + PROJECT_NAME;
    File projectDirectory = new File(root, projectDirectoryName);
    projectDirectory.mkdir();

    // Original file
    String fileName = fileName(1);
    String fileContent = "This is the content of the file";
    File file = new File(root, fileName);

    // New file
    String newFileName = fileName(2);
    File newFile = new File(root, newFileName);

    // Initial state
    assertThat(sftp.getChannel().pwd()).isEqualTo("/");
    assertThat(sftp.getChannel().ls(projectDirectoryName)).hasSize(0);

    // Change directory
    sftp.getChannel().cd(projectDirectoryName);
    assertThat(sftp.getChannel().pwd()).isEqualTo(projectDirectoryName);

    // Put file
    sftp.getChannel().put(inputStream(fileContent), fileName);
    assertThat(file).exists().hasContent(fileContent);
    assertThat(sftp.getChannel().ls(projectDirectoryName)).hasSize(1);

    // Rename file
    sftp.getChannel().rename(fileName, newFileName);
    assertThat(file).doesNotExist();
    assertThat(newFile).exists().hasContent(fileContent);
    assertThat(sftp.getChannel().ls(projectDirectoryName)).hasSize(1);

    // Remove file
    sftp.getChannel().rm(newFileName);
    assertThat(newFile).doesNotExist();
    assertThat(sftp.getChannel().ls(projectDirectoryName)).hasSize(0);
  }

  @Test(expected = SftpException.class)
  public void testRemoveNonExistentFile() throws SftpException {
    sftp.getChannel().rm("/does/not/exist");
  }

  @Test(expected = SftpException.class)
  public void testCdIntoNonExistent() throws SftpException {
    sftp.getChannel().cd("/does/not/exist");
  }

  @Test
  public void testActiveSession() throws InterruptedException {
    // Connected
    assertThat(service.getActiveSessions()).isEqualTo(1);

    // Disconnect
    sftp.disconnect();
    Thread.sleep(1000); // Allow for asynchronous disconnection latency
    assertThat(service.getActiveSessions()).isEqualTo(0);
  }

  @After
  public void tearDown() {
    service.stop();
  }

  private static RawLocalFileSystem fileSystem() {
    RawLocalFileSystem localFileSystem = new RawLocalFileSystem();
    localFileSystem.setConf(new Configuration());

    return localFileSystem;
  }

  private static String fileName(int i) {
    return format("/%s/file%s.txt", PROJECT_NAME, i);
  }

  private static InputStream inputStream(String text) {
    return new ByteArrayInputStream(text.getBytes(UTF_8));
  }

}
