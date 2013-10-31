package org.icgc.dcc.submission.web.resource;

import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.glassfish.grizzly.http.util.Header.Authorization;
import static org.glassfish.jersey.internal.util.Base64.encodeAsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;

import javax.ws.rs.core.Application;

import lombok.val;

import org.icgc.dcc.submission.core.AbstractDccModule;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.repository.ProjectRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.inject.Module;

@RunWith(MockitoJUnitRunner.class)
public class ProjectResourceTest extends ResourceTest {

  @InjectMocks
  private ProjectRepository projectRepository;

  private final String UNAUTH_USER = "nobody";
  private final String AUTH_USER = "richard";
  private final String ADMIN_USER = "admin";

  private static final String AUTH_HEADER = Authorization.toString();

  private String getAuthValue(String username) {
    return "X-DCC-Auth " + encodeAsString(username + ":" + username + "spasswd");
  }

  @Override
  protected Application configure() {

    val projectOne = new Project("PRJ1", "Project One");
    projectOne.setUsers(Sets.newHashSet(AUTH_USER));
    projectOne.setGroups(Sets.newHashSet(AUTH_USER));

    val projectTwo = new Project("PRJ2", "Project Two");
    projectTwo.setUsers(Sets.newHashSet(ADMIN_USER));
    projectTwo.setGroups(Sets.newHashSet(ADMIN_USER));

    projectRepository = mock(ProjectRepository.class);
    when(projectRepository.findProject(projectOne.getKey())).thenReturn(projectOne);
    when(projectRepository.findProject(projectTwo.getKey())).thenReturn(projectTwo);
    when(projectRepository.findProjects()).thenReturn(Sets.newHashSet(projectOne, projectTwo));

    return super.configure();
  }

  @Override
  protected Collection<? extends Module> configureModules() {
    return ImmutableList.of(new AbstractDccModule() {

      @Override
      protected void configure() {
        bind(ProjectRepository.class).toInstance(projectRepository);
      }

    });
  }

  @Test
  public void testGetProjectWhenNotAuthorized() {
    val reponse =
        target().path("projects").request(MIME_TYPE).header(AUTH_HEADER, getAuthValue(UNAUTH_USER)).get();
    verifyZeroInteractions(projectRepository);
    assertThat(reponse.getStatus()).isEqualTo(UNAUTHORIZED.getStatusCode());
    assertThat(reponse.readEntity(String.class)).isEqualTo("");
  }

  @Test
  public void testGetProjectWhenAuthorized() {
    val reponse = target().path("projects").request(MIME_TYPE).header(AUTH_HEADER, getAuthValue(AUTH_USER)).get();
    verify(projectRepository).findProjects();
    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertThat(reponse.readEntity(String.class))
        .isEqualTo("{\"key\":\"PRJ1\",\"name\":\"Project One\"}");
  }

  @Test
  public void testGetProjectWhenAuthorizedAsAdmin() {
    val reponse = target().path("projects").request(MIME_TYPE).header(AUTH_HEADER, getAuthValue(ADMIN_USER)).get();
    verify(projectRepository).findProjects();
    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertThat(reponse.readEntity(String.class))
        .isEqualTo("[{\"key\":\"PRJ2\",\"name\":\"Project Two\"},{\"key\":\"PRJ1\",\"name\":\"Project One\"}]");
  }
}
