package org.icgc.dcc.submission.server.repository;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.common.collect.Lists.newArrayList;
import lombok.val;

import org.icgc.dcc.submission.release.model.QRelease;
import org.icgc.dcc.submission.release.model.QSubmission;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.Submission;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.mongodb.MongoClientURI;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

@RunWith(MockitoJUnitRunner.class)
public class ReleaseRepositoryTest extends AbstractRepositoryTest {

  private SubmissionRepository submissionRepository;
  private ReleaseRepository releaseRepository;

  private MorphiaQuery<Release> releaseMorphiaQuery;
  private MorphiaQuery<Submission> submissionMorphiaQuery;
  private Datastore datastore;
  private Release releaseOne;
  private Release releaseTwo;

  @Before
  public void setUp() throws Exception {
    val morphia = new Morphia();

    datastore = morphia.createDatastore(embeddedMongo.getMongo(), new MongoClientURI(getMongoUri()).getDatabase());
    val submission1 = new Submission("P1", "P1", "R1");
    val submission2 = new Submission("P2", "P2", "R1");
    datastore.save(submission1);
    datastore.save(submission2);

    releaseOne = new Release("R1");
    releaseOne.setState(ReleaseState.COMPLETED);

    releaseTwo = new Release("R2");

    datastore.save(releaseOne);
    datastore.save(releaseTwo);

    datastore.ensureIndexes();

    submissionRepository = new SubmissionRepository(morphia, datastore);
    releaseRepository = new ReleaseRepository(morphia, datastore);

    releaseMorphiaQuery = new MorphiaQuery<Release>(morphia, datastore, QRelease.release);
    submissionMorphiaQuery = new MorphiaQuery<Submission>(morphia, datastore, QSubmission.submission);
  }

  @Test
  public void testFindReleases() {
    val expected = newArrayList(releaseOne, releaseTwo);
    val actual = releaseRepository.findReleases();
    val morphiaResponse = copyOf(releaseMorphiaQuery.list());

    assertThat(actual).isEqualTo(expected);
    assertThat(morphiaResponse).isEqualTo(expected);
  }

  @Test
  public void testFindReleaseSummaries() {
    val actual = releaseRepository.findReleaseSummaries();
    val full = releaseRepository.findReleases();

    assertThat(actual).hasSize(full.size());
  }

  @Test
  public void testFindNextReleaseQueue() {
    val actual = releaseRepository.findNextReleaseQueue();
    val full = releaseRepository.findNextRelease();

    assertThat(full.getName()).isNotEmpty();
    assertThat(actual.getName()).isNull();
    assertThat(actual.getQueue()).isEqualTo(full.getQueue());
  }

  @Test
  public void testFind() {
    val expected = releaseOne;
    val actual = releaseRepository.findReleaseByName(releaseOne.getName());
    val morphiaResponse = releaseMorphiaQuery.where(QRelease.release.name.eq(releaseOne.getName())).singleResult();

    assertThat(actual).isEqualTo(expected);
    assertThat(morphiaResponse).isEqualTo(expected);
  }

  @Test
  public void testFindOpen() {
    val expected = releaseTwo;
    val actual = releaseRepository.findOpenRelease();
    val morphiaResponse =
        releaseMorphiaQuery.where(QRelease.release.state.eq(ReleaseState.OPENED)).singleResult();

    assertThat(actual).isEqualTo(expected);
    assertThat(morphiaResponse).isEqualTo(expected);
  }

  // @Test
  // public void testUpdateRelease() throws Exception {
  // val submission = new Submission("PRJ3", "Project Three", releaseTwo.getName());
  // val getOpenReleaseQuery =
  // releaseMorphiaQuery.where(QRelease.release.state.eq(ReleaseState.OPENED));
  //
  // // Check that Release in DB does not have Submission
  // // FIXME: Add submissions
  // val release = new ReleaseSubmissionView(getOpenReleaseQuery.singleResult());
  // release.getSubmission(submission.getProjectKey());
  // assertThat(!release.getSubmission(submission.getProjectKey()).isPresent());
  //
  // // Add Submission
  // release.addSubmission(submission);
  //
  // // Check that Release has Submission
  // assertThat(release.getSubmissions()).contains(submission);
  //
  // // Save to DB
  // releaseRepository.updateRelease(release.getName(), release);
  //
  // // Confirm that Release in DB has Submission
  // val actual = getOpenReleaseQuery.singleResult();
  // assertThat(actual).isEqualTo(release);
  // assertThat(actual.getSubmissions()).contains(submission);
  // }

  // @Test
  // public void testUpdateCompletedRelease() throws Exception {
  // val submission = new Submission("P3", "P3", "R1");
  // val testRelease = new Release("R1");
  // testRelease.setState(ReleaseState.COMPLETED);
  // testRelease.setReleaseDate();
  // testRelease.addSubmission(submission);
  //
  // val newRelease = releaseRepository.updateCompletedRelease(testRelease);
  // assertThat(newRelease.getName()).isEqualTo(testRelease.getName());
  // assertThat(newRelease.getState()).isEqualTo(testRelease.getState());
  // assertThat(newRelease.getReleaseDate()).isEqualTo(testRelease.getReleaseDate());
  //
  // val submissions = newRelease.getSubmissions();
  // assertThat(submissions).hasSize(1);
  // assertThat(submissions.get(0)).isEqualTo(submission);
  //
  // assertThat(submissionMorphiaQuery.list()).hasSize(1);
  // }

  // @Test
  // public void testUpdateReleaseStringRelease() throws Exception {
  // // This method is tested exactly the same way as the updateCompletedRelease()
  // testUpdateCompletedRelease();
  // }

}
