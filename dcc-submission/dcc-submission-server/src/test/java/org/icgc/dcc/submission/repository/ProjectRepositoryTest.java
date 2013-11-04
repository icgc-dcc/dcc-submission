package org.icgc.dcc.submission.repository;

import static java.lang.String.format;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import lombok.val;

import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.core.model.QProject;
import org.icgc.dcc.test.mongodb.EmbeddedMongo;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException.DuplicateKey;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

@RunWith(MockitoJUnitRunner.class)
public class ProjectRepositoryTest {

  @Rule
  public final EmbeddedMongo embeddedMongo = new EmbeddedMongo();

  private final String AUTH_ALLOWED_USER = "richard";
  private final String AUTH_NOT_ALLOWED_USER = "ricardo";

  private ProjectRepository projectRepository;

  private MorphiaQuery<Project> bareMorphiaQuery;

  private final Project projectOne = new Project("PRJ1", "Project One");

  private final Project projectTwo = new Project("PRJ2", "Project Two");

  private Datastore datastore;

  @Before
  public void setUp() throws Exception {
    val mailService = mock(MailService.class);
    val morphia = new Morphia();
    val uri = new MongoClientURI(getMongoUri());

    projectOne.setUsers(Sets.newHashSet(AUTH_ALLOWED_USER));

    datastore = morphia.createDatastore(embeddedMongo.getMongo(), uri.getDatabase());

    datastore.save(projectOne);
    datastore.save(projectTwo);

    datastore.ensureIndexes();

    projectRepository = new ProjectRepository(morphia, datastore, mailService);

    bareMorphiaQuery = new MorphiaQuery<Project>(morphia, datastore, QProject.project);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testFindProjects() {
    val expected = Sets.newHashSet(projectOne, projectTwo);
    val actual = projectRepository.findAll();
    val bare = ImmutableSet.copyOf(bareMorphiaQuery.list());

    assertThat(actual).isEqualTo(expected);
    assertThat(bare).isEqualTo(expected);
  }

  @Test
  public void testFindProjectsForUser() {
    val expected = Sets.newHashSet(projectOne);
    val actual = projectRepository.findAllForUser(AUTH_ALLOWED_USER);
    val bare = ImmutableSet.copyOf(bareMorphiaQuery.where(QProject.project.users.contains(AUTH_ALLOWED_USER)).list());

    assertThat(actual).isEqualTo(expected);
    assertThat(bare).isEqualTo(expected);
  }

  @Test
  public void testFindProject() {
    val expected = projectOne;
    val actual = projectRepository.find(projectOne.getKey());
    val bare = bareMorphiaQuery.where(QProject.project.key.eq(projectOne.getKey())).singleResult();

    assertThat(actual).isEqualTo(expected);
    assertThat(bare).isEqualTo(expected);
  }

  @Test
  public void testFindProjectForAllowedUser() {
    val expected = projectOne;
    val actual = projectRepository.findForUser(projectOne.getKey(), AUTH_ALLOWED_USER);
    val bare =
        bareMorphiaQuery.where(QProject.project.key.eq(projectOne.getKey()))
            .where(QProject.project.users.contains(AUTH_ALLOWED_USER)).singleResult();

    assertThat(actual).isEqualTo(expected);
    assertThat(bare).isEqualTo(expected);
  }

  @Test
  public void testFindProjectForNotAllowedUser() {
    val actual = projectRepository.findForUser(projectOne.getKey(), AUTH_NOT_ALLOWED_USER);
    val bare =
        bareMorphiaQuery.where(QProject.project.key.eq(projectOne.getKey()))
            .where(QProject.project.users.contains(AUTH_NOT_ALLOWED_USER)).singleResult();

    assertThat(actual).isNull();
    assertThat(bare).isNull();
  }

  @Test
  public void testInsertProject() throws Exception {
    val projectThree = new Project("PRJ3", "Project Three");

    assertThat(projectRepository.find(projectThree.getKey())).isNull();

    val response = projectRepository.upsert(projectThree);

    assertThat(response).isNotNull();

    assertThat(projectRepository.find(projectThree.getKey())).isEqualTo(projectThree);
    assertThat(bareMorphiaQuery.where(QProject.project.key.eq(projectThree.getKey())).singleResult()).isEqualTo(
        projectThree);
  }

  @Test
  public void testUpdateProject() throws Exception {
    val projectThree = new Project("PRJ3", "Project Three");

    assertThat(projectRepository.find(projectThree.getKey())).isNull();

    val first = projectRepository.upsert(projectThree);
    projectThree.setAlias("PRJ ALIAS");
    val second = projectRepository.upsert(projectThree);

    assertThat(first).isEqualTo(second);

    assertThat(projectRepository.find(projectThree.getKey())).isEqualTo(projectThree);
    assertThat(bareMorphiaQuery.where(QProject.project.key.eq(projectThree.getKey())).singleResult()).isEqualTo(
        projectThree);
  }

  @Test(expected = DuplicateKey.class)
  public void testInsertDuplicateProject() throws Exception {
    assertThat(projectRepository.find("PRJ3")).isNull();

    // Need to create two objects so it tries to add and not update
    projectRepository.upsert(new Project("PRJ3", "Project Three"));
    projectRepository.upsert(new Project("PRJ3", "Project Three"));
  }

  private String getMongoUri() {
    return format("mongodb://localhost:%s/dcc-submission-server.ProjectRepository", embeddedMongo.getPort());
  }
}
