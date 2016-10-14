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

import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.submission.release.model.QRelease.release;
import static org.icgc.dcc.submission.release.model.ReleaseState.COMPLETED;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;

import java.util.List;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.submission.release.model.QRelease;
import org.icgc.dcc.submission.release.model.Release;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.springframework.beans.factory.annotation.Autowired;

public class ReleaseRepository extends AbstractRepository<Release, QRelease> {

  @Autowired
  public ReleaseRepository(@NonNull Morphia morphia, @NonNull Datastore datastore) {
    super(morphia, datastore, release);
  }

  public Release findOpenRelease() {
    return singleResult(entity.state.eq(OPENED));
  }

  public long countReleases() {
    return count();
  }

  public long countOpenReleases() {
    return count(entity.state.eq(OPENED));
  }

  public List<Release> findReleases() {
    return list();
  }

  public List<Release> findReleaseSummaries() {
    return list(entity.name, entity.dictionaryVersion, entity.releaseDate, entity.state);
  }

  public Release findNextRelease() {
    return singleResult(entity.state.eq(OPENED));
  }

  public Release findNextReleaseQueue() {
    return singleResult(entity.state.eq(OPENED), entity.queue);
  }

  public String findNextReleaseDictionaryVersion() {
    return singleResult(entity.state.eq(OPENED), entity.dictionaryVersion).getDictionaryVersion();
  }

  public Release findReleaseByName(@NonNull String releaseName) {
    return uniqueResult(entity.name.eq(releaseName));
  }

  public Release findReleaseSummaryByName(@NonNull String releaseName) {
    return uniqueResult(entity.name.eq(releaseName), entity.name, entity.dictionaryVersion, entity.releaseDate,
        entity.state);
  }

  public Release findCompletedRelease(@NonNull String releaseName) {
    return uniqueResult(entity.state.eq(COMPLETED).and(entity.name.eq(releaseName)));
  }

  public List<Release> findCompletedReleases() {
    return list(entity.state.eq(COMPLETED));
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

    checkState(!result.getHadError(), "Error updating release '%s': %s", releaseName, result.getError());
    checkState(result.getUpdatedCount() == 1, "Updating release '%s' failed: %s", releaseName, result.getWriteResult());
  }

  public Release updateCompletedRelease(@NonNull Release completedRelease) {
    return findAndModify(
        createQuery()
            .filter("name", completedRelease.getName()),
        createUpdateOperations()
            .set("state", completedRelease.getState())
            .set("releaseDate", completedRelease.getReleaseDate()));
  }

}