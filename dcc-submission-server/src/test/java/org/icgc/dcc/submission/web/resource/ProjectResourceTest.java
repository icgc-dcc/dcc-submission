package org.icgc.dcc.submission.web.resource;

import static javax.ws.rs.client.Entity.json;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.glassfish.jersey.internal.util.Base64.encodeAsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.NON_EXTENSIBLE;

import java.util.List;

import javax.annotation.Resource;
import javax.ws.rs.core.Response;

import org.elasticsearch.common.collect.Lists;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.service.ProjectService;
import org.icgc.dcc.submission.service.ReleaseService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Sets;
import com.google.common.net.HttpHeaders;
import com.mongodb.CommandResult;
import com.mongodb.MongoException.DuplicateKey;

import lombok.SneakyThrows;
import lombok.val;

public class ProjectResourceTest extends ResourceTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String UNAUTH_USER = "nobody";
  private static final String AUTH_ALLOWED_USER = "richard";
  private static final String AUTH_NOT_ALLOWED_USER = "ricardo";

  private static final String AUTH_HEADER = HttpHeaders.AUTHORIZATION.toString();

  private String getAuthValue(String username) {
    return "X-DCC-Auth " + encodeAsString(username + ":" + username + "spasswd");
  }

  @Autowired
  private ProjectService projectService;
  @Autowired
  private ReleaseService releaseService;
  @Autowired
  private List<Release> releases;
  @Resource
  private Submission submissionOne;
  @Resource
  private Submission submissionTwo;
  @Resource
  private Project projectOne;
  @Resource
  private Project projectTwo;

  @Configuration
  static class ResourceConfig {

    @Bean
    public Project projectTwo() {
      return new Project("PRJ2", "Project Two");
    }

    @Bean
    public Project projectOne() {
      val projectOne = new Project("PRJ1", "Project One");
      projectOne.setUsers(Sets.newHashSet(AUTH_ALLOWED_USER));

      return projectOne;
    }

    @Bean
    public Submission submissionTwo() {
      return new Submission("PRJ2", "Project Two", release().getName());
    }

    @Bean
    public Submission submissionOne() {
      return new Submission("PRJ1", "Project One", release().getName());
    }

    @Bean
    public Release release() {
      val release = new Release("REL1");

      return release;
    }

    @Bean
    public ReleaseService releaseService() {
      val releases = Lists.newArrayList(release());

      val releaseService = mock(ReleaseService.class);
      when(releaseService.getNextRelease()).thenReturn(release());
      when(releaseService.getReleases()).thenReturn(releases);

      return releaseService;
    }

    @Bean
    public ProjectService projectService() {
      val projectOne = projectOne();
      val projectTwo = projectTwo();

      val submissionOne = submissionOne();
      val submissionTwo = submissionTwo();
      val submissions = Sets.newHashSet(submissionOne, submissionTwo);

      val projectService = mock(ProjectService.class);
      when(projectService.getProject(projectOne.getKey())).thenReturn(projectOne);
      when(projectService.getProjectByUser(projectOne.getKey(), AUTH_ALLOWED_USER)).thenReturn(projectOne);
      when(projectService.getProjectsByUser(any(String.class))).thenReturn(Lists.newArrayList(projectOne));
      when(projectService.getProjects()).thenReturn(Lists.newArrayList(projectOne, projectTwo));
      when(projectService.getSubmissions(Lists.newArrayList(release()), projectOne.getKey())).thenReturn(submissions);

      return projectService;
    }

  }

  @Override
  protected void register() {
    register(ResourceConfig.class);
  }

  @Test
  public void testGetProjectsWhenNotAuthorized() {
    val reponse =
        (OutboundJaxrsResponse) target().path("projects").request(MIME_TYPE)
            .header(AUTH_HEADER, getAuthValue(UNAUTH_USER)).get();
    verifyZeroInteractions(projectService);
    assertThat(reponse.getStatus()).isEqualTo(UNAUTHORIZED.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("");
  }

  @Test
  public void testGetProjectsWhenAuthorized() {
    val reponse =
        target().path("projects").request(MIME_TYPE).header(AUTH_HEADER, getAuthValue(AUTH_ALLOWED_USER)).get();
    verify(projectService).getProjectsByUser(any(String.class));
    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertEntity(reponse, "[{\"key\":\"PRJ1\",\"name\":\"Project One\""
        + ",\"users\": [\"" + AUTH_ALLOWED_USER + "\"], \"groups\": []}]");
  }

  @Test
  public void testGetProjectsWhenAuthorizedAsAdmin() {
    val reponse = target().path("projects").request(MIME_TYPE).get();
    verify(projectService, atLeast(1)).getProjects();
    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertEntity(reponse, "[{\"key\":\"PRJ1\",\"name\":\"Project One\",\"users\": [\"" + AUTH_ALLOWED_USER
        + "\"], \"groups\": []},{\"key\":\"PRJ2\",\"name\":\"Project Two\", \"users\": [], \"groups\": []}]");
  }

  @Test
  public void testGetProjectWhenNotAuthorized() {
    val reponse =
        (OutboundJaxrsResponse) target().path("projects/" + projectOne.getKey()).request(MIME_TYPE)
            .header(AUTH_HEADER, getAuthValue(UNAUTH_USER)).get();
    verifyZeroInteractions(projectService);
    assertThat(reponse.getStatus()).isEqualTo(UNAUTHORIZED.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("");
  }

  @Test
  public void testGetProjectWhenAuthorizedWithoutAccess() {
    val reponse =
        target().path("projects/" + projectOne.getKey()).request(MIME_TYPE)
            .header(AUTH_HEADER, getAuthValue(AUTH_NOT_ALLOWED_USER))
            .get();
    verify(projectService, never()).getProject(any(String.class));
    assertThat(reponse.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
    assertEntity(reponse, "{\"code\":\"NoSuchEntity\",\"parameters\":[\"PRJ1\"]}");
  }

  @Test
  public void testGetProjectWhenAuthorizedWithAccess() {
    val reponse =
        target().path("projects/" + projectOne.getKey()).request(MIME_TYPE)
            .header(AUTH_HEADER, getAuthValue(AUTH_ALLOWED_USER)).get();
    verify(projectService).getProject(projectOne.getKey());
    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertEntity(reponse, "{\"key\":\"PRJ1\",\"name\":\"Project One\""
        + ",\"users\": [\"" + AUTH_ALLOWED_USER + "\"], \"groups\": []}");
  }

  @Test
  public void testGetProjectWhenAuthorizedDoesNotExist() {
    val reponse =
        target().path("projects/DNE").request(MIME_TYPE).get();
    assertThat(reponse.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
    assertEntity(reponse, "{\"code\":\"NoSuchEntity\",\"parameters\":[\"DNE\"]}");
  }

  @Test
  public void testAddProjectWhenNotAuthorized() throws Exception {
    val reponse =
        (OutboundJaxrsResponse) target().path("projects").request(MIME_TYPE)
            .header(AUTH_HEADER, getAuthValue(UNAUTH_USER)).post(json("{}"));
    verifyZeroInteractions(projectService);
    assertThat(reponse.getStatus()).isEqualTo(UNAUTHORIZED.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("");
  }

  @Test
  public void testAddProjectWithMissingFields() throws Exception {
    val projectJson = json("{\"keymissing\":\"PRJ1\",\"name\":\"Project One\"}");
    val reponse = target().path("projects").request(MIME_TYPE).post(projectJson);
    verifyZeroInteractions(projectService);
    // Apparently JAX-RS doesn't have a '422 Unprocessable Entity' Status
    assertThat(reponse.getStatus()).isEqualTo(422);
  }

  @Test
  public void testAddProjectWithInvalidSymbolsInProjectKey() throws Exception {
    val projectJson = json("{\"key\":\"P#$%^&*RJ1\",\"name\":\"Project One\"}");
    val reponse = target().path("projects").request(MIME_TYPE).post(projectJson);
    verifyZeroInteractions(projectService);
    // Apparently JAX-RS doesn't have a '422 Unprocessable Entity' Status
    assertThat(reponse.getStatus()).isEqualTo(422);
  }

  @Test
  public void testAddProjectWithSpaceInProjectKey() throws Exception {
    val projectJson = json("{\"key\":\"P RJ1\",\"name\":\"Project One\"}");
    val reponse = target().path("projects").request(MIME_TYPE).post(projectJson);
    verifyZeroInteractions(projectService);
    // Apparently JAX-RS doesn't have a '422 Unprocessable Entity' Status
    assertThat(reponse.getStatus()).isEqualTo(422);
  }

  @Test
  public void testAddProjectWhenNotAdmin() throws Exception {
    val projectJson = json("{\"key\":\"PRJ1\",\"name\":\"Project One\"}");
    val reponse =
        target().path("projects").request(MIME_TYPE).header(AUTH_HEADER, getAuthValue(AUTH_ALLOWED_USER))
            .post(projectJson);

    verify(projectService, never()).addProject(any(Project.class));

    assertThat(reponse.getStatus()).isEqualTo(UNAUTHORIZED.getStatusCode());
    assertEntity(reponse, "{\"code\":\"Unauthorized\",\"parameters\":[]}");
  }

  @Test
  public void testAddProjectWhenAdmin() throws Exception {
    val projectJson = json("{\"key\":\"PRJ1\",\"name\":\"Project One\", \"users\": [\"myuser\"]}");
    val reponse =
        (OutboundJaxrsResponse) target().path("projects").request(MIME_TYPE).post(projectJson);

    verify(projectService).addProject(any(Project.class));
    verify(releaseService).addSubmission("PRJ1", "Project One");

    assertThat(reponse.getStatus()).isEqualTo(CREATED.getStatusCode());
    assertThat(reponse.getLocation().toString()).isEqualTo("projects/PRJ1");
  }

  @Test
  public void testAddProjectThatAlreadyExists() throws Exception {
    doThrow(new DuplicateKey(mock(CommandResult.class))).when(projectService).addProject(any(Project.class));

    val projectJson = json("{\"key\":\"PRJ1\",\"name\":\"Project One\"}");
    val reponse = target().path("projects").request(MIME_TYPE).post(projectJson);

    verify(projectService).addProject(any(Project.class));
    verifyZeroInteractions(releaseService);

    assertThat(reponse.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    assertEntity(reponse, "{\"code\":\"AlreadyExists\",\"parameters\":[\"PRJ1\"]}");
  }

  @Test
  public void testUpdateProjectWhenNotAdmin() throws Exception {
    val projectJson = json("{\"key\":\"PRJ1\",\"name\":\"Project One\"}");
    val reponse =
        target().path("projects/PRJ1").request(MIME_TYPE).header(AUTH_HEADER, getAuthValue(AUTH_ALLOWED_USER))
            .post(projectJson);

    verify(projectService, never()).updateProject(any(Project.class));

    assertThat(reponse.getStatus()).isEqualTo(UNAUTHORIZED.getStatusCode());
    assertEntity(reponse, "{\"code\":\"Unauthorized\",\"parameters\":[]}");
  }

  @Test
  public void testUpdateProjectWhenAdmin() throws Exception {
    val projectJson = json("{\"key\":\"PRJ1\",\"name\":\"Project One\"}");
    val reponse = target().path("projects/PRJ1").request(MIME_TYPE).post(projectJson);

    verify(projectService).updateProject(any(Project.class));

    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
  }

  @Test
  public void testUpdateProjectWrongKey() throws Exception {
    val projectJson = json("{\"key\":\"PRJ1\",\"name\":\"Project One\"}");
    val reponse = target().path("projects/PRJ2").request(MIME_TYPE).post(projectJson);

    verify(projectService, never()).updateProject(any(Project.class));

    assertThat(reponse.getStatus()).isEqualTo(PRECONDITION_FAILED.getStatusCode());
  }

  @Test
  public void testGetProjectSubmissionsWhenNotAuthorized() throws Exception {
    val reponse =
        (OutboundJaxrsResponse) target().path("projects/" + projectOne.getKey() + "/releases").request(MIME_TYPE)
            .header(AUTH_HEADER, getAuthValue(UNAUTH_USER)).get();

    verifyZeroInteractions(releaseService);
    verifyZeroInteractions(projectService);

    assertThat(reponse.getStatus()).isEqualTo(UNAUTHORIZED.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("");
  }

  @Test
  public void testGetProjectSubmissionsWhenAuthorizedWithoutAccess() {
    val reponse =
        target().path("projects/" + projectOne.getKey()).request(MIME_TYPE)
            .header(AUTH_HEADER, getAuthValue(AUTH_NOT_ALLOWED_USER)).get();

    verifyZeroInteractions(releaseService);
    verify(projectService, never()).getProject(any(String.class));

    assertThat(reponse.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
    assertEntity(reponse, "{\"code\":\"NoSuchEntity\",\"parameters\":[\"PRJ1\"]}");
  }

  @Test
  public void testGetProjectSubmissions() throws Exception {
    val reponse = target().path("projects/" + projectOne.getKey() + "/releases").request(MIME_TYPE).get();

    verify(releaseService).getReleases();
    verify(projectService).getSubmissions(releases, projectOne.getKey());

    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertEntity(reponse,
        ""
            + "["
            + "{\"projectKey\":\"PRJ1\",\"projectName\":\"Project One\",\"releaseName\":\"REL1\",\"lastUpdated\":"
            + submissionOne.getLastUpdated().getTime()
            + ",\"state\":\"NOT_VALIDATED\",\"report\":{\"dataTypeReports\":[]}"
            + "}"
            + ","
            + "{\"projectKey\":\"PRJ2\",\"projectName\":\"Project Two\",\"releaseName\":\"REL1\",\"lastUpdated\":"
            + submissionTwo.getLastUpdated().getTime()
            + ",\"state\":\"NOT_VALIDATED\",\"report\":{\"dataTypeReports\":[]}"
            + "}"
            + "]");
  }

  @SneakyThrows
  private static void assertEntity(Response response, String expected) {
    val actual = ((OutboundJaxrsResponse) response).readEntity(String.class);
    assertEquals(expected, actual, NON_EXTENSIBLE);
  }

}
