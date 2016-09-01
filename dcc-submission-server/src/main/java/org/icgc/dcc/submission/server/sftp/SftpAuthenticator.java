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

import static org.icgc.dcc.submission.core.security.Authorizations.isSuperUser;
import static org.icgc.dcc.submission.server.sftp.SftpSessions.setAuthentication;

import java.io.IOException;

import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.google.common.eventbus.Subscribe;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * MINA abstraction for implementing SFTP authentication.
 * <p>
 * Delegates to Shiro based implementation by way of the {@link SftpContext}.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SftpAuthenticator implements PasswordAuthenticator {

  /**
   * Disconnect message sent to clients when disabled.
   */
  private static final String DISABLED_MESSAGE =
      "The ICGC DCC Submission SFTP Server is disabled for scheduled maintenance. Please login and try again later.";

  /**
   * Authenticator state.
   */
  @NonNull
  private final AuthenticationManager authenticator;
  @NonNull
  private final SftpContext context;

  private volatile boolean enabled = true;

  @Override
  public boolean authenticate(String username, String password, ServerSession session) {
    val authentication = authenticate(username, password);

    val authenticated = authentication.isAuthenticated();
    if (authenticated) {
      // Add principal to MINA SFTP session
      if (!isAccessible(authentication)) {
        // Only allow new connection when enabled
        log.info("Blocked connection for user '{}' because SFTP is disabled", username);
        disconnect(session);

        return false;
      }

      setAuthentication(session, authentication);

      // Send an informative message
      sendBanner(username, session, authentication);
    }

    return authenticated;
  }

  /**
   * Event fired from {@link SftpServerService}.
   * 
   * @param event
   */
  @Subscribe
  public void onEvent(SftpChangeEvent event) {
    log.info("Received SFTP event: {}", event);

    // Synchronize state
    this.enabled = event.isEnabled();
  }

  private boolean isDisabled() {
    return !this.enabled;
  }

  private void disconnect(ServerSession session) {
    log.info("Sending disconnect message '{}' to {}", DISABLED_MESSAGE, session.getUsername());
    try {
      session.disconnect(0, DISABLED_MESSAGE);
    } catch (IOException e) {
      log.error("Exception sending disconnect message: {}", e);
    }
  }

  private Authentication authenticate(String username, String password) {
    // Delegate to Spring
    val authentication = new UsernamePasswordAuthenticationToken(username, password);
    return authenticator.authenticate(authentication);
  }

  private void sendBanner(String username, ServerSession session, Authentication authentication) {
    // Send a custom welcome message
    val banner = new SftpBanner(context, authentication);
    banner.send(username, session);
  }

  private boolean isAccessible(Authentication authentication) {
    return !isDisabled() || isSuperUser(authentication);
  }

}