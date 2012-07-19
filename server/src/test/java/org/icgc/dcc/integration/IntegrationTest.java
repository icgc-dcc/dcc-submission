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
package org.icgc.dcc.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.internal.util.Base64;
import org.icgc.dcc.release.model.Release;
import org.junit.Test;

/**
 * 
 */
public class IntegrationTest {
  private Client client;

  private final String baseURI = "http://localhost:5380/ws/releases";

  private WebTarget target;

  public class DCCHttpBasicAuthFilter implements ClientRequestFilter {

    private final String authentication;

    public DCCHttpBasicAuthFilter(final String username, final String password) {
      authentication = "X-DCC-Auth " + Base64.encodeAsString(username + ":" + password);
    }

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
      if(!requestContext.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
        requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authentication);
      }
    }

  }

  @Test
  public void test_release_resource() {
    // start the web server
    // new Main().main(null);
    // create client for the server
    this.client = ClientFactory.newClient();
    this.target = this.client.target(baseURI);
    this.target.configuration().register(new DCCHttpBasicAuthFilter("admin", "adminspasswd"));

    // get release1
    this.target = this.client.target(baseURI).path("/{name}");
    Release release = this.target.pathParam("name", "release1").request(MediaType.APPLICATION_JSON).get(Release.class);
    assertEquals("release1", release.getName());
    // get release list request
    GenericType<List<Release>> list = new GenericType<List<Release>>() {
    };
    List<Release> result = target.request(MediaType.APPLICATION_JSON).get(list);
    assertTrue(!result.isEmpty());
  }

}
