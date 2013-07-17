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

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;

/**
 * MINA abstraction for implementing SFTP authentication.
 * <p>
 * Delegates to Shiro based implementation by way of the {@link SftpContext}.
 */
@Slf4j
class SftpAuthenticator implements PasswordAuthenticator {

  /**
   * Disconnect message sent to clients when disabled.
   */
  private static final String DISABLED_MESSAGE =
      "The ICGC DCC Submission SFTP Server is disabled for scheduled maintenance. Please login and try again later.";

  /**
   * Authenticator state.
   */
  private final SftpContext context;
  private final SftpBanner banner;
  private volatile boolean enabled = true;

  @Inject
  public SftpAuthenticator(SftpContext context) {
    this.context = context;
    this.banner = new SftpBanner(context);
  }

  @Override
  public boolean authenticate(String username, String password, ServerSession session) {
    if (isDisabled()) {
      // Only allow new connection when enabled
      log.info("Blocked connection for user '{}' because SFTP is disabled", username);
      disconnect(session);

      return false;
    }

    boolean authenticated = authenticate(username, password);
    if (authenticated) {
      sendBanner(username, session);
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

  private boolean authenticate(String username, String password) {
    // Delegate to Shiro
    return context.authenticate(username, password);
  }

  private void sendBanner(String username, ServerSession session) {
    // Send a custom welcome message
    banner.send(username, session);
  }

}