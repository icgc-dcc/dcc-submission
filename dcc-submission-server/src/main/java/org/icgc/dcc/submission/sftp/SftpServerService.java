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

import static com.google.common.util.concurrent.Service.State.TERMINATED;
import static java.lang.String.valueOf;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.session.AbstractSession;
import org.icgc.dcc.submission.core.model.Status;
import org.icgc.dcc.submission.core.model.UserSession;
import org.icgc.dcc.submission.shiro.AuthorizationPrivileges;

import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Service abstraction to the SFTP sub-system.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject) )
public class SftpServerService extends AbstractService {

  /**
   * Message sent to active session when disabling SFTP.
   */
  private static final String DISABLE_MESSAGE =
      "The ICGC DCC Submission SFTP Server is shutting down for scheduled maintenance. Please login and try again later.";

  /**
   * Service state.
   */
  @NonNull
  private final SshServer sshd;
  @NonNull
  private final EventBus eventBus;
  private volatile boolean enabled = true;

  public Status getActiveSessions() {
    Status status = new Status(enabled, state());
    if (state() == TERMINATED) {
      return status;
    }

    List<AbstractSession> activeSessions = sshd.getActiveSessions();
    for (AbstractSession activeSession : activeSessions) {

      // Shorthands
      val ioSession = activeSession.getIoSession();
      String username = activeSession.getUsername();

      Map<String, String> ioSessionMap = getIoSessionMap(ioSession);
      log.info(getLogMessage(username),
          new Object[] { username, ioSessionMap });

      status.addUserSession(new UserSession(username, ioSessionMap));
    }

    return status;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void enable() {
    this.enabled = true;
    notifyChange(this.enabled);
  }

  public void disable() {
    disconnectActiveSessions();

    this.enabled = false;
    notifyChange(this.enabled);
  }

  @Override
  protected void doStart() {
    try {
      log.info("Starting DCC SSH Server on port {}", sshd.getPort());
      sshd.start();
      notifyStarted();
    } catch (IOException e) {
      log.error("Failed to start SFTP server on {}:{} : {}",
          new Object[] { sshd.getHost(), sshd.getPort(), e.getMessage() });
      notifyFailed(e);
    }
  }

  @Override
  protected void doStop() {
    try {
      sshd.stop(true);
      notifyStopped();
    } catch (InterruptedException e) {
      log.error("Failed to stop SFTP server on {}:{} : {}",
          new Object[] { sshd.getHost(), sshd.getPort(), e.getMessage() });
      notifyFailed(e);
    }
  }

  private void notifyChange(boolean enabled) {
    SftpChangeEvent event = new SftpChangeEvent(enabled);

    log.info("Sending SFTP event: {}...", event);
    eventBus.post(event);
    log.info("SFTP event sent");
  }

  private void disconnectActiveSessions() {
    val activeSessions = sshd.getActiveSessions();

    for (val activeSession : activeSessions) {
      if (isSuperUser(activeSession)) {
        // Don't disconnect super users
        continue;
      }

      disconnectSession(activeSession, DISABLE_MESSAGE);
    }
  }

  private void disconnectSession(AbstractSession session, String message) {
    log.info("Sending disconnect message '{}' to {}", message, session.getUsername());
    try {
      session.disconnect(0, message);
    } catch (IOException e) {
      log.error("Exception sending disconnect message: {}", e);
    }
  }

  private String getLogMessage(String username) {
    String intro =
        username == null ? "Authentication pending ('{}' username) " : "User with username '{}' has an active ";

    return intro + "SFTP session created on '{}', last written to '{}'; full ioSession is: {}";
  }

  /**
   * Returns some of the useful values for an {@link IoSession}.
   */
  private Map<String, String> getIoSessionMap(IoSession ioSession) {
    val map = Maps.<String, String> newLinkedHashMap();
    map.put("id", valueOf(ioSession.getId()));
    map.put("localAddress", ioSession.getLocalAddress().toString());
    map.put("remoteAddress", ioSession.getRemoteAddress().toString());

    return map;
  }

  private static boolean isSuperUser(AbstractSession activeSession) {
    val subject = SftpSessions.getSessionSubject(activeSession);
    return subject.isPermitted(AuthorizationPrivileges.ALL.getPrefix());
  }

}
