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
package org.icgc.dcc.submission.controller;

import static org.icgc.dcc.submission.core.auth.Authorizations.getUsername;
import static org.icgc.dcc.submission.core.auth.Authorizations.hasSpecificProjectPrivilege;
import static org.icgc.dcc.submission.core.auth.Authorizations.isSuperUser;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.net.URI;
import java.util.List;

import javax.validation.Valid;

import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.service.ProjectService;
import org.icgc.dcc.submission.service.ReleaseService;
import org.icgc.dcc.submission.web.model.ServerErrorCode;
import org.icgc.dcc.submission.web.model.ServerErrorResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mongodb.MongoException.DuplicateKey;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/ws/projects")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProjectController {

  private final ProjectService projectService;
  private final ReleaseService releaseService;

  @GetMapping
  public ResponseEntity<?> getProjects(Authentication authentication) {
    log.info("Request for all Projects");

    val user = getUsername(authentication);
    List<Project> projects;

    if (isSuperUser(authentication)) {
      log.info("'{}' is super user", user);
      projects = projectService.getProjects();
    } else {
      log.info("'{}' is not super user", user);
      projects = projectService.getProjectsByUser(user);
    }

    return ResponseEntity.ok(projects);
  }

  @PostMapping
  public ResponseEntity<?> addProject(@Valid @RequestBody Project project, Authentication authentication) {
    log.info("Request to add Project '{}'", project);

    val user = getUsername(authentication);
    if (isSuperUser(authentication) == false) {
      log.warn("'{}' is not super user", user);
      return Responses.unauthorizedResponse();
    }
    log.info("'{}' is super user", user);

    ResponseEntity<?> response;
    try {
      // Save Project to DB
      projectService.addProject(project);

      // Update Release and save to DB
      releaseService.addSubmission(project.getKey(), project.getName());

      response = ResponseEntity.created(getProjectURL(project.getKey())).build();
    } catch (DuplicateKey e) {
      response = ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new ServerErrorResponseMessage(ServerErrorCode.ALREADY_EXISTS, project.getKey()));
      log.warn("Project '{}' already exists! Could NOT be added.", project.getKey());
    }

    return response;
  }

  @GetMapping("{projectKey:.+}")
  public ResponseEntity<?> getProject(@PathVariable("projectKey") String projectKey, Authentication authentication) {
    log.info("Request for Project '{}'", projectKey);

    val user = getUsername(authentication);
    Project project;

    if (hasAccess(authentication, projectKey) == false) {
      log.info("Project '{}' not visible to '{}'", projectKey, user);
      return Responses.notFound(projectKey);
    }

    project = projectService.getProject(projectKey);

    if (project == null) {
      log.info("Project '{}' not found", projectKey);
      return Responses.notFound(projectKey);
    }

    return ResponseEntity.ok(project);
  }

  @PostMapping("{projectKey}")
  public ResponseEntity<?> updateProject(
      @PathVariable("projectKey") String projectKey,
      @Valid @RequestBody Project project,
      Authentication authentication) {
    log.info("Request to update Project '{}' with '{}'", projectKey, project);

    val user = getUsername(authentication);
    if (isSuperUser(authentication) == false) {
      log.warn("'{}' is not super user", user);
      return Responses.unauthorizedResponse();
    }
    log.info("'{}' is super user", user);

    if (!projectKey.equals(project.getKey())) {
      log.warn("Project key '{}' does not match endpoint for '{}'", project.getKey(), projectKey);
      return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body("Project Key Missmatch");
    }

    val result = projectService.updateProject(project);

    return ResponseEntity.ok(result);
  }

  @GetMapping("{projectKey}/releases")
  public ResponseEntity<?> getProjectSubmissions(
      @PathVariable("projectKey") String projectKey,
      Authentication authentication) {
    log.info("Request for all Submissions from Project '{}'", projectKey);

    val user = getUsername(authentication);

    if (hasAccess(authentication, projectKey) == false) {
      log.warn("Project '{}' not visible to '{}'", projectKey, user);
      return Responses.notFound(projectKey);
    }

    val releases = releaseService.getReleases();
    val submissions = projectService.getSubmissions(releases, projectKey);

    return ResponseEntity.ok(submissions);
  }

  private boolean hasAccess(Authentication authentication, String projectKey) {
    return projectKey != null && hasSpecificProjectPrivilege(authentication, projectKey);
  }

  private static URI getProjectURL(String projectKey) {
    val controller = on(ProjectController.class);
    controller.getProject(projectKey, null);
    return fromMethodCall(controller).build().toUri();
  }

}
