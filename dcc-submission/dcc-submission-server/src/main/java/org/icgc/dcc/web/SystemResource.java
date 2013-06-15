/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.web;

import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.icgc.dcc.web.Authorizations.isOmnipotentUser;
import static org.icgc.dcc.web.Authorizations.unauthorizedResponse;
import static org.icgc.dcc.web.ServerErrorCode.MISSING_REQUIRED_DATA;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import lombok.extern.slf4j.Slf4j;

import org.codehaus.jackson.JsonNode;
import org.icgc.dcc.core.SystemService;
import org.icgc.dcc.core.model.Status;
import org.icgc.dcc.http.jersey.PATCH;

import com.google.inject.Inject;

/**
 * Endpoint for system related operations.
 * 
 * @see http://stackoverflow.com/questions/6433480/restful-actions-services-that-dont-correspond-to-an-entity
 * @see http://stackoverflow.com/questions/8660003/restful-design-of-a-resource-with-binary-states
 * @see http://stackoverflow.com/questions/8914852/rest-interface-design-for-machine-control
 * @see http://stackoverflow.com/questions/6776198/rest-model-state-transitions
 * @see http
 * ://stackoverflow.com/questions/5591348/how-to-implement-a-restful-resource-for-a-state-machine-or-finite-automata
 */
@Path("/")
@Slf4j
public class SystemResource {

  @Inject
  private SystemService system;

  @GET
  @Path("/systems/sftp")
  public Response getStatus(@Context
  SecurityContext securityContext) {
    log.info("Getting status...");
    if (isOmnipotentUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    Status status = system.getStatus();

    return Response.ok(status).build();
  }

  @PATCH
  @Path("/systems/sftp")
  public Response patch(
      @Context
      SecurityContext securityContext,

      JsonNode state) {
    log.info("Setting SFTP state to {}...", state);
    if (isOmnipotentUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    JsonNode active = state.path("active");
    if (active.isMissingNode()) {
      return status(BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(MISSING_REQUIRED_DATA))
          .build();
    }

    if (active.asBoolean()) {
      system.enableSftp();
    } else {
      system.disableSftp();
    }

    Status status = system.getStatus();

    return ok(status).build();
  }

}
