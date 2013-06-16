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

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.util.concurrent.Service.State.TERMINATED;
import static java.lang.String.valueOf;
import static org.apache.sshd.common.FactoryManager.DEFAULT_NIO_WORKERS;
import static org.apache.sshd.common.FactoryManager.NIO_WORKERS;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.mina.core.session.IoSession;
import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.session.AbstractSession;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.model.Status;
import org.icgc.dcc.core.model.UserSession;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;
import org.icgc.dcc.sftp.fs.HdfsFileSystemFactory;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.typesafe.config.Config;

/**
 * Service abstraction to the SFTP sub-system.
 */
@Slf4j
public class SftpServerService extends AbstractService {

  /**
   * Message sent to active session when disabling SFTP.
   */
  private static final String DISABLE_MESSAGE =
      "The ICGC DCC Submission SFTP Server is shutting down for scheduled maintenance. Please login and try again later.";

  /**
   * Service state.
   */
  private final SshServer sshd;
  private volatile boolean enabled = true;

  @Inject
  public SftpServerService(Config config, UsernamePasswordAuthenticator passwordAuthenticator, DccFileSystem fs,
      ProjectService projectService, ReleaseService releaseService) {
    this.sshd = SshServerFactory.create(
        checkNotNull(config),
        this,
        checkNotNull(passwordAuthenticator),
        checkNotNull(fs),
        checkNotNull(projectService),
        checkNotNull(releaseService));
  }

  public Status getActiveSessions() {
    Status status = new Status(enabled, state());
    if (state() == TERMINATED) {
      return status;
    }

    List<AbstractSession> activeSessions = sshd.getActiveSessions();
    for (AbstractSession activeSession : activeSessions) {

      // Shorthands
      IoSession ioSession = activeSession.getIoSession();
      long creationTime = ioSession.getCreationTime();
      long lastWriteTime = ioSession.getLastWriteTime();
      String username = activeSession.getUsername();

      Map<String, String> ioSessionMap = getIoSessionMap(ioSession);
      log.info(getLogMessage(username),
          new Object[] { username, formatDateTime(creationTime), formatDateTime(lastWriteTime), ioSessionMap });

      status.addUserSession(new UserSession(username, creationTime, lastWriteTime, ioSessionMap));
    }

    return status;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void enable() {
    this.enabled = true;
  }

  public void disable() {
    disconnectActiveSessions();
    this.enabled = false;
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

  void disconnectActiveSessions() {
    List<AbstractSession> activeSessions = sshd.getActiveSessions();

    for (AbstractSession activeSession : activeSessions) {
      disconnectSession(activeSession, DISABLE_MESSAGE);
    }
  }

  void disconnectSession(AbstractSession session, String message) {
    log.info("Sending disconnect message '{}' to {}", message, session.getUsername());
    try {
      session.disconnect(0, message);
    } catch (IOException e) {
      log.error("Exception sending disconnect message: {}", e);
    }
  }

  private String getLogMessage(String username) {
    String intro = username == null ?
        "Authentication pending ('{}' username) " :
        "User with username '{}' has an active ";

    return intro + "SFTP session created on '{}', last written to '{}'; full ioSession is: {}";
  }

  /**
   * Returns some of the useful values for an {@link IoSession}.
   */
  private Map<String, String> getIoSessionMap(IoSession ioSession) {
    Map<String, String> map = newLinkedHashMap();

    map.put("id", valueOf(ioSession.getId()));
    map.put("creationTime", formatDateTime(ioSession.getCreationTime()));

    map.put("readerIdleCount", valueOf(ioSession.getReaderIdleCount()));
    map.put("writerIdleCount", valueOf(ioSession.getWriterIdleCount()));
    map.put("bothIdleCount", valueOf(ioSession.getBothIdleCount()));

    map.put("lastIoTime", formatDateTime(ioSession.getLastIoTime()));
    map.put("lastBothIdleTime", formatDateTime(ioSession.getLastBothIdleTime()));

    map.put("lastReadTime", formatDateTime(ioSession.getLastReadTime()));
    map.put("lastReaderIdleTime", formatDateTime(ioSession.getLastReaderIdleTime()));

    map.put("lastWriteTime", formatDateTime(ioSession.getLastWriteTime()));
    map.put("lastWriterIdleTime", formatDateTime(ioSession.getLastWriterIdleTime()));

    map.put("readBytes", valueOf(ioSession.getReadBytes()));
    map.put("readBytesThroughput", valueOf(ioSession.getReadBytesThroughput()));

    map.put("scheduledWriteBytes", valueOf(ioSession.getScheduledWriteBytes()));
    map.put("scheduledWriteMessages", valueOf(ioSession.getScheduledWriteMessages()));

    map.put("writtenBytes", valueOf(ioSession.getWrittenBytes()));
    map.put("writtenBytesThroughput", valueOf(ioSession.getWrittenBytesThroughput()));

    map.put("writtenMessages", valueOf(ioSession.getWrittenMessages()));
    map.put("writtenMessagesThroughput", valueOf(ioSession.getWrittenMessagesThroughput()));

    return map;
  }

  private static String formatDateTime(long timestamp) {
    return new DateTime(timestamp).toString();
  }

  @Slf4j
  static class SshServerFactory {

    /**
     * Configuration file path.
     */
    private static final String SFTP_CONFIG_SECTION = "sftp";

    static SshServer create(
        Config config,
        SftpServerService service,
        UsernamePasswordAuthenticator passwordAuthenticator,
        DccFileSystem fs,
        ProjectService projectService,
        ReleaseService releaseService) {

      // Create default server
      SshServer sshd = SshServer.setUpDefaultServer();

      // Set customized configuration
      sshd.setPort(config.getInt(getConfigPath("port")));
      setProperties(sshd, config);

      // Set customized extension points
      SftpContext context = new SftpContext(fs, releaseService, projectService, passwordAuthenticator);
      sshd.setKeyPairProvider(new PEMGeneratorHostKeyProvider(config.getString(getConfigPath("path")), "RSA", 2048));
      sshd.setFileSystemFactory(new HdfsFileSystemFactory(context));
      sshd.setSubsystemFactories(ImmutableList.<NamedFactory<Command>> of(new SftpSubsystem.Factory()));
      sshd.setPasswordAuthenticator(new SftpPasswordAuthenticator(service, context));

      return sshd;
    }

    private static void setProperties(SshServer sshd, Config config) {
      String nioWorkersPath = getConfigPath(NIO_WORKERS);

      if (config.hasPath(nioWorkersPath)) {
        Integer nioWorkers = config.getInt(nioWorkersPath);
        log.info("Setting '{}' to '{}'", NIO_WORKERS, nioWorkers);
        sshd.setProperties(new ImmutableMap.Builder<String, String>().put(NIO_WORKERS, valueOf(nioWorkers)).build());
      } else {
        log.info("Using default value for '{}': '{}'", NIO_WORKERS, DEFAULT_NIO_WORKERS);
      }
    }

    private static String getConfigPath(String param) {
      return on(".").join(SFTP_CONFIG_SECTION, param);
    }

  }

}
