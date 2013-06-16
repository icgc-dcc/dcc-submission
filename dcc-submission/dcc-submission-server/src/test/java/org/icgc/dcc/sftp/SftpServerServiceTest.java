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
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.RawLocalFileSystem;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.ProjectServiceException;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.Status;
import org.icgc.dcc.core.model.UserSession;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.filesystem.SubmissionDirectory;
import org.icgc.dcc.release.NextRelease;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.io.CharStreams;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.typesafe.config.Config;

@RunWith(MockitoJUnitRunner.class)
@Slf4j
public class SftpServerServiceTest {

  /**
   * Test configuration.
   */
  private static final String RELEASE_NAME = "release1";
  private static final String PROJECT_KEY = "project1";
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";
  private static final int NIO_WORKERS = 3;

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();
  @Rule
  public Sftp sftp = new Sftp(USERNAME, PASSWORD, false);

  @Mock
  Config config;
  @Mock
  Subject subject;
  @Mock
  UsernamePasswordAuthenticator passwordAuthenticator;

  @Mock
  Release release;
  @Mock
  Submission submission;
  @Mock
  Project project;
  @Mock
  NextRelease nextRelease;

  @Mock
  DccFileSystem fs;
  @Mock
  SubmissionDirectory submissionDirectory;
  @Mock
  ReleaseFileSystem releaseFileSystem;

  @Mock
  ProjectService projectService;
  @Mock
  ReleaseService releaseService;

  SftpServerService service;
  File root;
  List<Project> projects;

