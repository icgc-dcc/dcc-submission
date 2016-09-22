/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.server.web.controller;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import lombok.val;

import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.server.service.ReleaseService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mongodb.CommandResult;
import com.mongodb.MongoException.DuplicateKey;

@WebMvcTest(ProjectController.class)
public class ProjectControllerTest extends ControllerTest {

  /**
   * Test data.
   */
  private static final String AUTH_ALLOWED_USER = "richard";
  private static final String AUTH_NOT_ALLOWED_USER = "ricardo";

  private Submission submissionOne;
  private Submission submissionTwo;
  private Project projectOne;
  private Project projectTwo;
  private List<Release> releases;

  /**
   * Test collaborators.
   */
  @MockBean
  private ReleaseService releaseService;

  @Before
  public void setUp() {
    val release = new Release("REL1");
    releases = ImmutableList.of(release);

    when(releaseService.getNextRelease()).thenReturn(release);
    when(releaseService.getReleases()).thenReturn(releases);

    projectOne = new Project("PRJ1", "Project One");
    projectOne.setUsers(Sets.newHashSet(AUTH_ALLOWED_USER));
    projectTwo = new Project("PRJ2", "Project Two");
    val projects = Lists.newArrayList(projectOne, projectTwo);

    submissionOne = new Submission("PRJ1", "Project One", release.getName());
    submissionTwo = new Submission("PRJ2", "Project Two", release.getName());
    val submissions = ImmutableSet.of(submissionOne, submissionTwo);

    when(projectService.getProjects()).thenReturn(projects);
    when(projectService.getProject(projectOne.getKey())).thenReturn(projectOne);
    when(projectService.getProjectByUser(projectOne.getKey(), AUTH_ALLOWED_USER)).thenReturn(projectOne);
    when(projectService.getProjectsByUser(AUTH_ALLOWED_USER)).thenReturn(Lists.newArrayList(projectOne));
    // when(projectService.getSubmissions(releases, projectOne.getKey()))
    // .thenReturn(submissions);
  }

  @Test
  public void testGetProjectsWhenNotAuthorized() throws Exception {
    mvc
        .perform(
            get("/ws/projects")
                .accept(MediaType.APPLICATION_JSON)
                .with(unknown()))
        .andExpect(status().isUnauthorized())
        .andExpect(contentEmpty());
  }

  @Test
  public void testGetProjectsWhenAuthorized() throws Exception {
    mvc
        .perform(
            get("/ws/projects")
                .accept(MediaType.APPLICATION_JSON)
                .with(user(AUTH_ALLOWED_USER)))
        .andExpect(status().isOk())
        .andExpect(content().string("[{\"key\":\"PRJ1\",\"name\":\"Project One\""
            + ",\"users\":[\"" + AUTH_ALLOWED_USER + "\"],\"groups\":[]}]"));
  }

