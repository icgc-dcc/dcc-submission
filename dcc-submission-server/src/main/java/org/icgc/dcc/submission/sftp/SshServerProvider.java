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
package org.icgc.dcc.submission.sftp;

import static java.lang.String.valueOf;
import static org.apache.sshd.common.FactoryManager.DEFAULT_NIO_WORKERS;
import static org.apache.sshd.common.FactoryManager.NIO_WORKERS;

import javax.inject.Provider;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.icgc.dcc.submission.sftp.fs.HdfsFileSystemFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory class for encapsulating the "complex" logic of creating an {@link SshServer}.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SshServerProvider implements Provider<SshServer> {

  /**
   * Provider dependencies.
   */
  @NonNull
  private final SubmissionProperties properties;
  @NonNull
  private final SftpContext context;
  @NonNull
  private final SftpAuthenticator authenticator;

  @Override
  public SshServer get() {
    // Create default server
    SshServer sshd = SshServer.setUpDefaultServer();

    // Set customized configuration
    sshd.setPort(properties.getSftp().getPort());
    setProperties(sshd, properties);

    // Set customized extension points
    sshd.setKeyPairProvider(new PEMGeneratorHostKeyProvider(properties.getSftp().getPath(), "RSA", 2048));
    sshd.setPasswordAuthenticator(authenticator);
    if (properties.getSftp().getKey() != null) {
      sshd.setPublickeyAuthenticator(new SftpPublicKeyAuthenticator(properties.getSftp().getKey()));
    }
    sshd.setFileSystemFactory(new HdfsFileSystemFactory(context));
    sshd.setSubsystemFactories(ImmutableList.<NamedFactory<Command>> of(new SftpSubsystem.Factory()));

    return sshd;
  }

  private static void setProperties(SshServer sshd, SubmissionProperties properties) {
    val nioWorkers = properties.getSftp().getNioWorkers();
    if (nioWorkers != null) {
      log.info("Setting '{}' to '{}'", NIO_WORKERS, nioWorkers);
      sshd.setProperties(new ImmutableMap.Builder<String, String>().put(NIO_WORKERS, valueOf(nioWorkers)).build());
    } else {
      log.info("Using default value for '{}': '{}'", NIO_WORKERS, DEFAULT_NIO_WORKERS);
    }
  }

}
