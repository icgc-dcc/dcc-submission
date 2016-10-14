package org.icgc.dcc.submission.server.repository;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.common.collect.Lists.newArrayList;
import static org.icgc.dcc.submission.release.model.ReleaseState.COMPLETED;
import lombok.val;

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
  public void testUpdateRelease() throws Exception {
    releaseTwo.setState(COMPLETED);
    releaseRepository.updateRelease("R2", releaseTwo);
    val completedReleaseCount = morphiaQuery.where(QRelease.release.state.eq(COMPLETED)).count();
    assertThat(completedReleaseCount).isEqualTo(2);
  }

  @Test
  public void testUpdateCompletedRelease() throws Exception {
    releaseTwo.complete();
    releaseRepository.updateCompletedRelease(releaseTwo);

    val releases = morphiaQuery.where(QRelease.release.name.eq("R2")).list();
    assertThat(releases).hasSize(1);

    val r2 = releases.get(0);
    assertThat(r2.getState()).isEqualTo(ReleaseState.COMPLETED);
    assertThat(r2.getReleaseDate()).isEqualTo(releaseTwo.getReleaseDate());
  }

}
