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

package org.icgc.dcc.portal.core;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.icgc.dcc.portal.responses.ErrorResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLDecoder;

public final class JsonUtils {
  public static ObjectMapper MAPPER = new ObjectMapper();

  private JsonUtils() {}

  public static JsonNode readRequestString(String filters) {
    String wrappedFilters = filters.replaceFirst("^\\{?", "{").replaceFirst("}?$", "}");
    try {
      return MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
          .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
          .readValue(URLDecoder.decode(wrappedFilters, "UTF-8"), JsonNode.class);
    } catch (IOException e) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse(Response.Status.BAD_REQUEST, e)).type(MediaType.APPLICATION_JSON_TYPE).build());
    }
  }

}
