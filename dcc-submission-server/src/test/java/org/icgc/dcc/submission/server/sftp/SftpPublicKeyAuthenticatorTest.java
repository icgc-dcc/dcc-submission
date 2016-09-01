/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.server.sftp;

import static com.google.common.base.Charsets.UTF_8;
import static com.jcraft.jsch.KeyPair.RSA;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.icgc.dcc.submission.fs.SubmissionFileSystem;
import org.icgc.dcc.submission.server.service.MailService;
import org.icgc.dcc.submission.server.service.ProjectService;
import org.icgc.dcc.submission.server.service.ReleaseService;
import org.icgc.dcc.submission.server.sftp.SftpAuthenticator;
import org.icgc.dcc.submission.server.sftp.SftpContext;
import org.icgc.dcc.submission.server.sftp.SftpServerService;
import org.icgc.dcc.submission.server.sftp.SshServerProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import lombok.SneakyThrows;
import lombok.val;

@RunWith(MockitoJUnitRunner.class)
public class SftpPublicKeyAuthenticatorTest {

  /**
   * Test configuration.
   */
  private static final String USERNAME = "username";
  private static final String SFTP_HOST = "127.0.0.1";
  private static final int SFTP_PORT = 5322;

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  SubmissionProperties properties = new SubmissionProperties();

  @Mock
  AuthenticationManager authenticator;
  @Mock
  SftpAuthenticator sftpAuthenticator;
  @Mock
  SubmissionFileSystem fs;
  @Mock
  ProjectService projectService;
  @Mock
  ReleaseService releaseService;
  @Mock
  MailService mailService;

  @Before
  public void setUp() throws IOException, JSchException {
    properties.getSftp().setPort(5322);
    properties.getSftp().setPath(tmp.newFile().getAbsolutePath());
  }

  @Test
  @SneakyThrows
  public void testPublicKey() {
    // Mock authentication
    val authentication = new UsernamePasswordAuthenticationToken(USERNAME, "", null);
    when(authenticator.authenticate(any())).thenReturn(authentication);

    // Setup public and private keys for test
    val keyStore = tmp.newFolder();
    val keyName = "sftp";
    val privateKey = new File(keyStore, keyName);
    val publicKey = new File(keyStore, keyName + ".pub");

    // Create SFTP client
    val jsch = new JSch();
    createKeyPair(jsch, privateKey, publicKey);
    jsch.addIdentity(privateKey.getAbsolutePath());

    // Enable public key in application
    properties.getSftp().setKey(getPublicKeyValue(publicKey));

    // Create class under test
    val service = createService();
    try {
      service.startAsync().awaitRunning();

      // Connect to server
      val session = jsch.getSession(USERNAME, SFTP_HOST, SFTP_PORT);
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();

      val sftpChannel = session.openChannel("sftp");
      sftpChannel.connect();
    } finally {
      service.stopAsync().awaitTerminated();
    }
  }

  @SneakyThrows
  private void createKeyPair(JSch jsch, File privateKey, File publicKey) {
    val keyPair = KeyPair.genKeyPair(jsch, RSA);
    keyPair.writePrivateKey(privateKey.getAbsolutePath());
    keyPair.writePublicKey(publicKey.getAbsolutePath(), "");
  }

  private String getPublicKeyValue(File publicKey) throws IOException {
    // Remove algorithm
    return Files.toString(publicKey, UTF_8).replace("ssh-rsa ", "");
  }

  private SftpServerService createService() {
    val context = new SftpContext(fs, releaseService, projectService, authenticator, mailService);
    val sshd = new SshServerProvider(properties, context, sftpAuthenticator).get();
    val eventBus = new EventBus();
    eventBus.register(authenticator);

    return new SftpServerService(sshd, eventBus);
  }

}
