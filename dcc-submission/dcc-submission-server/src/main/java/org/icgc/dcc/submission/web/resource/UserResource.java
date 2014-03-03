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
package org.icgc.dcc.submission.web.resource;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.icgc.dcc.submission.web.model.ServerErrorCode.NO_SUCH_ENTITY;
import static org.icgc.dcc.submission.web.util.Authorizations.isSuperUser;
import static org.icgc.dcc.submission.web.util.Responses.unauthorizedResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.model.Feedback;
import org.icgc.dcc.submission.core.model.User;
import org.icgc.dcc.submission.release.model.DetailedUser;
import org.icgc.dcc.submission.service.MailService;
import org.icgc.dcc.submission.service.UserService;
import org.icgc.dcc.submission.web.model.ServerErrorResponseMessage;
import org.icgc.dcc.submission.web.util.Authorizations;
import org.icgc.dcc.submission.web.util.ResponseTimestamper;

import com.google.inject.Inject;

/**
 * Resource (REST end-points) for userService.
 */
@Slf4j
@Path("users")
public class UserResource {

  @Inject
  private UserService userService;

  @Inject
  private MailService mailService;

  @GET
  @Path("self")
  public Response getResource(

      @Context SecurityContext securityContext

      )
  {
    val username = Authorizations.getUsername(securityContext);
    val admin = isSuperUser(securityContext);
    val user = new DetailedUser(username, admin);

    return Response.ok(user).build();
  }

  @POST
  @Path("self")
  @Consumes("application/json")
  public Response feedback(Feedback feedback) {
    // No authorization check necessary
    log.info("Sending feedback email: {}", feedback);
    mailService.sendSupportFeedback(feedback);
    log.info("Finished feedback email: {}", feedback);

    return Response.ok().build();
  }

  @PUT
  @Path("unlock/{username}")
  public Response unlock(

      @PathParam("username") String username,

      @Context SecurityContext securityContext

      )
  {
    log.info("Unlocking user: {}", username);
    if (isSuperUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    val optionalUser = userService.getUserByUsername(username);
    if (optionalUser.isPresent() == false) {
      log.warn("unknown user {} provided", username);
      return Response.
          status(BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(NO_SUCH_ENTITY, username))
          .build();
    } else {
      User user = optionalUser.get();
      if (user.isLocked()) {
        user = userService.resetUser(user);
        log.info("user {} was unlocked", username);
      } else {
        log.warn("user {} was not locked, aborting unlocking procedure", username);
      }

      return ResponseTimestamper.ok(user).build();
    }
  }

}
