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
import static com.google.common.io.Resources.getResource;
import static lombok.AccessLevel.PACKAGE;

import java.io.IOException;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.session.ServerSession;

import com.google.common.io.Resources;

/**
 * Helper class for generating SFTP user customized authentication banner message.
 */
@Slf4j
@AllArgsConstructor(access = PACKAGE)
class SftpBanner {

  /**
   * EOF for SSH_MSG_USERAUTH_BANNER
   */
  private static final String BANNER_EOF = "\n\n";

  /**
   * External banner text file.
   * @see http://patorjk.com/software/taag/#p=display&f=Slant&t=ICGC%20DCC%20-%20Submission
   */
  private static final String BANNER_FILE = "banner.txt";

  /**
   * Support email address.
   */
  // TODO: Externalize
  private static final String SUPPORT_EMAIL = "***REMOVED***";

  /**
   * Standard operating procedure for DCC submissions.
   */
  private static final String SOP_URL = "http://dcc.icgc.org/pages/docs/dcc_submission_system_sop-beta.pdf";

  /**
   * External banner text.
   */
  private final String BANNER = getBanner();

  @NonNull
  private final SftpContext context;

  public void send(String username, ServerSession session) {
    try {
      // General information
      String releaseName = context.getNextReleaseName();

      // User specific information
      List<String> projectKeys = context.getUserProjectKeys();

      // Create a customized message for the supplied user
      String message = getMessage(releaseName, username, projectKeys);

      write(session, message);
    } catch (IOException e) {
      log.warn("Error sending SFTP connection welcome banner: ", e);
    }
  }

  private void write(ServerSession session, String message) throws IOException {
    // Create message buffer
    Buffer buffer = session.createBuffer(SshConstants.Message.SSH_MSG_USERAUTH_BANNER, 0);
    buffer.putString(message);
    buffer.putString(BANNER_EOF);

    session.writePacket(buffer);
  }

  @SneakyThrows
  private String getMessage(String releaseName, String username, List<String> projectKeys) {
    String message = "\n" +
        BANNER + "\n\n" +
        "Hello '" + username + "', welcome to the ICGC DCC Submission SFTP Server!\n" +
        "\n" +
        "  - Accepting submissions for release: '" + releaseName + "'\n" +
        "  - Downloading is disabled (ex. 'get', 'mget')\n" +
        "  - You may 'cd', 'put' 'rm' 'ls' in the following project directories:\n" +
        formatDirectories(projectKeys) +
        "  - Submission instructions may be found at: '" + SOP_URL + "'\n" +
        "  - For support contact: '" + SUPPORT_EMAIL + "'\n" +
        "\n";

    return message;
  }

  private String formatDirectories(List<String> projectKeys) {
    String directories = "";
    for (String projectKey : projectKeys) {
      directories += "    * '" + projectKey + "/'\n";
    }

    return directories;
  }

  @SneakyThrows
  private static String getBanner() {
    return Resources.toString(getResource(BANNER_FILE), UTF_8);
  }

}
