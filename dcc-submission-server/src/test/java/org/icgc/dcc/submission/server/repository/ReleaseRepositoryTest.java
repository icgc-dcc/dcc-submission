package org.icgc.dcc.submission.server.repository;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.common.collect.Lists.newArrayList;
import static org.icgc.dcc.submission.release.model.QSubmission.submission;
import lombok.val;

import org.icgc.dcc.submission.release.ReleaseException;
import org.icgc.dcc.submission.release.model.QRelease;
import org.icgc.dcc.submission.release.model.QSubmission;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;
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
    releaseOne.addSubmission(submission1);

    releaseTwo = new Release("R2");
    releaseOne.addSubmission(submission2);

    datastore.save(releaseOne);
    datastore.save(releaseTwo);

    datastore.ensureIndexes();

    submissionRepository = new SubmissionRepository(morphia, datastore);
    releaseRepository = new ReleaseRepository(morphia, datastore, submissionRepository);

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

  @Test
  public void testAddReleaseSubmission() throws Exception {
    val submission = new Submission("PRJ3", "Project Three", releaseOne.getName());
    val getOpenReleaseQuery =
        releaseMorphiaQuery.where(QRelease.release.state.eq(ReleaseState.OPENED));

    // Check that Release in DB does not have Submission
    val openRelease = getOpenReleaseQuery.singleResult();
    try {
      openRelease.getSubmission(submission.getProjectKey());
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ReleaseException.class);
    }

    // Add Submission
    val modifiedRelease = releaseRepository.addReleaseSubmission(openRelease.getName(), submission);

    // Check that Release has Submission
    assertThat(modifiedRelease.getSubmissions()).contains(submission);

    // Confirm that Release in DB has Submission
    val actual = getOpenReleaseQuery.singleResult();
    assertThat(actual).isEqualTo(modifiedRelease);
    assertThat(actual.getSubmissions()).contains(submission);
  }

  @Test
  public void testUpdateRelease() throws Exception {
    val submission = new Submission("PRJ3", "Project Three", releaseTwo.getName());
    val getOpenReleaseQuery =
        releaseMorphiaQuery.where(QRelease.release.state.eq(ReleaseState.OPENED));

    // Check that Release in DB does not have Submission
    val release = getOpenReleaseQuery.singleResult();
    release.getSubmission(submission.getProjectKey());
    assertThat(!release.getSubmission(submission.getProjectKey()).isPresent());

    // Add Submission
    release.addSubmission(submission);

    // Check that Release has Submission
    assertThat(release.getSubmissions()).contains(submission);

    // Save to DB
    releaseRepository.updateRelease(release.getName(), release);

    // Confirm that Release in DB has Submission
    val actual = getOpenReleaseQuery.singleResult();
    assertThat(actual).isEqualTo(release);
    assertThat(actual.getSubmissions()).contains(submission);
  }

  private String getMongoUri() {
    return format("mongodb://localhost:%s/dcc-submission-server.ReleaseRepository", embeddedMongo.getPort());
  }

  @Test
  public void testUpdateCompletedRelease() throws Exception {
    val submission = new Submission("P3", "P3", "R1");
    val testRelease = new Release("R1");
    testRelease.setState(ReleaseState.COMPLETED);
    testRelease.setReleaseDate();
    testRelease.addSubmission(submission);

    val newRelease = releaseRepository.updateCompletedRelease(testRelease);
    assertThat(newRelease.getName()).isEqualTo(testRelease.getName());
    assertThat(newRelease.getState()).isEqualTo(testRelease.getState());
    assertThat(newRelease.getReleaseDate()).isEqualTo(testRelease.getReleaseDate());

    val submissions = newRelease.getSubmissions();
    assertThat(submissions).hasSize(1);
    assertThat(submissions.get(0)).isEqualTo(submission);

    assertThat(submissionMorphiaQuery.list()).hasSize(1);
  }

  @Test
  public void testUpdateReleaseStringRelease() throws Exception {
    // This method is tested exactly the same way as the updateCompletedRelease()
    testUpdateCompletedRelease();
  }

  @Test
  public void testUpdateReleaseSubmissionState() throws Exception {
    releaseRepository.updateReleaseSubmissionState("R1", "P1", SubmissionState.ERROR);

    val notErrorCount = submissionMorphiaQuery.where(submission.state.ne(SubmissionState.ERROR)).count();
    assertThat(notErrorCount).isEqualTo(1);

    val errorSubmissions = datastore.createQuery(Submission.class).filter("state", SubmissionState.ERROR).asList();
    assertThat(errorSubmissions).hasSize(1);
    assertThat(errorSubmissions.get(0).getId()).isEqualTo("R1#P1");
  }

  @Test
  public void testUpdateReleaseSubmission() throws Exception {
    val r1Release = datastore.createQuery(Release.class).filter("name", "R1").get();
    val submission = r1Release.getSubmission("P1").get();
    submission.setState(SubmissionState.ERROR);

    releaseRepository.updateReleaseSubmission("R1", submission);

    assertThat(datastore.createQuery(Submission.class).countAll()).isEqualTo(2);
    assertThat(datastore.createQuery(Submission.class).filter("releaseName", "R1").countAll()).isEqualTo(2);
    val errorSubmissions = datastore.createQuery(Submission.class).filter("state", SubmissionState.ERROR).asList();
    assertThat(errorSubmissions).hasSize(1);
    assertThat(errorSubmissions.get(0).getId()).isEqualTo("R1#P1");
  }

}
