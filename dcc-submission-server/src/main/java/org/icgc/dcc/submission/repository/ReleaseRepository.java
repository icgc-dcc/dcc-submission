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
import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static org.icgc.dcc.common.core.model.Identifiable.Identifiables.getId;
import static org.icgc.dcc.submission.release.model.QRelease.release;
import static org.icgc.dcc.submission.release.model.ReleaseState.COMPLETED;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;

import java.util.List;
import java.util.Set;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.common.core.model.Identifiable;
import org.icgc.dcc.submission.release.model.QRelease;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.google.inject.Inject;

public class ReleaseRepository extends AbstractRepository<Release, QRelease> {

  @Inject
  public ReleaseRepository(@NonNull Morphia morphia, @NonNull Datastore datastore) {
    super(morphia, datastore, release);
  }

  public Release findOpenRelease() {
    return singleResult(_.state.eq(OPENED));
  }

  public long countReleases() {
    return count();
  }

  public long countOpenReleases() {
    return count(_.state.eq(OPENED));
  }

  public List<Release> findReleases() {
    return list();
  }

  public List<Release> findReleaseSummaries() {
    return list(_.name, _.dictionaryVersion, _.releaseDate, _.state, _.submissions.any().projectKey);
  }

  public Release findNextRelease() {
    return singleResult(_.state.eq(OPENED));
  }

  public Release findNextReleaseQueue() {
    return singleResult(_.state.eq(OPENED), _.queue);
  }

  public String findNextReleaseDictionaryVersion() {
    return singleResult(_.state.eq(OPENED), _.dictionaryVersion).getDictionaryVersion();
  }

  public Release findReleaseByName(@NonNull String releaseName) {
    return uniqueResult(_.name.eq(releaseName));
  }

  public String findDictionaryVersion(@NonNull String releaseName) {
    return findReleaseByName(releaseName).getDictionaryVersion();
  }

  /**
   * Returns the {@link Submission} ID as implemented from {@link Identifiable}.
   */
  public Set<String> findSubmissionIds(@NonNull String releaseName) {
    return copyOf(transform(
        findReleaseByName(releaseName).getSubmissions(),
        getId()));
  }

  public Set<String> findProjectKeys(@NonNull String releaseName) {
    return Submission.getProjectKeys(findSubmissionIds(releaseName));
  }

  public Set<String> findSignedOffProjectKeys(@NonNull String releaseName) {
    return copyOf(transform(
        filter(
            findReleaseByName(releaseName).getSubmissions(),
            Submission.isSignedOff()),
        Submission.getProjectKeyFunction()));
  }

  public Release findReleaseSummaryByName(@NonNull String releaseName) {
    return uniqueResult(_.name.eq(releaseName),
        _.name, _.dictionaryVersion, _.releaseDate, _.state,
        _.submissions.any().projectKey, _.submissions.any().projectName, _.submissions.any().state,
        _.submissions.any().report.dataTypeReports.any().dataType,
        _.submissions.any().report.dataTypeReports.any().dataTypeState);
  }

  public Release findCompletedRelease(@NonNull String releaseName) {
    return uniqueResult(_.state.eq(COMPLETED).and(_.name.eq(releaseName)));
  }

  public List<Release> findCompletedReleases() {
    return list(_.state.eq(COMPLETED));
  }

  public void saveNewRelease(@NonNull Release newRelease) {
    save(newRelease);
  }

  public boolean updateRelease(@NonNull String releaseName, @NonNull Release updatedRelease,
      @NonNull String updatedReleaseName, @NonNull String updatedDictionaryVersion) {
    val releaseUpdate = update(
        createQuery()
            .filter("name", releaseName),
        createUpdateOperations()
            .set("name", updatedReleaseName)
            .set("dictionaryVersion", updatedDictionaryVersion)
            .set("queue", updatedRelease.getQueue()));

    val success = releaseUpdate.getUpdatedCount() != 1;
    return success;
  }

  public void updateRelease(@NonNull String releaseName, @NonNull Release updatedRelease) {
    val result = updateFirst(
        createQuery()
            .filter("name", releaseName),
        updatedRelease,
        false);

    result.getUpdatedCount();

    checkState(!result.getHadError(), "Error updating release '%s': %s", releaseName, result.getError());
    checkState(result.getUpdatedCount() == 1, "Updating release '%s' failed: %s", releaseName, result.getWriteResult());
  }

  public Release updateCompletedRelease(@NonNull Release completedRelease) {
    return findAndModify(
        createQuery()
            .filter("name", completedRelease.getName()),
        createUpdateOperations()
            .set("state", completedRelease.getState())
            .set("releaseDate", completedRelease.getReleaseDate())
            .set("submissions", completedRelease.getSubmissions()));
  }

  public void updateReleaseQueue(@NonNull String releaseName, @NonNull List<QueuedProject> queue) {
    val result = update(
        createQuery()
            .filter("name", releaseName),
        createUpdateOperations()
            .set("queue", queue));

    checkState(result.getUpdatedCount() == 1,
        "Updated more than one release when updating release '%s' with queue '%s'",
        releaseName, queue);
  }

  public Release addReleaseSubmission(@NonNull String releaseName, @NonNull Submission submission) {
    return findAndModify(
        createQuery()
            .field("name").equal(releaseName),
        createUpdateOperations()
            .add("submissions", submission));
  }

  public void updateReleaseSubmission(@NonNull String releaseName, @NonNull Submission submission) {
    val result = update(
        createQuery()
            .filter("name", releaseName)
            .filter("submissions.projectKey", submission.getProjectKey()),
        createUpdateOperations$()
            .set("submissions.$", submission));

    checkState(result.getUpdatedCount() == 1,
        "Updated more than one release when updating release '%s' with submission '%s'",
        releaseName, submission);
  }

  public void updateReleaseSubmissionState(@NonNull String releaseName, @NonNull String projectKey,
      @NonNull SubmissionState state) {
    val result = update(
        createQuery()
            .filter("name", releaseName)
            .filter("submissions.projectKey", projectKey),
        createUpdateOperations$()
            .set("submissions.$.state", state));

    checkState(result.getUpdatedCount() == 1,
        "Updated more than one release submission when updating release '%s' project '%s' state to '%s'",
        releaseName, projectKey, state);
  }

}