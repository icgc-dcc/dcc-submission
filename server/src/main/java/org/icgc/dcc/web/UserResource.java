/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.icgc.dcc.core.UserService;
import org.icgc.dcc.core.model.Feedback;
import org.icgc.dcc.core.model.User;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;

import com.google.inject.Inject;

/**
 * 
 */
@Path("users/self")
public class UserResource {

  @Inject
  private UserService users;

  @Inject
  private UsernamePasswordAuthenticator passwordAuthenticator;

  @GET
  public Response getRoles(@Context HttpHeaders headers) {

    String username = passwordAuthenticator.getCurrentUser();
    User user = users.getUser(username);

    if(user == null) {
      user = new User();
      user.setUsername(username);
      users.saveUser(user);
    }

    user.getRoles().addAll(this.passwordAuthenticator.getRoles());

    return Response.ok(user).build();
  }

  @POST
  @Consumes("application/json")
  public Response feedback(Feedback feedback, @Context Request req) {
    Properties props = new Properties();
    props.put("mail.smtp.host", "smtp.oicr.on.ca");
    Session session = Session.getDefaultInstance(props, null);
    try {
      Message msg = new MimeMessage(session);
      msg.setFrom(new InternetAddress(feedback.getEmail()));

      msg.setSubject(feedback.getSubject());
      msg.setText(feedback.getMessage());
      msg.addRecipient(Message.RecipientType.TO, new InternetAddress("shane.wilson@oicr.on.ca"));

      Transport.send(msg);
    } catch(AddressException e) {
      System.out.println("an error occured while emailing: " + e);
    } catch(MessagingException e) {
      System.out.println("an error occured while emailing: " + e);
    }

    return Response.ok().build();
  }

}
