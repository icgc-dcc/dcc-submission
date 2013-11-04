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

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.mongodb.MongoClientURI;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

public class ProjectRepositoryTest {

  @Rule
  public final EmbeddedMongo embeddedMongo = new EmbeddedMongo();

  private final String AUTH_ALLOWED_USER = "richard";
  private final String AUTH_NOT_ALLOWED_USER = "ricardo";

  private ProjectRepository projectRepository;

  private Project projectOne;

  private Project projectTwo;

  private MorphiaQuery<Project> bareMorphiaQuery;

  @Before
  public void setUp() throws Exception {
    val mailService = mock(MailService.class);
    val morphia = new Morphia();

    MongoClientURI uri = new MongoClientURI(getMongoUri());

    Datastore datastore = morphia.createDatastore(embeddedMongo.getMongo(), uri.getDatabase());
    projectOne = new Project("PRJ1", "Project One");
    projectOne.setUsers(Sets.newHashSet(AUTH_ALLOWED_USER));

    projectTwo = new Project("PRJ2", "Project Two");

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
    val actual = projectRepository.findProjects();
    val bare = ImmutableSet.copyOf(bareMorphiaQuery.list());

    assertThat(actual).isEqualTo(expected);
    assertThat(bare).isEqualTo(expected);
  }

  @Test
  public void testFindProjectsForUser() {
    val expected = Sets.newHashSet(projectOne);
    val actual = projectRepository.findProjectsForUser(AUTH_ALLOWED_USER);
    val bare = ImmutableSet.copyOf(bareMorphiaQuery.where(QProject.project.users.contains(AUTH_ALLOWED_USER)).list());

    assertThat(actual).isEqualTo(expected);
    assertThat(bare).isEqualTo(expected);
  }

  @Test
  public void testFindProject() {
    val expected = projectOne;
    val actual = projectRepository.findProject(projectOne.getKey());
    val bare = bareMorphiaQuery.where(QProject.project.key.eq(projectOne.getKey())).singleResult();

    assertThat(actual).isEqualTo(expected);
    assertThat(bare).isEqualTo(expected);
  }

  @Test
  public void testFindProjectForAllowedUser() {
    val expected = projectOne;
    val actual = projectRepository.findProjectForUser(projectOne.getKey(), AUTH_ALLOWED_USER);
    val bare =
        bareMorphiaQuery.where(QProject.project.key.eq(projectOne.getKey()))
            .where(QProject.project.users.contains(AUTH_ALLOWED_USER)).singleResult();

    assertThat(actual).isEqualTo(expected);
    assertThat(bare).isEqualTo(expected);
  }

  @Test
  public void testFindProjectForNotAllowedUser() {
    val actual = projectRepository.findProjectForUser(projectOne.getKey(), AUTH_NOT_ALLOWED_USER);
    val bare =
        bareMorphiaQuery.where(QProject.project.key.eq(projectOne.getKey()))
            .where(QProject.project.users.contains(AUTH_NOT_ALLOWED_USER)).singleResult();

    assertThat(actual).isNull();
    assertThat(bare).isNull();
  }

  private String getMongoUri() {
    return format("mongodb://localhost:%s/dcc-submission-server.ProjectRepository", embeddedMongo.getPort());
  }

}
