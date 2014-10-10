package org.icgc.dcc.submission.repository;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import lombok.val;

import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.core.model.QProject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException.DuplicateKey;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

public class ProjectRepositoryTest extends AbstractRepositoryTest {

  private final String AUTH_ALLOWED_USER = "richard";
  private final String AUTH_NOT_ALLOWED_USER = "ricardo";

  private ProjectRepository projectRepository;

  private MorphiaQuery<Project> morphiaQuery;

  private final Project projectOne = new Project("PRJ1", "Project One");

  private final Project projectTwo = new Project("PRJ2", "Project Two");

  private Datastore datastore;

  @Before
  public void setUp() throws Exception {
    val morphia = new Morphia();
    val uri = new MongoClientURI(getMongoUri());

    projectOne.setUsers(Sets.newHashSet(AUTH_ALLOWED_USER));

    datastore = morphia.createDatastore(embeddedMongo.getMongo(), uri.getDatabase());

    datastore.save(projectOne);
    datastore.save(projectTwo);

    datastore.ensureIndexes();

    projectRepository = new ProjectRepository(morphia, datastore);

    morphiaQuery = new MorphiaQuery<Project>(morphia, datastore, QProject.project);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testFindProjectProjects() {
    val expected = Lists.newArrayList(projectOne, projectTwo);
    val actual = projectRepository.findProjects();
    val morphiaResponse = ImmutableList.copyOf(morphiaQuery.list());

    assertThat(actual).isEqualTo(expected);
    assertThat(morphiaResponse).isEqualTo(expected);
  }

  @Test
  public void testFindProjectProjectsForUser() {
    val expected = Lists.newArrayList(projectOne);
    val actual = projectRepository.findProjectsByUser(AUTH_ALLOWED_USER);
    val morphiaResponse =
        ImmutableList.copyOf(morphiaQuery.where(QProject.project.users.contains(AUTH_ALLOWED_USER)).list());

    assertThat(actual).isEqualTo(expected);
    assertThat(morphiaResponse).isEqualTo(expected);
  }

  @Test
  public void testFindProject() {
    val expected = projectOne;
    val actual = projectRepository.findProject(projectOne.getKey());
    val morphiaResponse = morphiaQuery.where(QProject.project.key.eq(projectOne.getKey())).singleResult();

    assertThat(actual).isEqualTo(expected);
    assertThat(morphiaResponse).isEqualTo(expected);
  }

  @Test
  public void testFindProjectForUserAllowed() {
    val expected = projectOne;
    val actual = projectRepository.findProjectByUser(projectOne.getKey(), AUTH_ALLOWED_USER);
    val morphiaResponse =
        morphiaQuery.where(QProject.project.key.eq(projectOne.getKey()))
            .where(QProject.project.users.contains(AUTH_ALLOWED_USER)).singleResult();

    assertThat(actual).isEqualTo(expected);
    assertThat(morphiaResponse).isEqualTo(expected);
  }

  @Test
  public void testFindProjectForUserNotAllowed() {
    val actual = projectRepository.findProjectByUser(projectOne.getKey(), AUTH_NOT_ALLOWED_USER);
    val morphiaResponse =
        morphiaQuery.where(QProject.project.key.eq(projectOne.getKey()))
            .where(QProject.project.users.contains(AUTH_NOT_ALLOWED_USER)).singleResult();

    assertThat(actual).isNull();
    assertThat(morphiaResponse).isNull();
  }

  @Test
  public void testInsertProject() throws Exception {
    val projectThree = new Project("PRJ3", "Project Three");

    assertThat(projectRepository.findProject(projectThree.getKey())).isNull();

    val response = projectRepository.upsertProject(projectThree);

    assertThat(response).isNotNull();

    assertThat(projectRepository.findProject(projectThree.getKey())).isEqualTo(projectThree);
    assertThat(morphiaQuery.where(QProject.project.key.eq(projectThree.getKey())).singleResult()).isEqualTo(
        projectThree);
  }

  @Test
  public void testUpdateProject() throws Exception {
    val projectThree = new Project("PRJ3", "Project Three");

    assertThat(projectRepository.findProject(projectThree.getKey())).isNull();

    val first = projectRepository.upsertProject(projectThree);
    projectThree.setAlias("PRJ ALIAS");
    val second = projectRepository.upsertProject(projectThree);

    assertThat(first).isEqualTo(second);

    assertThat(projectRepository.findProject(projectThree.getKey())).isEqualTo(projectThree);
    assertThat(morphiaQuery.where(QProject.project.key.eq(projectThree.getKey())).singleResult()).isEqualTo(
        projectThree);
  }

  @Test(expected = DuplicateKey.class)
  public void testInsertDuplicateProject() throws Exception {
    assertThat(projectRepository.findProject("PRJ3")).isNull();

    // Need to create two objects so it tries to add and not update
    projectRepository.upsertProject(new Project("PRJ3", "Project Three"));
    projectRepository.upsertProject(new Project("PRJ3", "Project Three"));
  }

  private String getMongoUri() {
    return format("mongodb://localhost:%s/dcc-submission-server.ProjectRepository", embeddedMongo.getPort());
  }

}
