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

import static com.google.common.collect.ImmutableList.copyOf;
import static org.icgc.dcc.submission.core.model.QProject.project;

import java.util.List;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.core.model.QProject;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.Morphia;

import com.google.inject.Inject;

public class ProjectRepository extends AbstractRepository<Project, QProject> {

  @Inject
  public ProjectRepository(@NonNull Morphia morphia, @NonNull Datastore datastore) {
    super(morphia, datastore, project);
  }

  public Project findProject(@NonNull String projectKey) {
    return uniqueResult(entity.key.eq(projectKey));
  }

  public Project findProjectByUser(@NonNull String projectKey, @NonNull String username) {
    return singleResult(entity.key.eq(projectKey).and(entity.users.contains(username)));
  }

  public List<Project> findProjects() {
    return list();
  }

  public List<Project> findProjects(@NonNull Iterable<String> projectKeys) {
    return list(entity.key.in(copyOf(projectKeys)));
  }

  public List<Project> findProjectsByUser(@NonNull String username) {
    return list(entity.users.contains(username));
  }

  /**
   * Upsert will either insert or update the Project depending on whether it already exists in the database. Existence
   * check is based on whether the Project already has an Id. If the Project is missing an Id and already exists, Mongo
   * with throw {@link com.mongodb.MongoException.DuplicateKey}. If the Project has an Id and already exists it will be
   * updated. Otherwise it will be added.
   * 
   * @return Response object from Mongo
   */
  public Key<Project> upsertProject(@NonNull Project project) {
    val response = save(project);

    return response;
  }

}