  @Before
  public void setUp() throws IOException, JSchException {
    root = tmp.newFolder(RELEASE_NAME);
    projects = newArrayList(project);

    // Mock configuration
    when(config.getInt("sftp.port")).thenReturn(sftp.getPort());
    when(config.getString("sftp.path")).thenReturn("");
    when(config.hasPath("sftp.nio-workers")).thenReturn(true);
    when(config.getInt("sftp.nio-workers")).thenReturn(NIO_WORKERS);

    // Mock authentication
    when(passwordAuthenticator.authenticate(anyString(), (char[]) any(), anyString())).thenReturn(subject);
    when(passwordAuthenticator.getSubject()).thenReturn(subject);

    // Mock release / project
    when(project.getKey()).thenReturn(PROJECT_KEY);
    when(release.getName()).thenReturn(RELEASE_NAME);
    when(nextRelease.getRelease()).thenReturn(release);
    when(releaseService.getNextRelease()).thenReturn(nextRelease);
    when(projectService.getProject(PROJECT_KEY)).thenReturn(project);
    when(projectService.getProject(not(eq(PROJECT_KEY)))).thenThrow(
        new ProjectServiceException("No project found with key"));
    when(projectService.getProjectsBySubject(any(Subject.class))).thenReturn(projects);

    // Mock file system
    when(fs.buildReleaseStringPath(release)).thenReturn(root.getAbsolutePath());
    when(fs.getReleaseFilesystem(release, subject)).thenReturn(releaseFileSystem);
    when(fs.getFileSystem()).thenReturn(fileSystem());
    when(releaseFileSystem.getDccFileSystem()).thenReturn(fs);
    when(releaseFileSystem.getRelease()).thenReturn(release);
    when(releaseFileSystem.getSubmissionDirectory(PROJECT_KEY)).thenReturn(submissionDirectory);
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
    String projectDirectoryName = createProjectDirectory();

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

  @Test(expected = IOException.class)
  public void testGetNotPossible() throws SftpException, IOException {
    // Create the simulated project directory
    String projectDirectoryName = createProjectDirectory();

    // File
    String fileName = fileName(1);
    String fileContent = "This is the content of the file";
    File file = new File(root, fileName);

    // Put file
    sftp.getChannel().cd(projectDirectoryName);
    sftp.getChannel().put(inputStream(fileContent), fileName);
    assertThat(file).exists().hasContent(fileContent);

    // This should throw on read
    read(sftp.getChannel().get(fileName));
  }

  @Test(expected = SftpException.class)
  public void testRemoveNonExistentFile() throws SftpException {
    sftp.getChannel().rm("/does/not/exist");
  }

  @Test(expected = SftpException.class)
  public void testCdIntoNonExistent() throws SftpException {
    sftp.getChannel().cd("/does/not/exist");
  }

  @Test(expected = SftpException.class)
  public void testLsIntoNonExistent() throws SftpException {
    sftp.getChannel().ls("/does/not/exist");
  }

  @Test(expected = SftpException.class)
  public void testRenameNonExistent() throws SftpException {
    sftp.getChannel().rename("/does/not/exist", "/still/does/not/exist");
  }

  @Test(expected = SftpException.class)
  public void testGetNonExistent() throws SftpException {
    sftp.getChannel().get("/does/not/exist");
  }

  @Test
  public void testActiveSession() throws InterruptedException {
    // Connected
    checkActiveSessions(1);

    // Disconnect
    disconnectAndCheck();
  }

  // re-enable in DCC-1226
  @Ignore
  @Test
  public void testMaxSession() throws InterruptedException {
    int extraClientCount = NIO_WORKERS + 1; // one is already connected

    ExecutorService executor = newFixedThreadPool(extraClientCount);
    for (int i = 0; i < extraClientCount; i++) {
      final int thread = i + 1;
      executor.execute(new Runnable() {

        @Override
        public void run() {
          Sftp sftpTmp = new Sftp(USERNAME, PASSWORD, false);
          try {
            log.info("Connecting - {}", thread);
            sftpTmp.connect();
            log.info("Connected - {}", thread);

            String projectDirectoryName = createProjectDirectory(); // Somehow it seems we can't write at the root...
            log.info(lsString(sftpTmp, projectDirectoryName));

            log.info("Writting - {}", thread);
            sftpTmp.getChannel().put(new ByteArrayInputStream(new byte[250000000]), fileName("dummy" + thread));

            log.info("Written - {}", thread);
            log.info(lsString(sftpTmp, projectDirectoryName));

            log.info("Disconnecting - {}", thread);
            sftpTmp.disconnect();
            log.info("Disconnected - {}", thread);

          } catch (Exception e) {
            e.printStackTrace();
            Assert.assertFalse(true);
          }
        }

        @SuppressWarnings("unchecked")
        private String lsString(Sftp sftpTmp, String projectDirectoryName) throws SftpException {
          return new ArrayList<String>(sftpTmp.getChannel().ls(projectDirectoryName)).toString();
        }

      });
      Thread.sleep(1000);
    }
    Thread.sleep(extraClientCount * 1500);

    checkActiveSessions(extraClientCount + 1); // One is already connected

    Thread.sleep(extraClientCount * 15000);
    disconnectAndCheck();
  }

  private void disconnectAndCheck() throws InterruptedException {
    sftp.disconnect();
    Thread.sleep(1000); // Allow for asynchronous disconnection latency
    checkActiveSessions(0);
  }

  private void checkActiveSessions(int total) {
    Status activeSessions = service.getActiveSessions();
    assertThat(activeSessions.getActiveSftpSessions()).isEqualTo(total);
    List<UserSession> userSessions = activeSessions.getUserSessions();
    assertThat(userSessions.size()).isEqualTo(total);
    for (int i = 0; i < total; i++) {
      assertThat(userSessions.get(i).getUserName()).isEqualTo(USERNAME);
    }
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

  private String createProjectDirectory() {
    String projectDirectoryName = getProjectDirectoryName();
    File projectDirectory = new File(root, projectDirectoryName);
    projectDirectory.mkdir();

    return projectDirectoryName;
  }

  private String getProjectDirectoryName() {
    return "/" + PROJECT_KEY;
  }

  private static String fileName(int i) {
    return fileName(String.valueOf(i));
  }

  private static String fileName(String s) {
    return format("/%s/file%s.txt", PROJECT_KEY, s);
  }

  private static String read(InputStream inputStream) throws IOException {
    return CharStreams.toString(new InputStreamReader(inputStream, UTF_8));
  }

  private static InputStream inputStream(String text) {
    return new ByteArrayInputStream(text.getBytes(UTF_8));
  }

}
