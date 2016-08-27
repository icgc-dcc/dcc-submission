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

import static org.icgc.dcc.submission.server.web.ServerErrorCode.MISSING_REQUIRED_DATA;

import org.icgc.dcc.submission.core.model.Status;
import org.icgc.dcc.submission.server.security.Admin;
import org.icgc.dcc.submission.server.service.SystemService;
import org.icgc.dcc.submission.server.web.ServerErrorResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Endpoint for systemService related operations.
 * 
 * @see http://stackoverflow.com/questions/6433480/restful-actions-services-that-dont-correspond-to-an-entity
 * @see http://stackoverflow.com/questions/8660003/restful-design-of-a-resource-with-binary-states
 * @see http://stackoverflow.com/questions/8914852/rest-interface-design-for-machine-control
 * @see http://stackoverflow.com/questions/6776198/rest-model-state-transitions
 * @see http
 * ://stackoverflow.com/questions/5591348/how-to-implement-a-restful-resource-for-a-state-machine-or-finite-automata
 */
@Slf4j
@RestController
@RequestMapping("/ws/systems")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SystemController {

  private final SystemService systemService;

  @Admin
  @GetMapping
  public ResponseEntity<?> getStatus() {
    log.info("Getting status...");
    Status status = systemService.getStatus();

    return ResponseEntity.ok(status);
  }

  @Admin
  @PatchMapping
  public ResponseEntity<?> patch(@RequestBody JsonNode state) {
    log.info("Setting SFTP state to {}...", state);
    JsonNode active = state.path("active");
    if (active.isMissingNode()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new ServerErrorResponseMessage(MISSING_REQUIRED_DATA));
    }

    if (active.asBoolean()) {
      systemService.enable();
    } else {
      systemService.disable();
    }

    Status status = systemService.getStatus();

    return ResponseEntity.ok(status);
  }

}
