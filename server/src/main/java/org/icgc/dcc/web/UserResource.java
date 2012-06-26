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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.icgc.dcc.model.User;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;
import org.icgc.dcc.service.UserService;

import com.google.inject.Inject;
import com.sun.jersey.core.util.Base64;

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
    String auth = headers.getRequestHeader("authorization").get(0);

    auth = auth.substring("Basic ".length());
    String[] values = new String(Base64.base64Decode(auth)).split(":", -1);
    String username = values[0];
    String password = values.length > 1 ? values[1] : "";

    if(passwordAuthenticator.authenticate(username, password.toCharArray(), null)) {

      User user = users.getUser(username);
      if(user != null) {
        return Response.ok(user).build();
      }
      return Response.status(Status.NOT_FOUND).build();
    }
    return Response.status(Status.UNAUTHORIZED).build();
  }

}
