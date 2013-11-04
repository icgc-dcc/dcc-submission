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

import java.util.Set;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.core.model.QProject;
import org.icgc.dcc.submission.core.morphia.BaseMorphiaService;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Key;
import com.google.code.morphia.Morphia;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.mongodb.WriteConcern;

/**
 * 
 */
@Slf4j
public class ProjectRepository extends BaseMorphiaService<Project> {

  @Inject
  public ProjectRepository(Morphia morphia, Datastore datastore, MailService mailService) {
    super(morphia, datastore, QProject.project, mailService);
    super.registerModelClasses(Project.class);
  }

  /**
   * Search for a Project by its key
   * 
   * @param projectKey
   * 
   * @return Project or null if none found
   * 
   * @see #findForUser(String, String)
   */
  public Project find(String projectKey) {
    log.info("Finding Project {}", projectKey);
    return where(QProject.project.key.eq(projectKey)).singleResult();
  }

  /**
   * Search for a Project by its key and whether it is accessible to the user
   * 
   * @param projectKey
   * 
   * @param username
   * 
   * @return Project or null if none found
   * 
   * @see #find(String)
   */
  public Project findForUser(String projectKey, String username) {
    log.info("Finding Project {} for User {}", projectKey, username);
    return where(QProject.project.key.eq(projectKey)).where(QProject.project.users.contains(username)).singleResult();
  }

  /**
   * Search for all Projects
   * 
   * @return All Projects
   * 
   * @see #findAllForUser(String)
   */
  public Set<Project> findAll() {
    log.info("Finding all Projects");
    return ImmutableSet.copyOf(query().list());
  }

  /**
   * Search for all Projects accessible to the user
   * 
   * @param username
   * 
   * @return All Projects viewable by the user
   * 
   * @see #findAll()
   */
  public Set<Project> findAllForUser(String username) {
    log.info("Finding all Projects for {}", username);
    return ImmutableSet.copyOf(where(QProject.project.users.contains(username)).list());
  }

  /**
   * Upsert will either insert or update the Project depending on whether it already exists in the database. Existence
   * check is based on whether the Project already has an Id. <br>
   * <br>
   * If the Project is missing an Id and already exists, Mongo with throw
   * {@link com.mongodb.MongoException.DuplicateKey}. <br>
   * If the Project has an Id and already exists it will be updated. <br>
   * Otherwise it will be added.
   * 
   * @param project Project used in upsert
   * 
   * @return Response object from Mongo
   */
  public Key<Project> upsert(Project project) {
    log.info("Upserting {}", project);
    val response = this.datastore().save(project, WriteConcern.ACKNOWLEDGED);

    log.info("Upsert Successful! {}", response);
    return response;
  }
}
