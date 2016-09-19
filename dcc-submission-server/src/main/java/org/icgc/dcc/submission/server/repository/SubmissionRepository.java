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

import com.google.common.collect.ImmutableList;

public class SubmissionRepository extends AbstractRepository<Submission, QSubmission> {

  @Autowired
  public SubmissionRepository(Morphia morphia, Datastore datastore) {
    super(morphia, datastore, submission);
  }

  public void addSubmissions(@NonNull Submission... submissions) {
    addSubmissions(ImmutableList.copyOf(submissions));
  }

  public void addSubmissions(@NonNull Iterable<Submission> submissions) {
    for (val submission : submissions) {
      save(submission);
    }
  }

  public void updateSubmissions(@NonNull Iterable<Submission> submissions) {
    for (val submission : submissions) {
      updateFirst(
          createFilterByIdQuery(submission),
          submission,
          true);
    }
  }

  public int updateSubmission(@NonNull Submission submission) {
    val result = updateFirst(createFilterByIdQuery(submission), submission, false);

    return result.getUpdatedCount();
  }

  public int updateSubmissionState(@NonNull String releaseName, @NonNull String projectKey,
      @NonNull SubmissionState state) {
    val result = update(
        createQuery()
            .filter("releaseName", releaseName)
            .filter("projectKey", projectKey),
        createUpdateOperations$()
            .set("state", state));

    return result.getUpdatedCount();
  }

  public List<Submission> findSubmissionSummaryByRelease(@NonNull String releaseName) {
    return list(entity.releaseName.eq(releaseName), entity.projectKey, entity.projectName, entity.state,
        entity.report.dataTypeReports.any().dataType, entity.report.dataTypeReports.any().dataTypeState);
  }

  public List<Submission> findSubmissionProjectKeysByRelease(@NonNull String releaseName) {
    return list(entity.releaseName.eq(releaseName), entity.projectKey);
  }

  private Query<Submission> createFilterByIdQuery(Submission submission) {
    return createQuery()
        .filter("id", submission.getId());
  }

}
