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

import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;

class SftpPasswordAuthenticator implements PasswordAuthenticator {

  /**
   * Disconnect message sent to clients when disabled.
   */
  private static final String DISABLED_MESSAGE =
      "The ICGC DCC Submission SFTP Server is disabled for scheduled maintenance. Please login and try again later.";

  /**
   * Authenticator state.
   */
  private final SftpServerService service;
  private final UsernamePasswordAuthenticator delegate;
  private final SftpBanner banner;

  SftpPasswordAuthenticator(SftpServerService service, UsernamePasswordAuthenticator delegate,
      ProjectService projectService, ReleaseService releaseService) {
    this.service = service;
    this.delegate = delegate;
    this.banner = new SftpBanner(releaseService, projectService);
  }

  @Override
  public boolean authenticate(String username, String password, ServerSession session) {
    if (isDisabled()) {
      // Only allow connections when enabled
      disconnect(session);

      return false;
    }

    boolean authenticated = authenticate(username, password);
    if (authenticated) {
      sendBanner(session);
    }

    return authenticated;
  }

  private boolean isDisabled() {
    return !service.isEnabled();
  }

  private void disconnect(ServerSession session) {
    service.disconnectSession(session, DISABLED_MESSAGE);
  }

  private boolean authenticate(String username, String password) {
    return delegate.authenticate(username, password.toCharArray(), null) != null;
  }

  private void sendBanner(ServerSession session) {
    banner.send(session, delegate.getSubject());
  }

}