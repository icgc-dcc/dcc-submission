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

import static org.icgc.dcc.submission.controller.Responses.noSuchEntityResponse;
import static org.icgc.dcc.submission.controller.Responses.unauthorizedResponse;
import static org.icgc.dcc.submission.core.auth.Authorizations.hasReleaseViewPrivilege;
import static org.icgc.dcc.submission.core.auth.Authorizations.hasSpecificProjectPrivilege;
import static org.icgc.dcc.submission.core.auth.Authorizations.isSuperUser;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.ALREADY_INITIALIZED;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.EMPTY_REQUEST;

import javax.validation.Valid;

import org.icgc.dcc.submission.core.model.Views.Digest;
import org.icgc.dcc.submission.release.model.DetailedSubmission;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.service.ReleaseService;
import org.icgc.dcc.submission.service.SystemService;
import org.icgc.dcc.submission.web.model.ServerErrorResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.annotations.VisibleForTesting;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/ws/releases")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ReleaseController {

  private final ReleaseService releaseService;
  private final SystemService systemService;

  // TODO: This method seems like it should be removed since it is being exposed just for testing
  @PutMapping
  @VisibleForTesting
  public ResponseEntity<?> initialize(
      @Valid @RequestBody Release release,
      Authentication authentication) {
    log.info("Initializing releases with: {}", release);
    if (isSuperUser(authentication) == false) {
      return Responses.unauthorizedResponse();
    }

    if (release != null) {
      val empty = releaseService.countOpenReleases() == 0;
      if (empty) {
        releaseService.createInitialRelease(release);

        return ResponseEntity
            .ok(release);
      } else {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ServerErrorResponseMessage(ALREADY_INITIALIZED));
      }
    } else {
      return ResponseEntity
          .status(HttpStatus.BAD_REQUEST)
          .body(new ServerErrorResponseMessage(EMPTY_REQUEST));
    }
  }

  @GetMapping
  @JsonView(Digest.class)
  public ResponseEntity<?> getReleases(Authentication authentication) {
    log.debug("Getting visible releases");
    if (hasReleaseViewPrivilege(authentication) == false) {
      return unauthorizedResponse();
    }

    val visibileReleases = releaseService.getReleasesBySubject(authentication);

    return ResponseEntity.ok(visibileReleases);
  }

  @GetMapping("{name}")
  public ResponseEntity<?> getReleaseByName(
      @PathVariable("name") String name,
      Authentication authentication) {
    log.debug("Getting release using: {}", name);
    val releaseView = releaseService.getReleaseViewBySubject(name, authentication);

    if (releaseView.isPresent() == false) {
      return noSuchEntityResponse(name);
    }

    val result = releaseView.get();
    result.setLocked(!systemService.isEnabled());

    return ResponseEntity.ok(result);
  }

  @GetMapping("{name}/submissions/{projectKey:.+}")
  public ResponseEntity<?> getSubmission(
      @PathVariable("name") String releaseName,
      @PathVariable("projectKey") String projectKey,
      Authentication authentication) {
    log.debug("Getting detailed submission: {}.{}", releaseName, projectKey);
    if (hasSpecificProjectPrivilege(authentication, projectKey) == false) {
      return Responses.unauthorizedResponse();
    }

    // TODO: use Optional...
    DetailedSubmission detailedSubmission = releaseService.getDetailedSubmission(releaseName, projectKey);
    if (detailedSubmission == null) {
      return noSuchEntityResponse(releaseName, projectKey);
    }

    detailedSubmission.setLocked(!systemService.isEnabled());

    return ResponseEntity.ok(detailedSubmission);
  }

  @GetMapping("{name}/submissions/{projectKey:.*}/report")
  public ResponseEntity<?> getReport(
      @PathVariable("name") String releaseName,
      @PathVariable("projectKey") String projectKey,
      Authentication authentication) {
    log.debug("Getting submission report for: {}.{}", releaseName, projectKey);
    if (hasSpecificProjectPrivilege(authentication, projectKey) == false) {
      return Responses.unauthorizedResponse();
    }

    // TODO: use Optional...
    val submission = releaseService.getSubmission(releaseName, projectKey);
    if (submission == null) {
      return noSuchEntityResponse(releaseName, projectKey);
    }

    // DCC-799: Runtime type will be SubmissionReport. Static type is Object to untangle cyclic dependencies between
    // dcc-submission-server and dcc-submission-core.
    val report = submission.getReport();
    return ResponseEntity.ok(report);
  }

  @GetMapping("{name}/submissions/{projectKey:.+}/report/{fileName:.+}/")
  public ResponseEntity<?> getFileReport(
      @PathVariable("name") String releaseName,
      @PathVariable("projectKey") String projectKey,
      @PathVariable("fileName") String fileName,
      Authentication authentication) {
    log.debug("Getting file report for: {}.{}.{}", new Object[] { releaseName, projectKey, fileName });
    if (hasSpecificProjectPrivilege(authentication, projectKey) == false) {
      return Responses.unauthorizedResponse();
    }

    val fileReport = releaseService.getFileReport(releaseName, projectKey, fileName);
    if (fileReport.isPresent() == false) {
      return noSuchEntityResponse(releaseName, projectKey, fileName);
    }

    return ResponseEntity.ok(fileReport.get());
  }

  @GetMapping("{name}/submissions/{projectKey:.*}/files")
  public ResponseEntity<?> getSubmissionFileList(
      @PathVariable("name") String releaseName,
      @PathVariable("projectKey") String projectKey,
      Authentication authentication) {
    log.debug("Getting submission file list for release {} and project {}", releaseName, projectKey);
    if (hasSpecificProjectPrivilege(authentication, projectKey) == false) {
      return Responses.unauthorizedResponse();
    }

    if (!releaseService.submissionExists(releaseName, projectKey)) {
      return noSuchEntityResponse(releaseName, projectKey);
    }

    val submissionFiles = releaseService.getSubmissionFiles(releaseName, projectKey);
    return ResponseEntity.ok(submissionFiles);
  }

}
