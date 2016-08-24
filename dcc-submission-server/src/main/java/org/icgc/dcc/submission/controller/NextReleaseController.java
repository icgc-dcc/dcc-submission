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

import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.submission.controller.Responses.badRequest;
import static org.icgc.dcc.submission.controller.Responses.noContent;
import static org.icgc.dcc.submission.controller.Responses.unauthorizedResponse;
import static org.icgc.dcc.submission.core.auth.Authorizations.hasReleaseClosePrivilege;
import static org.icgc.dcc.submission.core.auth.Authorizations.hasReleaseViewPrivilege;
import static org.icgc.dcc.submission.core.auth.Authorizations.hasSpecificProjectPrivilege;
import static org.icgc.dcc.submission.core.auth.Authorizations.hasSubmissionSignoffPrivilege;
import static org.icgc.dcc.submission.core.auth.Authorizations.isSuperUser;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.INVALID_STATE;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.RELEASE_EXCEPTION;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.UNAVAILABLE;

import java.util.List;

import javax.validation.Valid;

import org.icgc.dcc.submission.core.InvalidStateException;
import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.icgc.dcc.submission.core.model.DccModelOptimisticLockException;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.release.ReleaseException;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.service.ReleaseService;
import org.icgc.dcc.submission.service.SystemService;
import org.icgc.dcc.submission.service.ValidationService;
import org.icgc.dcc.submission.web.model.ServerErrorCode;
import org.icgc.dcc.submission.web.model.ServerErrorResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/ws/nextRelease")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NextReleaseController {

  private static final Joiner JOINER = Joiner.on("/");

  /**
   * Dependencies.
   */
  private final SubmissionProperties properties;
  private final ReleaseService releaseService;
  private final ValidationService validationScheduler;
  private final SystemService systemService;

  @GetMapping
  public ResponseEntity<?> getNextRelease(Authentication authentication) {
    log.debug("Getting nextRelease");
    if (hasReleaseViewPrivilege(authentication) == false) {
      return unauthorizedResponse();
    }

    String prefix = properties.getHttp().getPath();
    String redirectionPath = JOINER.join(
        prefix,
        "releases",
        releaseService.getNextRelease() // guaranteed not to be null
            .getName());

    return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).header(HttpHeaders.LOCATION, redirectionPath).build();
  }

  /**
   * Returns the current dictionary.
   * <p>
   * More: <code>{@link ReleaseService#getNextDictionary()}</code><br/>
   * Open-access intentional (DCC-758)
   */
  @GetMapping("dictionary")
  public Dictionary getDictionary() {
    return releaseService.getNextDictionary();
  }

  @PostMapping
  public ResponseEntity<?> release(@RequestBody Release nextRelease, Authentication authentication) {
    log.info("Releasing nextRelease, new release will be: {}", nextRelease);

    // TODO: This is intentionally not validated, since we're only currently using the name. This seems sketchy to me
    if (hasReleaseClosePrivilege(authentication) == false) {
      return unauthorizedResponse();
    }

    Release release = releaseService.getNextRelease(); // guaranteed not null
    String oldReleaseName = release.getName();
    log.info("Releasing {}", oldReleaseName);

    Release newRelease = null;
    try {
      newRelease = releaseService.performRelease(nextRelease.getName());
      log.info("Released {}", oldReleaseName);
    } catch (ReleaseException e) {
      ServerErrorCode code = RELEASE_EXCEPTION;
      log.error(code.getFrontEndString(), e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerErrorResponseMessage(code));
    } catch (InvalidStateException e) {
      ServerErrorCode code = e.getCode();
      log.error(code.getFrontEndString(), e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerErrorResponseMessage(code));
    }
    return ResponseEntity.ok(newRelease);
  }

  @GetMapping("queue")
  public ResponseEntity<?> getQueue() {
    // No authorization check needed, but authentication required (see DCC-808)
    log.debug("Getting the queue for nextRelease");
    List<String> projectIds = releaseService.getQueuedProjectKeys();
    Object[] projectIdArray = projectIds.toArray();

    return ResponseEntity.ok(projectIdArray);
  }

  @PostMapping("queue")
  public ResponseEntity<?> queue(@Valid @RequestBody List<QueuedProject> queuedProjects,
      Authentication authentication) {
    log.info("Enqueuing projects for nextRelease: {}", queuedProjects);

    if (!isAccessible(authentication)) {
      log.warn("Not accessible to '{}'", authentication.getName());
      return unauthorizedResponse();
    }

    List<String> projectKeys = newArrayList();
    for (val queuedProject : queuedProjects) {
      val projectKey = queuedProject.getKey();
      if (hasSpecificProjectPrivilege(authentication, projectKey) == false) {
        return unauthorizedResponse();
      }

      projectKeys.add(projectKey);
    }

    try {
      releaseService.queueSubmissions(queuedProjects);
    } catch (ReleaseException e) {
      log.error("Error trying to queue submission(s)", e);

      return badRequest(INVALID_STATE, projectKeys);
    } catch (InvalidStateException e) {
      val code = e.getCode();
      val offendingState = e.getState();
      log.error(code.getFrontEndString(), e);

      return badRequest(code, offendingState);
    } catch (DccModelOptimisticLockException e) {
      // Not very likely
      val code = UNAVAILABLE;
      log.error(code.getFrontEndString(), e);

      return ResponseEntity
          .status(HttpStatus.SERVICE_UNAVAILABLE)
          .header("Retry-After", "3")
          .body(new ServerErrorResponseMessage(code));
    }

    return noContent();
  }

  @DeleteMapping("queue")
  public ResponseEntity<?> removeAllQueued(Authentication authentication) {
    log.info("Removing all queued projects");
    if (isSuperUser(authentication) == false) {
      return unauthorizedResponse();
    }

    try {
      releaseService.removeQueuedSubmissions();
    } catch (InvalidStateException e) {
      ServerErrorCode code = e.getCode();
      log.error(code.getFrontEndString(), e);
      return badRequest(code, e.getMessage());
    } catch (Throwable t) {
      log.error("Error removing all queued projects:", t);
      throw t;
    }

    return ResponseEntity.ok().build();
  }

  @SneakyThrows
  @DeleteMapping("validation/{projectKey:.+}")
  public ResponseEntity<?> cancelValidation(@PathVariable("projectKey") String projectKey,
      Authentication authentication) {
    log.info("Cancelling validation for {}", projectKey);
    if (!hasSpecificProjectPrivilege(authentication, projectKey)) {
      return unauthorizedResponse();
    }

    try {
      validationScheduler.cancelValidation(projectKey);
    } catch (InvalidStateException e) {
      ServerErrorCode code = e.getCode();
      log.error(code.getFrontEndString(), e);
      return badRequest(code, e.getMessage());
    } catch (Throwable t) {
      log.error("Error cancelling validation for '" + projectKey + "':", t);
      throw t;
    }

    return ResponseEntity.ok().build();
  }

  @SuperUser
  @SneakyThrows
  @DeleteMapping("state/{projectKey:.+}")
  public ResponseEntity<?> resetState(@PathVariable("projectKey") String projectKey) {
    log.info("Resetting state for '{}'", projectKey);

    try {
      releaseService.resetSubmissions(ImmutableList.<String> of(projectKey));
    } catch (Throwable t) {
      log.error("Error resetting state for '" + projectKey + "':", t);
      throw t;
    }

    return ResponseEntity.ok().build();
  }

  @GetMapping("signed")
  public ResponseEntity<?> getSignedOff() {
    /* no authorization check needed (see DCC-808) */

    log.debug("Getting signed off projects for nextRelease");
    List<String> projectIds = releaseService.getSignedOffReleases();
    return ResponseEntity.ok(projectIds.toArray());
  }

  @PostMapping("signed")
  public ResponseEntity<?> signOff(@RequestBody List<String> projectKeys, Authentication authentication) {
    log.info("Signing off projects {}", projectKeys);

    if (!isAccessible(authentication)) {
      log.warn("Not accessible to '{}'", authentication.getName());
      return unauthorizedResponse();
    }

    if (hasSubmissionSignoffPrivilege(authentication) == false) {
      return unauthorizedResponse();
    }

    try {
      String username = authentication.getName();
      releaseService.signOffRelease(projectKeys, username);
    } catch (ReleaseException e) {
      ServerErrorCode code = ServerErrorCode.NO_SUCH_ENTITY;
      log.error(code.getFrontEndString(), e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerErrorResponseMessage(code, projectKeys));
    } catch (InvalidStateException e) {
      ServerErrorCode code = e.getCode();
      log.error(code.getFrontEndString(), e);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ServerErrorResponseMessage(code));
    } catch (DccModelOptimisticLockException e) { // not very likely
      ServerErrorCode code = ServerErrorCode.UNAVAILABLE;
      log.error(code.getFrontEndString(), e);
      return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
          .header("Retry-After", "3")
          .body(new ServerErrorResponseMessage(code));
    }

    return ResponseEntity.ok().build();
  }

  /**
   * See {@link ReleaseService#update(Release)}.
   */
  @SuperUser
  @PutMapping("update")
  public ResponseEntity<?> update(@Valid @RequestBody Release release, Authentication authentication) {
    log.info("Updating nextRelease with: {}", release);
    if (release != null) {
      String name = release.getName();

      log.info("updating {}", name);
      val empty = releaseService.countReleases() == 0;
      if (empty) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
      } else {
        val updatedName = release.getName();
        val updatedDictionaryVersion = release.getDictionaryVersion();
        val updatedRelease = releaseService.updateRelease(updatedName, updatedDictionaryVersion);
        log.info("updated {}", name);

        return ResponseEntity.ok(updatedRelease);
      }
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  private boolean isAccessible(Authentication authentication) {
    return systemService.isEnabled() || isSuperUser(authentication);
  }

}
