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

package org.icgc.dcc.submission.repository;

import java.util.List;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.shiro.subject.Subject;
import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.core.model.QProject;
import org.icgc.dcc.submission.core.morphia.BaseMorphiaService;
import org.icgc.dcc.submission.shiro.AuthorizationPrivileges;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

@Slf4j
public class ProjectRepository extends BaseMorphiaService<Project> {

  @Inject
  public ProjectRepository(Morphia morphia, Datastore datastore, MailService mailService) {
    super(morphia, datastore, QProject.project, mailService);
    super.registerModelClasses(Project.class);
  }

  final private boolean isAuthorized(Subject user, Project project) {
    return project != null && user.isPermitted(AuthorizationPrivileges.projectViewPrivilege(project.key()));
  }

  final private void authorize(Subject user, Project project) {
    if (isAuthorized(user, project) == false) {
      throw new RuntimeException("No project found with key " + project.key());
    }
  }

  final private List<Project> filterUnauthorized(Subject user, List<Project> projects) {
    log.info("Filtering project based on user {}'s authorization", user.getPrincipal());
    ImmutableList.Builder<Project> builder = ImmutableList.builder();
    for (val project : projects) {
      if (isAuthorized(user, project)) {
        log.info("{} authorized to see {}", user, project.key());
        builder.add(project);
      }
    }
    return builder.build();
  }

  final public Project getProject(Subject user, String projectKey) {
    log.info("Getting Project {} for user {}", projectKey, user.getPrincipal());
    val project = this.query().where(QProject.project.key.eq(projectKey)).singleResult();

    authorize(user, project);

    return project;
  }

  public List<Project> getProjects(Subject user) {
    log.info("Getting all Projects for user {}", user.getPrincipal());
    return filterUnauthorized(user, this.query().list());
  }
}
