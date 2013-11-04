package org.icgc.dcc.submission.repository;

import static java.lang.String.format;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import lombok.val;

import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.test.mongodb.EmbeddedMongo;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.common.collect.Sets;
import com.mongodb.MongoClientURI;

public class ProjectRepositoryTest {

  @Rule
  public final EmbeddedMongo embeddedMongo = new EmbeddedMongo();

  private final String AUTH_ALLOWED_USER = "richard";
  private final String AUTH_NOT_ALLOWED_USER = "ricardo";

  private ProjectRepository projectRepository;

  private Project projectOne;

  private Project projectTwo;

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
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testFindProjects() {
    val projects = projectRepository.findProjects();
    assertThat(projects).isEqualTo(Sets.newHashSet(projectOne, projectTwo));
  }

  @Test
  public void testFindProjectsForUser() {
    val projects = projectRepository.findProjects(AUTH_ALLOWED_USER);
    assertThat(projects).isEqualTo(Sets.newHashSet(projectOne));
  }

  @Test
  public void testFindProject() {
    val projects = projectRepository.findProject(projectOne.getKey());
    assertThat(projects).isEqualTo(projectOne);
  }

  @Test
  public void testFindProjectForAllowedUser() {
    val projects = projectRepository.findProject(projectOne.getKey(), AUTH_ALLOWED_USER);
    assertThat(projects).isEqualTo(projectOne);
  }

  @Test
  public void testFindProjectForNotAllowedUser() {
    val projects = projectRepository.findProject(projectOne.getKey(), AUTH_NOT_ALLOWED_USER);
    assertThat(projects).isNull();
  }

  private String getMongoUri() {
    return format("mongodb://localhost:%s/dcc-submission-server.ProjectRepository", embeddedMongo.getPort());
  }

}
