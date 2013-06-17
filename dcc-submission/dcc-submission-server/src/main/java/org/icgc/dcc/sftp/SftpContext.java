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

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import lombok.val;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.filesystem.SubmissionDirectory;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;

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
public class SftpContext {

  /**
   * Encapsulated context.
   */
  private final DccFileSystem fs;
  private final ReleaseService releaseService;
  private final ProjectService projectService;
  private final UsernamePasswordAuthenticator authenticator;

  @Inject
  public SftpContext(DccFileSystem fs, ReleaseService releaseService, ProjectService projectService,
      UsernamePasswordAuthenticator authenticator) {
    super();
    this.fs = fs;
    this.releaseService = releaseService;
    this.projectService = projectService;
    this.authenticator = authenticator;
  }

  public boolean authenticate(String username, String password) {
    return authenticator.authenticate(username, password.toCharArray(), null) != null;
  }

  public List<String> getUserProjectKeys() {
    List<String> projectKeys = newArrayList();
    for (val project : projectService.getProjectsBySubject(getCurrentUser())) {
      projectKeys.add(project.getKey());
    }

    return projectKeys;
  }

  // TODO: This should not be needed once the other todos are addressed
  public Release getNextRelease() {
    return releaseService.getNextRelease().getRelease();
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

  // TODO: Return Paths or Strings and nothing in org.dcc.filesystem.*
  public SubmissionDirectory getSubmissionDirectory(String projectKey) {
    return getReleaseFileSystem().getSubmissionDirectory(projectKey);
  }

  public Path getReleasePath() {
    String releasePath = fs.buildReleaseStringPath(getNextRelease());
    return new Path(releasePath);
  }

  // TODO: Accept Paths or Strings and nothing in org.dcc.filesystem.*
  public void resetSubmission(Submission submission) {
    releaseService.resetSubmission(getNextReleaseName(), submission.getProjectKey());
  }

  public void resetSubmissions() {
    for (Submission submission : getNextRelease().getSubmissions()) {
      // TODO: DCC-903 (only if open release uses it)
      resetSubmission(submission);
    }
  }

  public boolean isSystemDirectory(Path path) {
    return getReleaseFileSystem().isSystemDirectory(path);
  }

  private Subject getCurrentUser() {
    return authenticator.getSubject();
  }

}
