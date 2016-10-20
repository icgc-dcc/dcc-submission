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
package org.icgc.dcc.submission.server.repository;

import static org.icgc.dcc.submission.release.model.QSubmission.submission;

import java.util.Collection;
import java.util.List;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.submission.release.model.QSubmission;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.springframework.beans.factory.annotation.Autowired;

import com.mysema.query.mongodb.MongodbQuery;

public class SubmissionRepository extends AbstractRepository<Submission, QSubmission> {

  @Autowired
  public SubmissionRepository(Morphia morphia, Datastore datastore) {
    super(morphia, datastore, submission);
  }

  public void addSubmissions(@NonNull Iterable<Submission> submissions) {
    save(submissions);
  }

  public void addSubmission(@NonNull Submission submission) {
    save(submission);
  }

  public void updateExistingSubmissions(@NonNull Iterable<Submission> submissions) {
    for (val submission : submissions) {
      updateFirst(
          createFilterByIdQuery(submission),
          submission,
          false);
    }
  }

  public int updateSubmission(@NonNull Submission submission) {
    val result = updateFirst(createFilterByIdQuery(submission), submission, false);

    return result.getUpdatedCount();
  }

  public List<Submission> findSubmissionStateByReleaseName(@NonNull String releaseName) {
    return where(entity.releaseName.eq(releaseName))
        .list(entity.state);
  }

  public Submission findSubmissionByReleaseNameAndProjectKey(@NonNull String releaseName, @NonNull String projectKey) {
    return createFilterByReleaseNameQuery(releaseName)
        .where(entity.projectKey.eq(projectKey)).singleResult();
  }

  public Submission findSubmissionSummaryByReleaseNameAndProjectKey(@NonNull String releaseName,
      @NonNull String projectKey) {
    return createFilterByReleaseNameQuery(releaseName)
        .where(entity.projectKey.eq(projectKey))
        .singleResult(entity.releaseName, entity.projectKey, entity.state, entity.lastUpdated);
  }

  public List<Submission> findSubmissionsByReleaseNameAndProjectKey(@NonNull String releaseName,
      @NonNull Collection<String> projectKeys) {
    return createFilterByReleaseNameQuery(releaseName)
        .where(entity.projectKey.in(projectKeys))
        .list();
  }

  public List<Submission> findSubmissionSummariesByReleaseNameAndState(@NonNull String releaseName,
      @NonNull SubmissionState state) {
    return createFilterByReleaseNameQuery(releaseName)
        .where(entity.state.eq(state))
        .list(entity.releaseName, entity.projectKey, entity.state, entity.lastUpdated);
  }

  public List<Submission> findSubmissionsByReleaseName(@NonNull String releaseName) {
    return createFilterByReleaseNameQuery(releaseName).list();
  }

  /**
   * Find submissions without report and projectName by {@code releaseName}.
   */
  public List<Submission> findSubmissionSummariesByReleaseName(@NonNull String releaseName) {
    return createFilterByReleaseNameQuery(releaseName)
        .list(entity.releaseName, entity.projectKey, entity.state, entity.lastUpdated);
  }

  public List<Submission> findSubmissions() {
    return list();
  }

  public List<Submission> findSubmissionsByProjectKey(@NonNull String projectKey) {
    return query()
        .where(entity.projectKey.eq(projectKey))
        .list();
  }

  public int deleteByReleaseAndNotState(@NonNull String releaseName, @NonNull SubmissionState state) {
    val result = delete(createQuery()
        .filter("releaseName == ", releaseName)
        .filter("state !=", state));

    return result.getN();
  }

  private Query<Submission> createFilterByIdQuery(Submission submission) {
    return createQuery()
        .filter("id", submission.getId());
  }

  private MongodbQuery<Submission> createFilterByReleaseNameQuery(String releaseName) {
    return query().where(entity.releaseName.eq(releaseName));
  }

}
