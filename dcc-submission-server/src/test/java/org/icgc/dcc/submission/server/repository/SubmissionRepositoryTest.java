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

import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;

import org.icgc.dcc.submission.release.model.QSubmission;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.google.common.collect.ImmutableList;
import com.mongodb.MongoClientURI;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

public class SubmissionRepositoryTest extends AbstractRepositoryTest {

  private MorphiaQuery<Submission> morphiaQuery;
  private Datastore datastore;
  private SubmissionRepository submissionRepository;

  @Before
  public void setUp() throws Exception {
    val morphia = new Morphia();
    datastore = morphia.createDatastore(embeddedMongo.getMongo(), new MongoClientURI(getMongoUri()).getDatabase());
    val submission1 = new Submission("P1", "Proj1", "R1");
    val submission2 = new Submission("P2", "Proj2", "R1");
    val submission3 = new Submission("P3", "Proj3", "R2");
    datastore.save(submission1, submission2, submission3);
    morphiaQuery = new MorphiaQuery<Submission>(morphia, datastore, QSubmission.submission);
    submissionRepository = new SubmissionRepository(morphia, datastore);
  }

  @Test
  public void testFindSubmission() throws Exception {
    assertThat(submissionRepository.findSubmission("R1", "P1")).isNotNull();
    assertThat(submissionRepository.findSubmission("fake", "P1")).isNull();
  }

  @Test
  public void testFindSubmissionsByReleaseAndProject() throws Exception {
    assertThat(submissionRepository.findSubmissions("R1", ImmutableList.of("P1"))).hasSize(1);
    assertThat(submissionRepository.findSubmissions("R1", ImmutableList.of("P1", "P2"))).hasSize(2);
    assertThat(submissionRepository.findSubmissions("R2", ImmutableList.of("P3", "P2"))).hasSize(1);
    assertThat(submissionRepository.findSubmissions("R2", ImmutableList.of("P2"))).hasSize(0);
  }

  @Test
  public void testFindSubmissionsByRelease() throws Exception {
    val submissions = submissionRepository.findSubmissions("R1");
    assertThat(submissions).hasSize(2);
    assertThat(submissions.get(0).getProjectKey()).isEqualTo("P1");
    assertThat(submissions.get(1).getProjectKey()).isEqualTo("P2");
  }

  @Test
  public void testFindAllSubmissions() throws Exception {
    assertThat(submissionRepository.findSubmissions()).hasSize(3);
  }

  @Test
  public void testDelete() throws Exception {
    assertThat(submissionRepository.deleteByReleaseAndNotState("R1", SubmissionState.NOT_VALIDATED)).isEqualTo(0);
    assertThat(morphiaQuery.count()).isEqualTo(3);

    assertThat(submissionRepository.deleteByReleaseAndNotState("R1", SubmissionState.ERROR)).isEqualTo(2);
    assertThat(morphiaQuery.count()).isEqualTo(1);
  }

}
