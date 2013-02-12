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

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

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

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Rule
  public SftpRule sftp = new SftpRule();

  @Mock
  Subject subject;

  @Mock
  Release release;

  @Mock
  Project project;

  @Mock
  SubmissionDirectory submissionDirectory;

  @Mock
  NextRelease nextRelease;

  @Mock
  ReleaseFileSystem releaseFileSystem;

  @Mock
  Config config;

  @Mock
  UsernamePasswordAuthenticator passwordAuthenticator;

  @Mock
  DccFileSystem fs;

  @Mock
  ProjectService projectService;

  @Mock
  ReleaseService releaseService;

  SftpServerService service;

  File root;

  @Before
  public void setUp() throws IOException, JSchException {
    root = tmp.newFolder();

    // Mock configuration
    when(config.getInt("sftp.port")).thenReturn(sftp.getPort());
    when(config.getString("sftp.path")).thenReturn("");

    // Mock authentication
    when(passwordAuthenticator.authenticate(anyString(), (char[]) any(), anyString())).thenReturn(subject);
    when(passwordAuthenticator.getSubject()).thenReturn(subject);

    // Mock release / project
    when(nextRelease.getRelease()).thenReturn(release);
    when(releaseService.getNextRelease()).thenReturn(nextRelease);
    when(projectService.getProject(anyString())).thenReturn(project);

    // Mock file system
    when(submissionDirectory.isReadOnly()).thenReturn(false);
    when(fs.buildReleaseStringPath(any(Release.class))).thenReturn(root.getAbsolutePath());
    when(fs.getReleaseFilesystem(any(Release.class), any(Subject.class))).thenReturn(releaseFileSystem);
    when(fs.getFileSystem()).thenReturn(createFileSystem());
    when(releaseFileSystem.getDccFileSystem()).thenReturn(fs);
    when(releaseFileSystem.getSubmissionDirectory(project)).thenReturn(submissionDirectory);

    // Create an start CUT
    service = new SftpServerService(config, passwordAuthenticator, fs, projectService, releaseService);
    service.startAndWait();

    sftp.connect();
  }

  @Test
  public void testService() throws JSchException, SftpException {
    String content = "test";
    String fileName = "file.txt";

    sftp.put(content, fileName);

    assertThat(new File(root, fileName)).exists();
  }

  @After
  public void tearDown() {
    service.stop();
  }

  private static RawLocalFileSystem createFileSystem() {
    RawLocalFileSystem localFileSystem = new RawLocalFileSystem();
    localFileSystem.setConf(new Configuration());

    return localFileSystem;
  }

}
