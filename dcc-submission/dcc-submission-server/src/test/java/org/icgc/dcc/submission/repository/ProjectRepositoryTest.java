package org.icgc.dcc.submission.repository;

import static java.lang.String.format;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import lombok.val;

import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.test.mongodb.EmbeddedMongo;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.MongoClientURI;

public class ProjectRepositoryTest {

  @Rule
  public final EmbeddedMongo embeddedMongo = new EmbeddedMongo();

  private ProjectRepository projectRepository;

  @Before
  public void setUp() throws Exception {
    val mailService = mock(MailService.class);
    val morphia = new Morphia();

    MongoClientURI uri = new MongoClientURI(getMongoUri());

    Datastore datastore = morphia.createDatastore(embeddedMongo.getMongo(), uri.getDatabase());
    datastore.ensureIndexes();

    projectRepository = new ProjectRepository(morphia, datastore, mailService);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testFindProjects() {
    val projects = projectRepository.findProjects();
    assertThat(projects).isNull();
  }

  private String getMongoUri() {
    return format("mongodb://localhost:%s/dcc-submission-server.ProjectRepository", embeddedMongo.getPort());
  }

}
