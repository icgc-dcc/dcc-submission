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
import org.icgc.dcc.submission.core.morphia.BaseMorphiaService;
import org.icgc.dcc.submission.release.model.QRelease;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.Submission;

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
public class ReleaseRepository extends BaseMorphiaService<Release> {

  static final QRelease schema = QRelease.release;

  @Inject
  public ReleaseRepository(Morphia morphia, Datastore datastore, MailService mailService) {
    super(morphia, datastore, schema, mailService);
    super.registerModelClasses(Release.class);
  }

  /**
   * Search for all {@code Release}s
   * 
   * @return All Releases
   */
  public Set<Release> findAll() {
    log.info("Finding all Releases");
    return ImmutableSet.copyOf(query().list());
  }

  /**
   * Search for {@code Release} by name
   * 
   * @return Release
   */
  public Release find(String releaseName) {
    log.info("Finding Release {}", releaseName);
    return where(schema.name.eq(releaseName)).singleResult();
  }

  /**
   * Query for {@code Release} with state {@code OPENED}
   * 
   * @return Current Open Release
   */
  public Release findOpen() {
    log.info("Finding Current Open Release");
    return where(schema.state.eq(ReleaseState.OPENED)).singleResult();
  }

  /**
   * Adds {@code Submission} to the current open {@code Release} <br>
   * <br>
   * This method should be used instead of {@link #update(Release)} since it does not overwrite the Release object in
   * the DB.
   * 
   * @param submission
   * 
   * @return Current Open Release
   */
  public Release addSubmission(Submission submission, String releaseName) {
    log.info("Adding Submission for Project {} to Release {}", submission.getProjectKey(), releaseName);
    val q = datastore().createQuery(Release.class).field("name").equal(releaseName);
    val ops =
        datastore().createUpdateOperations(Release.class).add("submissions", submission);
    val modifiedRelease = this.datastore().findAndModify(q, ops);

    log.info("Submission {} added!", submission.getProjectKey());
    return modifiedRelease;
  }

  /**
   * Updates Release with new Release object. <br>
   * <br>
   * This will overwrite any changes that might have happened between initially getting the release and updating.
   * 
   * @param release Release
   * 
   * @return Response object from Mongo
   */
  public Key<Release> update(Release release) {
    log.info("Updating Release {}", release.getName());

    val response = datastore().save(release, WriteConcern.ACKNOWLEDGED);

    return response;
  }
}
