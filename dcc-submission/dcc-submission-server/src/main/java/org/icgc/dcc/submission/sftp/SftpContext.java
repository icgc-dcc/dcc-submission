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

import static org.icgc.dcc.submission.shiro.AuthorizationPrivileges.projectViewPrivilege;

import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.ReleaseFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.security.UsernamePasswordAuthenticator;
import org.icgc.dcc.submission.service.MailService;
import org.icgc.dcc.submission.service.ProjectService;
import org.icgc.dcc.submission.service.ReleaseService;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

/**
 * "Encapsulated Context Object" class that insulates and decouples the SFTP subsystem from DCC file system
 * abstractions. This is very similar in purpose to Hadoop's new API "Context Object for Mapper and Reducer".
 * <p>
 * Note that there still remain some accessors that should be removed as they violate the encapsulation. In this sense,
 * this should be considered a "Parameter Object" transitioning to a "Encapsulated Context Object".
 * 
 * @see http://www.two-sdg.demon.co.uk/curbralan/papers/europlop/ContextEncapsulation.pdf
 * @see http://www.allankelly.net/static/patterns/encapsulatecontext.pdf
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @_(@Inject))
public class SftpContext {

  /**
   * Encapsulated context.
   */
  @NonNull
  private final DccFileSystem fs;
  @NonNull
  private final ReleaseService releaseService;
  @NonNull
  private final ProjectService projectService;
  @NonNull
  private final UsernamePasswordAuthenticator authenticator;
  @NonNull
  private final MailService mailService;

  public boolean authenticate(String username, String password) {
    return authenticator.authenticate(username, password.toCharArray(), null) != null;
  }

  public List<String> getUserProjectKeys() {
    val user = getCurrentUser();
    val projectKeys = Lists.<String> newArrayList();
    for (val project : projectService.getProjects()) {
      if (user.isPermitted(projectViewPrivilege(project.getKey()))) {
        projectKeys.add(project.getKey());
      }
    }

    return projectKeys;
  }

  // TODO: This should not be needed once the other todos are addressed
  public Release getNextRelease() {
    return releaseService.getNextRelease();
  }

  public String getNextReleaseName() {
    return getNextRelease().getName();
  }

  // TODO: Return Paths or Strings and nothing in org.dcc.filesystem.*
  public ReleaseFileSystem getReleaseFileSystem() {
    return fs.getReleaseFilesystem(getNextRelease(), getCurrentUser());
  }

  public FileSystem getFileSystem() {
    return fs.getFileSystem();
  }

  public boolean isSystemDirectory(Path path) {
    return getReleaseFileSystem().isSystemDirectory(path);
  }

  public boolean isAdminUser() {
    return getReleaseFileSystem().isAdminUser();
  }

  // TODO: Return Paths or Strings and nothing in org.dcc.filesystem.*
  public SubmissionDirectory getSubmissionDirectory(String projectKey) {
    return getReleaseFileSystem().getSubmissionDirectory(projectKey);
  }

  public Path getReleasePath() {
    String releasePath = fs.buildReleaseStringPath(getNextRelease().getName());
    return new Path(releasePath);
  }

  // TODO: Accept Paths or Strings and nothing in org.dcc.filesystem.*
  public void notifySubmissionChange(@NonNull Submission submission, @NonNull Optional<Path> path) {
    log.info("Resetting submission '{}'...", submission.getProjectKey());
    releaseService.resetSubmission(getNextReleaseName(), submission.getProjectKey(), path);
  }

  public void notifySystemChange() {
    for (Submission submission : getNextRelease().getSubmissions()) {
      // TODO: DCC-903 (only if open release uses it)
      notifySubmissionChange(submission, Optional.<Path> absent());
    }
  }

  public void notifyFileTransferred(Path path) {
    val user = (String) getCurrentUser().getPrincipal();
    log.info("'{}' finished transferring file '{}'", user, path);
    mailService.sendFileTransferred(user, path.toUri().toString());
  }

  public void notifyFileRemoved(Path path) {
    val user = (String) getCurrentUser().getPrincipal();
    log.info("'{}' removed  file '{}'", user, path);
    mailService.sendFileRemoved(user, path.toUri().toString());
  }

  private Subject getCurrentUser() {
    return authenticator.getSubject();
  }

}
