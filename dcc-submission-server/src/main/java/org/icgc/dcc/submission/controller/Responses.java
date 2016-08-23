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

import static org.icgc.dcc.submission.web.model.ServerErrorCode.NO_SUCH_ENTITY;

import org.icgc.dcc.submission.web.model.ServerErrorCode;
import org.icgc.dcc.submission.web.model.ServerErrorResponseMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Responses {

  public static ResponseEntity<?> unauthorizedResponse() {
    return unauthorizedResponse(false);
  }

  public static ResponseEntity<?> unauthorizedResponse(boolean important) {
    ServerErrorResponseMessage errorMessage = new ServerErrorResponseMessage(ServerErrorCode.UNAUTHORIZED);
    if (important) {
      log.info("unauthorized action: {}", errorMessage);
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorMessage);
  }

  public static ResponseEntity<?> badRequest(ServerErrorCode code, Object... args) {
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(new ServerErrorResponseMessage(code, args));
  }

  public static ResponseEntity<?> created() {
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .build();
  }

  public static ResponseEntity<?> noContent() {
    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }

  public static ResponseEntity<?> notFound(String name) {
    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(new ServerErrorResponseMessage(NO_SUCH_ENTITY, name));
  }

  public static ResponseEntity<?> noSuchEntityResponse(String... names) {
    return noSuchEntityResponse(false, names);
  }

  public static ResponseEntity<?> noSuchEntityResponse(boolean important, String... names) {
    ServerErrorResponseMessage errorMessage =
        new ServerErrorResponseMessage(NO_SUCH_ENTITY, (Object[]) names);
    if (important) {
      log.info("No such entity: {}", errorMessage);
    }
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
  }

}
