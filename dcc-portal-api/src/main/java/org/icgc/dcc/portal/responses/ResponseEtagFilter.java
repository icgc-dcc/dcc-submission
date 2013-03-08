/*
 * Copyright 2013(c) The Ontario Institute for Cancer Research. All rights reserved.
 * 
 * This program and the accompanying materials are made available under the terms of the GNU Public
 * License v3.0. You should have received a copy of the GNU General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.icgc.dcc.portal.responses;

import com.google.common.hash.Hashing;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;

public class ResponseEtagFilter implements ContainerResponseFilter {

  // TODO Not the best place for this - probably in config file? or pulled from maven?
  private static final String API_VERSION_HEADER = "X-ICGC-Version";
  private static final String VERSION = "1";

  @Override
  public ContainerResponse filter(ContainerRequest containerRequest, ContainerResponse containerResponse) {
    Object entity = containerResponse.getEntity();
    EntityTag etag = generateETag(entity);
    Response.ResponseBuilder rb = containerRequest.evaluatePreconditions(etag);

    Response response = isModified(rb) ? modifiedResponse(containerResponse) : notModifiedResponse(rb);

    containerResponse.setResponse(response);
    return containerResponse;
  }

  private Response notModifiedResponse(Response.ResponseBuilder rb) {
    return rb.header(API_VERSION_HEADER, VERSION).build();
  }

  private boolean isModified(Response.ResponseBuilder rb) {
    return rb != null;
  }

  private EntityTag generateETag(Object entity) {
    return new EntityTag(Hashing.murmur3_128().hashString(entity.toString()).toString());
  }

  private Response modifiedResponse(ContainerResponse containerResponse) {
    return Response.status(containerResponse.getStatusType()).header(API_VERSION_HEADER, VERSION)
        .tag(containerResponse.getHttpHeaders().get("If-None-Match").toString()).entity(containerResponse.getEntity())
        .build();
  }
}
