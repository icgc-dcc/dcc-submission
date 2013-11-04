package org.icgc.dcc.submission.web.resource;

import static javax.ws.rs.client.Entity.json;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.glassfish.grizzly.http.util.Header.Authorization;
import static org.glassfish.jersey.internal.util.Base64.encodeAsString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;

import javax.ws.rs.core.Application;

import lombok.val;

import org.icgc.dcc.submission.core.AbstractDccModule;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.services.ProjectService;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.inject.Module;
import com.mongodb.CommandResult;
import com.mongodb.MongoException.DuplicateKey;

public class ProjectResourceTest extends ResourceTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final String UNAUTH_USER = "nobody";
  private final String AUTH_ALLOWED_USER = "richard";
  private final String AUTH_NOT_ALLOWED_USER = "ricardo";

  private static final String AUTH_HEADER = Authorization.toString();

  private String getAuthValue(String username) {
    return "X-DCC-Auth " + encodeAsString(username + ":" + username + "spasswd");
  }

  private ProjectService projectService;

  private Project projectOne;

  private Project projectTwo;

  @Override
  protected Application configure() {

    projectOne = new Project("PRJ1", "Project One");
    projectOne.setUsers(Sets.newHashSet(AUTH_ALLOWED_USER));

    projectTwo = new Project("PRJ2", "Project Two");

    projectService = mock(ProjectService.class);
    when(projectService.findProject(projectOne.getKey())).thenReturn(projectOne);
    when(projectService.findProjectForUser(projectOne.getKey(), AUTH_ALLOWED_USER)).thenReturn(projectOne);
    when(projectService.findProjectsForUser(any(String.class))).thenReturn(Sets.newHashSet(projectOne));
    when(projectService.findProjects()).thenReturn(Sets.newHashSet(projectOne, projectTwo));

    return super.configure();
  }

  @Override
  protected Collection<? extends Module> configureModules() {
    return ImmutableList.of(new AbstractDccModule() {

      @Override
      protected void configure() {
        bind(ProjectService.class).toInstance(projectService);
      }

    });
  }

  @Test
  public void testGetProjectsWhenNotAuthorized() {
    val reponse =
        target().path("projects").request(MIME_TYPE).header(AUTH_HEADER, getAuthValue(UNAUTH_USER)).get();
    verifyZeroInteractions(projectService);
    assertThat(reponse.getStatus()).isEqualTo(UNAUTHORIZED.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("");
  }

  @Test
  public void testGetProjectsWhenAuthorized() {
    val reponse =
        target().path("projects").request(MIME_TYPE).header(AUTH_HEADER, getAuthValue(AUTH_ALLOWED_USER)).get();
    verify(projectService).findProjectsForUser(any(String.class));
    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("[{\"key\":\"PRJ1\",\"name\":\"Project One\"}]");
  }

  @Test
  public void testGetProjectsWhenAuthorizedAsAdmin() {
    val reponse = target().path("projects").request(MIME_TYPE).get();
    verify(projectService, atLeast(1)).findProjects();
    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertThat(reponse.readEntity(String.class))
        .isEqualTo("[{\"key\":\"PRJ2\",\"name\":\"Project Two\"},{\"key\":\"PRJ1\",\"name\":\"Project One\"}]");
  }

  @Test
  public void testGetProjectWhenNotAuthorized() {
    val reponse =
        target().path("projects/" + projectOne.getKey()).request(MIME_TYPE)
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
    verify(projectService, never()).findProject(any(String.class));
    assertThat(reponse.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("{\"code\":\"NoSuchEntity\",\"parameters\":[\"PRJ1\"]}");
  }

  @Test
  public void testGetProjectWhenAuthorizedWithAccess() {
    val reponse =
        target().path("projects/" + projectOne.getKey()).request(MIME_TYPE)
            .header(AUTH_HEADER, getAuthValue(AUTH_ALLOWED_USER)).get();
    verify(projectService).findProjectForUser(projectOne.getKey(), AUTH_ALLOWED_USER);
    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("{\"key\":\"PRJ1\",\"name\":\"Project One\"}");
  }

  @Test
  public void testGetProjectWhenAuthorizedAsAdmin() {
    val reponse =
        target().path("projects/" + projectOne.getKey()).request(MIME_TYPE).get();
    verify(projectService).findProject(projectOne.getKey());
    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("{\"key\":\"PRJ1\",\"name\":\"Project One\"}");
  }

  @Test
  public void testGetProjectWhenAuthorizedDoesNotExist() {
    val reponse =
        target().path("projects/DNE").request(MIME_TYPE).get();
    assertThat(reponse.getStatus()).isEqualTo(NOT_FOUND.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("{\"code\":\"NoSuchEntity\",\"parameters\":[\"DNE\"]}");
  }

  @Test
  public void testAddProjectWhenNotAuthorized() throws Exception {
    val reponse =
        target().path("projects").request(MIME_TYPE).header(AUTH_HEADER, getAuthValue(UNAUTH_USER)).post(json("{}"));
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
  public void testAddProjectWithSymbolsInProjectKey() throws Exception {
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
    assertThat(reponse.readEntity(String.class)).isEqualTo("{\"code\":\"Unauthorized\",\"parameters\":[]}");
  }

  @Test
  public void testAddProjectWhenAdmin() throws Exception {
    val projectJson = json("{\"key\":\"PRJ1\",\"name\":\"Project One\"}");
    val reponse = target().path("projects").request(MIME_TYPE).post(projectJson);
    verify(projectService).addProject(any(Project.class));
    assertThat(reponse.getStatus()).isEqualTo(CREATED.getStatusCode());
    assertThat(reponse.getLocation().toString()).isEqualTo("projects/PRJ1");
  }

  @Test
  public void testAddProjectWithValidProjectKeyCharacters() throws Exception {
    val projectJson = json("{\"key\":\"ABC.abc_12-3\",\"name\":\"Project One\"}");
    val reponse = target().path("projects").request(MIME_TYPE).post(projectJson);
    verify(projectService).addProject(any(Project.class));
    assertThat(reponse.getStatus()).isEqualTo(CREATED.getStatusCode());
    assertThat(reponse.getLocation().toString()).isEqualTo("projects/ABC.abc_12-3");
  }

  @Test
  public void testAddProjectThatAlreadyExists() throws Exception {
    doThrow(new DuplicateKey(mock(CommandResult.class))).when(projectService).addProject(any(Project.class));

    val projectJson = json("{\"key\":\"PRJ1\",\"name\":\"Project One\"}");
    val reponse = target().path("projects").request(MIME_TYPE).post(projectJson);
    verify(projectService).addProject(any(Project.class));
    assertThat(reponse.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("{\"code\":\"AlreadyExists\",\"parameters\":[\"PRJ1\"]}");
  }

}
