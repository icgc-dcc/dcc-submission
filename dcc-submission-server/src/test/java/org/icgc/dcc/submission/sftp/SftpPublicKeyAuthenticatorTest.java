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
package org.icgc.dcc.submission.sftp;

import static com.google.common.base.Charsets.UTF_8;
import static com.jcraft.jsch.KeyPair.RSA;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import lombok.SneakyThrows;
import lombok.val;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.security.UsernamePasswordAuthenticator;
import org.icgc.dcc.submission.service.MailService;
import org.icgc.dcc.submission.service.ProjectService;
import org.icgc.dcc.submission.service.ReleaseService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;
import com.typesafe.config.Config;

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

  @Mock
  Config config;
  @Mock
  UsernamePasswordAuthenticator authenticator;
  @Mock
  SftpAuthenticator sftpAuthenticator;
  @Mock
  DccFileSystem fs;
  @Mock
  ProjectService projectService;
  @Mock
  ReleaseService releaseService;
  @Mock
  MailService mailService;
  @Mock
  Subject subject;

  @Before
  public void setUp() throws IOException, JSchException {
    // Mock configuration
    when(config.getInt("sftp.port")).thenReturn(5322);
    when(config.getString("sftp.path")).thenReturn(tmp.newFile().getAbsolutePath());
  }

  @After
  public void tearDown() {
    ThreadContext.remove();
  }

  @Test
  @SneakyThrows
  public void testPublicKey() {
    // Simulate the behavior of SecurityManagerProvider
    DefaultSecurityManager defaultSecurityManager = new DefaultSecurityManager();
    SecurityUtils.setSecurityManager(defaultSecurityManager);

    // Setup public and private keys for test
    val keyStore = tmp.newFolder();
    val keyName = "sftp";
    val privateKey = new File(keyStore, keyName);
    val publicKey = new File(keyStore, keyName + ".pub");

    // Create SFTP client
    JSch jsch = new JSch();
    createKeyPair(jsch, privateKey, publicKey);
    jsch.addIdentity(privateKey.getAbsolutePath());

    // Enable public key in application
    when(config.hasPath("sftp.key")).thenReturn(true);
    when(config.getString("sftp.key")).thenReturn(getPublicKeyValue(publicKey));

    // Create class under test
    SftpServerService service = createService();
    service.startAsync().awaitRunning();

    // Connect to server
    val session = jsch.getSession(USERNAME, SFTP_HOST, SFTP_PORT);
    session.setConfig("StrictHostKeyChecking", "no");
    session.connect();

    val sftpChannel = session.openChannel("sftp");
    sftpChannel.connect();

    service.stopAsync().awaitTerminated();
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
    val sshd = new SshServerProvider(config, context, sftpAuthenticator).get();
    val eventBus = new EventBus();
    eventBus.register(authenticator);

    return new SftpServerService(sshd, eventBus);
  }

}
