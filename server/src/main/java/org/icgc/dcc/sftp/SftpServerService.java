/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.Factory;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.Session;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.FileSystemFactory;
import org.apache.sshd.server.FileSystemView;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.typesafe.config.Config;

/**
 * 
 */
public class SftpServerService extends AbstractService {

  private static final Logger log = LoggerFactory.getLogger(SftpServerService.class);

  private final SshServer sshd;

  @Inject
  public SftpServerService(Config config, Integer port) {

    sshd = SshServer.setUpDefaultServer();
    sshd.setPort(port);
    sshd.setKeyPairProvider(new PEMGeneratorHostKeyProvider(System.getProperty("HOME") + "/conf/sshd.pem", "RSA", 2048));
    sshd.setShellFactory(new Factory<Command>() {

      @Override
      public Command create() {

        return new ShellCommand();
      }

    });
    sshd.setPasswordAuthenticator(new PasswordAuthenticator() {

      @Override
      public boolean authenticate(String username, String password, ServerSession session) {
        try {
          SecurityUtils.getSubject().login(
              new UsernamePasswordToken(username, password.toCharArray(), session.getIoSession().getRemoteAddress()
                  .toString()));
          // Sessions don't expire automatically
          SecurityUtils.getSubject().getSession().setTimeout(-1);
        } catch(AuthenticationException ae) {
          return false;
        }
        return SecurityUtils.getSubject().isAuthenticated();
      }
    });

    sshd.setFileSystemFactory(new FileSystemFactory() {
      @Override
      public FileSystemView createFileSystemView(Session session) throws IOException {
        // TODO Hook in to DCC filesystem classes via HdfsFileSystemView
        return null;
      }
    });
    sshd.setSubsystemFactories(ImmutableList.<NamedFactory<Command>> of(new SftpSubsystem.Factory()));
  }

  @Override
  protected void doStart() {
    try {
      log.info("Starting DCC SSH Server on port {}", sshd.getPort());
      sshd.start();
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void doStop() {
    try {
      sshd.stop(true);
    } catch(InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private class ShellCommand implements Command {

    private Thread thread;

    private ExitCallback exitCallback;

    private InputStream in;

    private OutputStream out;

    private OutputStream err;

    @Override
    @SuppressWarnings("deprecation")
    public void destroy() {
      thread.stop();
    }

    @Override
    public void setErrorStream(OutputStream err) {
      this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
      exitCallback = callback;
    }

    @Override
    public void setInputStream(InputStream in) {
      this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
      this.out = out;
    }

    @Override
    public void start(Environment env) throws IOException {
      thread = new Thread();// new Thread(shell);
      thread.setDaemon(true);
      thread.start();
    }
  }
}
