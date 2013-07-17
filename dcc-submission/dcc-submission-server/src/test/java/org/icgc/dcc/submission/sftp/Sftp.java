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

import lombok.extern.slf4j.Slf4j;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

@Slf4j
public class Sftp implements TestRule {

  private static final String SFTP_HOST = "127.0.0.1";
  private static final int SFTP_PORT = 5322;

  private final JSch jsch = new JSch();
  private Session session;
  private ChannelSftp sftpChannel;

  private final String username;
  private final String password;

  public Sftp(String username, String password, boolean logging) {
    this.username = username;
    this.password = password;
    if (logging) {
      JSch.setLogger(new SftpLogger());
    }
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        try {
          base.evaluate();
        } finally {
          disconnect();
        }
      }

    };
  }

  public int getPort() {
    return SFTP_PORT;
  }

  public String getHost() {
    return SFTP_HOST;
  }

  public ChannelSftp getChannel() {
    return sftpChannel;
  }

  public void connect() throws JSchException {
    if (sftpChannel != null && sftpChannel.isConnected()) {
      return;
    }

    session = jsch.getSession(username, SFTP_HOST, SFTP_PORT);
    session.setConfig("StrictHostKeyChecking", "no");
    session.setPassword(password);
    session.connect();

    sftpChannel = (ChannelSftp) session.openChannel("sftp");
    sftpChannel.setInputStream(System.in);
    sftpChannel.setOutputStream(System.out);
    sftpChannel.connect();
  }

  public void disconnect() {
    if (session == null) {
      return;
    }

    if (session.isConnected() == false) {
      return;
    }

    sftpChannel.exit();
    session.disconnect();
  }

  public static class SftpLogger implements com.jcraft.jsch.Logger {

    @Override
    public boolean isEnabled(int level) {
      return true;
    }

    @Override
    public void log(int level, String message) {
      log.debug(message);
    }

  }

}