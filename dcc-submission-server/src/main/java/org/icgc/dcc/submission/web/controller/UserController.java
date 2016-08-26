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
package org.icgc.dcc.submission.web.controller;

import static org.icgc.dcc.submission.core.security.Authorizations.getUsername;
import static org.icgc.dcc.submission.core.security.Authorizations.isSuperUser;
import static org.icgc.dcc.submission.web.ServerErrorCode.NO_SUCH_ENTITY;

import org.icgc.dcc.submission.core.model.Feedback;
import org.icgc.dcc.submission.core.model.User;
import org.icgc.dcc.submission.release.model.DetailedUser;
import org.icgc.dcc.submission.security.Admin;
import org.icgc.dcc.submission.service.MailService;
import org.icgc.dcc.submission.service.UserService;
import org.icgc.dcc.submission.web.ServerErrorResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Resource (REST end-points) for userService.
 */
@Slf4j
@RestController
@RequestMapping("/ws/users")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserController {

  /**
   * Dependencies.
   */
  private final UserService userService;
  private final MailService mailService;

  @GetMapping("self")
  public DetailedUser getResource(Authentication authentication) {
    if (authentication == null) return null;

    val username = getUsername(authentication);
    val admin = isSuperUser(authentication);
    val user = new DetailedUser(username, admin);

    return user;
  }

  @PostMapping("self")
  public void feedback(@RequestBody Feedback feedback) {
    // No authorization check necessary
    log.info("Sending feedback email: {}", feedback);
    mailService.sendSupportFeedback(feedback);
    log.info("Finished feedback email: {}", feedback);
  }

  @Admin
  @PutMapping("unlock/{username:.+}")
  public ResponseEntity<?> unlock(@PathVariable("username") String username, Authentication authentication) {
    log.info("Unlocking user: {}", username);
    val optionalUser = userService.getUserByUsername(username);
    if (optionalUser.isPresent() == false) {
      log.warn("unknown user {} provided", username);
      return ResponseEntity.badRequest()
          .body(new ServerErrorResponseMessage(NO_SUCH_ENTITY, username));
    } else {
      User user = optionalUser.get();
      if (user.isLocked()) {
        user = userService.resetUser(user);
        log.info("user {} was unlocked", username);
      } else {
        log.warn("user {} was not locked, aborting unlocking procedure", username);
      }

      return ResponseEntity.ok(user);
    }
  }

}
