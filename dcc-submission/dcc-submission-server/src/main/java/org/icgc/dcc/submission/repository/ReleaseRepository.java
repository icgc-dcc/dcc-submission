/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.submission.release.model.ReleaseState.COMPLETED;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;

import java.util.List;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.core.morphia.BaseMorphiaService;
import org.icgc.dcc.submission.release.model.QRelease;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.UpdateResults;
import com.google.inject.Inject;

public class ReleaseRepository extends BaseMorphiaService<Release> {

  private static final QRelease _ = QRelease.release;

  @Inject
  public ReleaseRepository(Morphia morphia, Datastore datastore, MailService mailService) {
    super(morphia, datastore, QRelease.release, mailService);
    super.registerModelClasses(Release.class);
  }

  public Release findOpenRelease() {
    return where(_.state.eq(OPENED)).singleResult();
  }

  public long countOpenReleases() {
    return where(_.state.eq(OPENED)).count();
  }

  public List<Release> findReleases() {
    return list(query().list());
  }

  public Release findNextRelease() {
    return where(_.state.eq(OPENED)).singleResult();
  }

  public Release findReleaseByName(String releaseName) {
    return where(_.name.eq(releaseName)).uniqueResult();
  }

  public Release findCompletedRelease(String releaseName) {
    return where(_.state.eq(COMPLETED).and(_.name.eq(releaseName))).uniqueResult();
  }

  public List<Release> findCompletedReleases() {
    return list(where(_.state.eq(COMPLETED)).list());
  }

  public void saveNewRelease(@NonNull Release newRelease) {
    save(newRelease);
  }

  public boolean updateRelease(@NonNull String releaseName, @NonNull Release updatedRelease,
      @NonNull String updatedReleaseName, @NonNull String updatedDictionaryVersion) {
    val releaseUpdate = update(
        select()
            .filter("name = ", releaseName),
        updateOperations()
            .set("name", updatedReleaseName)
            .set("dictionaryVersion", updatedDictionaryVersion)
            .set("queue", updatedRelease.getQueue()));

    val success = releaseUpdate.getUpdatedCount() != 1;
    return success;
  }

  public UpdateResults<Release> updateRelease(@NonNull String releaseName, @NonNull Release updatedRelease) {
    return updateFirst(
        select()
            .filter("name = ", releaseName),
        updatedRelease, false);
  }

  public Release updateCompletedRelease(@NonNull Release completedRelease) {
    return findAndModify(
        select()
            .filter("name", completedRelease.getName()),
        updateOperations()
            .set("state", completedRelease.getState())
            .set("releaseDate", completedRelease.getReleaseDate())
            .set("submissions", completedRelease.getSubmissions()));
  }

  public void updateReleaseQueue(@NonNull String releaseName, @NonNull List<QueuedProject> queue) {
    val result = update(
        select()
            .filter("name = ", releaseName),
        updateOperations()
            .set("queue", queue));

    checkState(result.getUpdatedCount() == 1,
        "Updated more than one release when updating release '%s' with queue '%s'",
        releaseName, queue);
  }

  public Release addReleaseSubmission(@NonNull String releaseName, @NonNull Submission submission) {
    return findAndModify(
        select()
            .field("name").equal(releaseName),
        updateOperations()
            .add("submissions", submission));
  }

  public void updateReleaseSubmission(@NonNull String releaseName, @NonNull Submission submission) {
    val result = update(
        select()
            .filter("name = ", releaseName)
            .filter("submissions.projectKey = ", submission.getProjectKey()),
        updateOperations$()
            .set("submissions.$", submission));

    checkState(result.getUpdatedCount() == 1,
        "Updated more than one release when updating release '%s' with submission '%s'",
        releaseName, submission);
  }

  public void updateReleaseSubmissionState(@NonNull String releaseName, @NonNull String projectKey,
      @NonNull SubmissionState state) {
    val result = update(
        select()
            .filter("name = ", releaseName)
            .filter("submissions.projectKey = ", projectKey),
        updateOperations$()
            .set("submissions.$.state", state));

    checkState(result.getUpdatedCount() == 1,
        "Updated more than one release submission when updating release '%s' project '%s' state to '%s'",
        releaseName, projectKey, state);
  }

}