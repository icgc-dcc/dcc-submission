package org.icgc.dcc.submission.repository;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.common.collect.Lists.newArrayList;

import org.icgc.dcc.submission.release.ReleaseException;
import org.icgc.dcc.submission.release.model.QRelease;
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

import lombok.val;

@RunWith(MockitoJUnitRunner.class)
public class ReleaseRepositoryTest extends AbstractRepositoryTest {

  private ReleaseRepository releaseRepository;

  private MorphiaQuery<Release> morphiaQuery;
  private Datastore datastore;
  private Release releaseOne;
  private Release releaseTwo;

  @Before
  public void setUp() throws Exception {
    val morphia = new Morphia();

    datastore = morphia.createDatastore(embeddedMongo.getMongo(), new MongoClientURI(getMongoUri()).getDatabase());

    releaseOne = new Release("R1");
    releaseOne.setState(ReleaseState.COMPLETED);
    releaseOne.addSubmission(new Submission("P1", "P1", "R1"));

    releaseTwo = new Release("R2");
    releaseOne.addSubmission(new Submission("P2", "P2", "R2"));

    datastore.save(releaseOne);
    datastore.save(releaseTwo);

    datastore.ensureIndexes();

    releaseRepository = new ReleaseRepository(morphia, datastore);

    morphiaQuery = new MorphiaQuery<Release>(morphia, datastore, QRelease.release);
  }

  @Test
  public void testFindReleases() {
    val expected = newArrayList(releaseOne, releaseTwo);
    val actual = releaseRepository.findReleases();
    val morphiaResponse = copyOf(morphiaQuery.list());

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
    val morphiaResponse = morphiaQuery.where(QRelease.release.name.eq(releaseOne.getName())).singleResult();

    assertThat(actual).isEqualTo(expected);
    assertThat(morphiaResponse).isEqualTo(expected);
  }

  @Test
  public void testFindOpen() {
    val expected = releaseTwo;
    val actual = releaseRepository.findOpenRelease();
    val morphiaResponse =
        morphiaQuery.where(QRelease.release.state.eq(ReleaseState.OPENED)).singleResult();

    assertThat(actual).isEqualTo(expected);
    assertThat(morphiaResponse).isEqualTo(expected);
  }

  @Test
  public void testAddReleaseSubmission() throws Exception {
    val submission = new Submission("PRJ3", "Project Three", releaseOne.getName());
    val getOpenReleaseQuery =
        morphiaQuery.where(QRelease.release.state.eq(ReleaseState.OPENED));

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
    val submission = new Submission("PRJ3", "Project Three", releaseOne.getName());
    val getOpenReleaseQuery =
        morphiaQuery.where(QRelease.release.state.eq(ReleaseState.OPENED));

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
}
