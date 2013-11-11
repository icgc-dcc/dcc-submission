package org.icgc.dcc.submission.repository;

import static java.lang.String.format;
import static org.fest.assertions.api.Assertions.assertThat;
import lombok.val;

import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.release.ReleaseException;
import org.icgc.dcc.submission.release.model.QRelease;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.ReleaseState;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.test.mongodb.EmbeddedMongo;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mongodb.MongoClientURI;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

@RunWith(MockitoJUnitRunner.class)
public class ReleaseRepositoryTest {

  @Rule
  public final EmbeddedMongo embeddedMongo = new EmbeddedMongo();

  @Mock
  public MailService mailService;

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

    releaseTwo = new Release("R2");

    datastore.save(releaseOne);
    datastore.save(releaseTwo);

    datastore.ensureIndexes();

    releaseRepository = new ReleaseRepository(morphia, datastore, mailService);

    morphiaQuery = new MorphiaQuery<Release>(morphia, datastore, QRelease.release);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testFindAll() {
    val expected = Sets.newHashSet(releaseOne, releaseTwo);
    val actual = releaseRepository.findAll();
    val morphiaResponse = ImmutableSet.copyOf(morphiaQuery.list());

    assertThat(actual).isEqualTo(expected);
    assertThat(morphiaResponse).isEqualTo(expected);
  }

  @Test
  public void testFind() {
    val expected = releaseOne;
    val actual = releaseRepository.find(releaseOne.getName());
    val morphiaResponse = morphiaQuery.where(QRelease.release.name.eq(releaseOne.getName())).singleResult();

    assertThat(actual).isEqualTo(expected);
    assertThat(morphiaResponse).isEqualTo(expected);
  }

  @Test
  public void testFindOpen() {
    val expected = releaseTwo;
    val actual = releaseRepository.findOpen();
    val morphiaResponse =
        morphiaQuery.where(QRelease.release.state.eq(ReleaseState.OPENED)).singleResult();

    assertThat(actual).isEqualTo(expected);
    assertThat(morphiaResponse).isEqualTo(expected);
  }

  @Test
  public void testAddSubmission() throws Exception {
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
    val modifiedRelease = releaseRepository.addSubmission(submission, openRelease.getName());

    // Check that Release has Submission
    assertThat(modifiedRelease.getSubmissions()).contains(submission);

    // Confirm that Release in DB has Submission
    val actual = getOpenReleaseQuery.singleResult();
    assertThat(actual).isEqualTo(modifiedRelease);
    assertThat(actual.getSubmissions()).contains(submission);
  }

  @Test
  public void testUpdate() throws Exception {
    val submission = new Submission("PRJ3", "Project Three", releaseOne.getName());
    val getOpenReleaseQuery =
        morphiaQuery.where(QRelease.release.state.eq(ReleaseState.OPENED));

    // Check that Release in DB does not have Submission
    val release = getOpenReleaseQuery.singleResult();
    try {
      release.getSubmission(submission.getProjectKey());
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ReleaseException.class);
    }

    // Add Submission
    release.addSubmission(submission);

    // Check that Release has Submission
    assertThat(release.getSubmissions()).contains(submission);

    // Save to DB
    releaseRepository.update(release);

    // Confirm that Release in DB has Submission
    val actual = getOpenReleaseQuery.singleResult();
    assertThat(actual).isEqualTo(release);
    assertThat(actual.getSubmissions()).contains(submission);
  }

  private String getMongoUri() {
    return format("mongodb://localhost:%s/dcc-submission-server.ReleaseRepository", embeddedMongo.getPort());
  }
}