  @Test
  public void testGetProjectsWhenAuthorizedAsAdmin() throws Exception {
    mvc
        .perform(
            get("/ws/projects")
                .accept(MediaType.APPLICATION_JSON)
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(content().string("[{\"key\":\"PRJ1\",\"name\":\"Project One\",\"users\":[\"" + AUTH_ALLOWED_USER
            + "\"],\"groups\":[]},{\"key\":\"PRJ2\",\"name\":\"Project Two\",\"users\":[],\"groups\":[]}]"));

    verify(projectService, atLeast(1)).getProjects();
  }

  @Test
  public void testGetProjectWhenNotAuthorized() throws Exception {
    mvc
        .perform(
            get("/ws/projects/" + projectOne.getKey())
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(contentEmpty());

    verifyZeroInteractions(projectService);
  }

  @Test
  public void testGetProjectWhenAuthorizedWithoutAccess() throws Exception {
    mvc
        .perform(
            get("/ws/projects/" + projectOne.getKey())
                .accept(MediaType.APPLICATION_JSON)
                .with(user(AUTH_NOT_ALLOWED_USER)))
        .andExpect(status().isNotFound())
        .andExpect(content().string("{\"code\":\"NoSuchEntity\",\"parameters\":[\"PRJ1\"]}"));

    verify(projectService, never()).getProject(any(String.class));
  }

  @Test
  public void testGetProjectWhenAuthorizedWithAccess() throws Exception {
    mvc
        .perform(
            get("/ws/projects/" + projectOne.getKey())
                .accept(MediaType.APPLICATION_JSON)
                .with(user(AUTH_ALLOWED_USER)))
        .andExpect(status().isOk())
        .andExpect(content().string("{\"key\":\"PRJ1\",\"name\":\"Project One\""
            + ",\"users\":[\"" + AUTH_ALLOWED_USER + "\"],\"groups\":[]}"));

    verify(projectService).getProject(projectOne.getKey());
  }

  @Test
  public void testGetProjectWhenAuthorizedDoesNotExist() throws Exception {
    mvc
        .perform(
            get("/ws/projects/DNE")
                .accept(MediaType.APPLICATION_JSON)
                .with(admin()))
        .andExpect(status().isNotFound())
        .andExpect(content().string("{\"code\":\"NoSuchEntity\",\"parameters\":[\"DNE\"]}"));
  }

  @Test
  public void testAddProjectWhenNotAuthorized() throws Exception {
    mvc
        .perform(
            post("/ws/projects")
                .accept(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized())
        .andExpect(contentEmpty());

    verify(projectService, never()).addProject(any(Project.class));
  }

  @Test
  public void testAddProjectWithMissingFields() throws Exception {
    mvc
        .perform(
            post("/ws/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"keymissing\":\"PRJ1\",\"name\":\"Project One\"}")
                .with(admin()))
        .andExpect(status().isBadRequest())
        .andExpect(contentEmpty());

    verify(projectService, never()).addProject(any(Project.class));
  }

  @Test
  public void testAddProjectWithInvalidSymbolsInProjectKey() throws Exception {
    mvc
        .perform(
            post("/ws/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"P#$%^&*RJ1\",\"name\":\"Project One\"}")
                .with(admin()))
        .andExpect(status().isBadRequest())
        .andExpect(contentEmpty());
  }

  @Test
  public void testAddProjectWithSpaceInProjectKey() throws Exception {
    mvc
        .perform(
            post("/ws/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"P RJ1\",\"name\":\"Project One\"}")
                .with(admin()))
        .andExpect(status().isBadRequest())
        .andExpect(contentEmpty());
  }

  @Test
  public void testAddProjectWhenNotAdmin() throws Exception {
    mvc
        .perform(
            post("/ws/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"PRJ1\",\"name\":\"Project One\"}")
                .with(user(AUTH_ALLOWED_USER)))
        .andExpect(status().isUnauthorized())
        .andExpect(content().string("{\"code\":\"Unauthorized\",\"parameters\":[]}"));

    verify(projectService, never()).addProject(any(Project.class));
  }

  @Test
  public void testAddProjectWhenAdmin() throws Exception {
    mvc
        .perform(
            post("/ws/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"PRJ1\",\"name\":\"Project One\", \"users\": [\"myuser\"]}")
                .with(admin()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/ws/projects/PRJ1"));

    verify(projectService).addProject(any(Project.class));
    verify(releaseService).addSubmission("PRJ1", "Project One");
  }

  @Test
  public void testAddProjectThatAlreadyExists() throws Exception {
    doThrow(new DuplicateKey(mock(CommandResult.class))).when(projectService).addProject(any(Project.class));

    mvc
        .perform(
            post("/ws/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"PRJ1\",\"name\":\"Project One\"}")
                .with(admin()))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("{\"code\":\"AlreadyExists\",\"parameters\":[\"PRJ1\"]}"));

    verify(projectService).addProject(any(Project.class));
    verifyZeroInteractions(releaseService);
  }

  @Test
  public void testUpdateProjectWhenNotAdmin() throws Exception {
    mvc
        .perform(
            post("/ws/projects/PRJ1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"PRJ1\",\"name\":\"Project One\"}")
                .with(user(AUTH_ALLOWED_USER)))
        .andExpect(status().isUnauthorized())
        .andExpect(content().string("{\"code\":\"Unauthorized\",\"parameters\":[]}"));

    verify(projectService, never()).updateProject(any(Project.class));
  }

  @Test
  public void testUpdateProjectWhenAdmin() throws Exception {
    mvc
        .perform(
            post("/ws/projects/PRJ1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"PRJ1\",\"name\":\"Project One\"}")
                .with(admin()))
        .andExpect(status().isOk());

    verify(projectService).updateProject(any(Project.class));
  }

  @Test
  public void testUpdateProjectWrongKey() throws Exception {
    mvc
        .perform(
            post("/ws/projects/PRJ2")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"PRJ1\",\"name\":\"Project One\"}")
                .with(admin()))
        .andExpect(status().isPreconditionFailed());

    verify(projectService, never()).updateProject(any(Project.class));
  }

  @Test
  public void testGetProjectSubmissionsWhenNotAuthorized() throws Exception {
    mvc
        .perform(
            get("/ws/projects/" + projectOne.getKey() + "/releases")
                .accept(MediaType.APPLICATION_JSON)
                .with(unknown()))
        .andExpect(status().isUnauthorized())
        .andExpect(contentEmpty());

    verifyZeroInteractions(releaseService);
    verifyZeroInteractions(projectService);
  }

  @Test
  public void testGetProjectSubmissionsWhenAuthorizedWithoutAccess() throws Exception {
    mvc
        .perform(
            get("/ws/projects/" + projectOne.getKey() + "/releases")
                .accept(MediaType.APPLICATION_JSON)
                .with(user(AUTH_NOT_ALLOWED_USER)))
        .andExpect(status().isNotFound())
        .andExpect(content().string("{\"code\":\"NoSuchEntity\",\"parameters\":[\"PRJ1\"]}"));

    verifyZeroInteractions(releaseService);
    verify(projectService, never()).getProject(any(String.class));
  }

  @Test
  public void testGetProjectSubmissions() throws Exception {
    mvc
        .perform(
            get("/ws/projects/" + projectOne.getKey() + "/releases")
                .accept(MediaType.APPLICATION_JSON)
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(content().string(""
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
            + "]"));

    verify(releaseService).getReleases();
    // verify(projectService).getSubmissions(releases, projectOne.getKey());
  }

}
