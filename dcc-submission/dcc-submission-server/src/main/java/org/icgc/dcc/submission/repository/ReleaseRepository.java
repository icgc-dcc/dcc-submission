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
import static com.google.common.collect.ImmutableList.copyOf;
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
import com.google.code.morphia.query.UpdateOperations;
import com.google.code.morphia.query.UpdateResults;
import com.google.inject.Inject;

public class ReleaseRepository extends BaseMorphiaService<Release> {

  private final QRelease _ = QRelease.release;

  @Inject
  public ReleaseRepository(Morphia morphia, Datastore datastore, MailService mailService) {
    super(morphia, datastore, QRelease.release, mailService);
    super.registerModelClasses(Release.class);
  }

  public long countOpenReleases() {
    return where(_.state.eq(OPENED)).count();
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
    return copyOf(where(_.state.eq(COMPLETED)).list());
  }

  public List<Release> findReleases() {
    return copyOf(query().list());
  }

  /**
   * Do *not* use to update an existing release (not intended that way).
   */
  public void saveNewRelease(@NonNull Release newRelease) {
    datastore().save(newRelease);
  }

  public boolean updateRelease(String newReleaseName, String newDictionaryVersion, Release release,
      String oldReleaseName) {
    val releaseUpdate = datastore().update(
        datastore().createQuery(Release.class)
            .filter("name = ", oldReleaseName),
        datastore().createUpdateOperations(Release.class)
            .set("name", newReleaseName)
            .set("dictionaryVersion", newDictionaryVersion)
            .set("queue", release.getQueue()));

    val success = releaseUpdate.getUpdatedCount() != 1;
    return success;
  }

  public UpdateResults<Release> updateRelease(String originalReleaseName, Release updatedRelease) {
    val update = datastore().updateFirst(
        datastore().createQuery(Release.class)
            .filter("name = ", originalReleaseName),
        updatedRelease, false);

    return update;
  }

  public void updateCompletedRelease(@NonNull Release oldRelease) {
    datastore().findAndModify(
        datastore().createQuery(Release.class)
            .filter("name", oldRelease.getName()),
        datastore().createUpdateOperations(Release.class)
            .set("state", oldRelease.getState())
            .set("releaseDate", oldRelease.getReleaseDate())
            .set("submissions", oldRelease.getSubmissions()));
  }

  public void updateReleaseQueue(String currentReleaseName, List<QueuedProject> queue) {
    datastore().update(
        datastore().createQuery(Release.class)
            .filter("name = ", currentReleaseName),
        datastore().createUpdateOperations(Release.class)
            .set("queue", queue));
  }

  public void updateSubmission(String releaseName, Submission submission) {
    val query = datastore().createQuery(Release.class);
    val result = datastore().update(
        query
            .filter("name = ", releaseName)
            .filter("submissions.projectKey = ", submission.getProjectKey()),
        updateOperations()
            .set("submissions.$", submission));

    checkState(result.getUpdatedCount() == 1,
        "Updated more than one release when updating release '%s' with submission '%s'",
        releaseName, submission);
  }

  public void updateSubmissionState(@NonNull String releaseName, @NonNull SubmissionState state,
      @NonNull String projectKey) {
    datastore().update(
        datastore().createQuery(Release.class)
            .filter("name = ", releaseName)
            .filter("submissions.projectKey = ", projectKey),
        updateOperations()
            .set("submissions.$.state", state));
  }

  /**
   * This is currently necessary in order to use the <i>field.$.nestedField</i> notation in updates. Otherwise one gets
   * an error like <i>
   * "The field '$' could not be found in 'org.icgc.dcc.submission.release.model.Release' while validating - submissions.$.state; if you wish to continue please disable validation."
   * </i>
   * <p>
   * For more information, see
   * http://groups.google.com/group/morphia/tree/browse_frm/month/2011-01/489d5b7501760724?rnum
   * =31&_done=/group/morphia/browse_frm/month/2011-01?
   */
  private UpdateOperations<Release> updateOperations() {
    return datastore().createUpdateOperations(Release.class).disableValidation();
  }

}