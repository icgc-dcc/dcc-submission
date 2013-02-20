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

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.SecurityContext;

import org.icgc.dcc.core.UserService;
import org.icgc.dcc.core.model.Feedback;
import org.icgc.dcc.core.model.User;
import org.icgc.dcc.release.model.DetailedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import static org.icgc.dcc.web.Authorizations.isOmnipotentUser;
import static org.icgc.dcc.web.Authorizations.unauthorizedResponse;

/**
 * Resource (REST end-points) for users.
 */
@Path("users")
public class UserResource {

  private static final Logger log = LoggerFactory.getLogger(UserResource.class);

  @Inject
  private UserService users;

  @GET
  @Path("self")
  public Response getResource(@Context SecurityContext securityContext) {
    String username = Authorizations.getUsername(securityContext);
    boolean admin = isOmnipotentUser(securityContext);
    return Response.ok(new DetailedUser(username, admin)).build();
  }

  @POST
  @Path("self")
  @Consumes("application/json")
  public Response feedback(Feedback feedback) { // TODO: merge with mail service (DCC-686)
    /* no authorization check necessary */

    log.debug("Sending feedback email: {}", feedback);
    Properties props = new Properties();
    props.put("mail.smtp.host", "smtp.oicr.on.ca");
    Session session = Session.getDefaultInstance(props, null);
    try {
      Message msg = new MimeMessage(session);
      msg.setFrom(new InternetAddress(feedback.getEmail()));

      msg.setSubject(feedback.getSubject());
      msg.setText(feedback.getMessage());
      msg.addRecipient(Message.RecipientType.TO, new InternetAddress("dcc@lists.oicr.on.ca"));

      Transport.send(msg);
    } catch(AddressException e) {
      log.error("an error occured while emailing: " + e);
    } catch(MessagingException e) {
      log.error("an error occured while emailing: " + e);
    }

    return Response.ok().build();
  }

  @PUT
  @Path("unlock/{username}")
  public Response unlock(@PathParam("username") String username, @Context SecurityContext securityContext) {

    log.info("Unlocking user: {}", username);
    if(isOmnipotentUser(securityContext) == false) {
      return unauthorizedResponse();
    }

    Optional<User> optionalUser = users.getUserByUsername(username);
    if(optionalUser.isPresent() == false) {
      log.warn("unknown user {} provided", username);
      return Response.status(Status.BAD_REQUEST)
          .entity(new ServerErrorResponseMessage(ServerErrorCode.NO_SUCH_ENTITY, new Object[] { username })).build();
    } else {
      User user = optionalUser.get();
      if(user.isLocked()) {
        user = users.resetUser(user);
        log.info("user {} was unlocked", username);
      } else {
        log.warn("user {} was not locked, aborting unlocking procedure", username);
      }
      return ResponseTimestamper.ok(user).build();
    }
  }
}
