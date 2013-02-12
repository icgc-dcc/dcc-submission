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

import java.io.ByteArrayInputStream;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SftpRule implements TestRule {

  private static final String SFTP_HOST = "127.0.0.1";

  private static final int SFTP_PORT = 5322;

  private final JSch jsch = new JSch();

  private Session session;

  private ChannelSftp sftpChannel;

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

  public void connect() throws JSchException {
    if(sftpChannel != null && sftpChannel.isConnected()) {
      return;
    }

    session = jsch.getSession("username", SFTP_HOST, SFTP_PORT);
    session.setConfig("StrictHostKeyChecking", "no");
    session.setPassword("password");
    session.connect();

    sftpChannel = (ChannelSftp) session.openChannel("sftp");
    sftpChannel.connect();
  }

  public void disconnect() {
    if(session == null) {
      return;
    }

    if(session.isConnected() == false) {
      return;
    }

    sftpChannel.exit();
    session.disconnect();
  }

  public void put(String content, String fileName) throws SftpException {
    // Execute
    sftpChannel.put(new ByteArrayInputStream(content.getBytes(UTF_8)), fileName);
  }

}