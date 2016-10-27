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

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.util.concurrent.Service.State.TERMINATED;
import static java.lang.String.valueOf;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.submission.server.sftp.SftpSessions.NO_FILE_TRANSFER;
import static org.icgc.dcc.submission.server.sftp.SftpSessions.getAuthentication;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.io.IoSession;
import org.apache.sshd.common.session.AbstractSession;
import org.icgc.dcc.submission.core.model.Status;
import org.icgc.dcc.submission.core.model.UserSession;
import org.icgc.dcc.submission.core.security.Authorizations;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.AbstractService;

/**
 * Service abstraction to the SFTP sub-system.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
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
    val status = new Status(enabled, state());
    if (state() == TERMINATED) {
      return status;
    }

    val activeSessions = sshd.getActiveSessions();
    log.debug("Active sessions: {}", activeSessions);
    for (val activeSession : activeSessions) {

      // Shorthands
      val username = activeSession.getUsername();
      if (isNullOrEmpty(username)) {
        log.debug("Skipping pending authentication session.");
        continue;
      }

      val ioSessionMap = getIoSessionMap(activeSession);
      log.info(getLogMessage(username),
          new Object[] { username, ioSessionMap });

      status.addUserSession(new UserSession(username, ioSessionMap));
    }

    return status;
  }

  public Collection<String> getFileTransfers() {
    val sessions = sshd.getActiveSessions();

    return sessions.stream()
        .map(SftpSessions::getFileTransfer)
        .filter(SftpServerService::hasFileTransfer)
        .map(transfer -> transfer.get().getPath())
        .collect(toImmutableList());
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
  private Map<String, String> getIoSessionMap(AbstractSession session) {
    val ioSession = session.getIoSession();
    val map = Maps.<String, String> newLinkedHashMap();
    map.put("id", valueOf(ioSession.getId()));
    map.put("localAddress", ioSession.getLocalAddress().toString());
    map.put("remoteAddress", ioSession.getRemoteAddress().toString());

    val transfer = SftpSessions.getFileTransfer(session);
    if (hasFileTransfer(transfer)) {
      map.put("fileTransfer", transfer.get().getPath());
    }

    return map;
  }

  private static boolean hasFileTransfer(Optional<FileTransfer> transfer) {
    return transfer.isPresent() && !NO_FILE_TRANSFER.equals(transfer.get());
  }

  private static boolean isSuperUser(AbstractSession activeSession) {
    val authentication = getAuthentication(activeSession);
    return Authorizations.isSuperUser(authentication);
  }

}
