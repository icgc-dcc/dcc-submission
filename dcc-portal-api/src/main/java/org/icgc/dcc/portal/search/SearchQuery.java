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

package org.icgc.dcc.portal.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.icgc.dcc.portal.responses.ErrorResponse;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;

@Data
@XmlRootElement(name = "SearchQuery")
public class SearchQuery {

  static final int DEFAULT_SIZE = 10;
  static final int MAX_SIZE = 100;

  @JsonProperty
  QueryBuilder query;

  @JsonProperty
  JsonNode filters;

  @JsonProperty
  String facets;

  @JsonProperty
  String[] fields;

  @JsonProperty
  String score;

  @Min(1)
  @JsonProperty
  Integer from;

  @Min(1)
  @Max(MAX_SIZE)
  @JsonProperty
  Integer size;

  @JsonProperty
  String sort;

  @JsonProperty
  SortOrder order;

  public SearchQuery(final int from, final int size) {
    // Save as 1-index
    this.from = from < 1 ? 1 : from;
    // Prevent massive requests
    this.size = size == 0 ? DEFAULT_SIZE : size > MAX_SIZE ? MAX_SIZE : size;
  }

  public SearchQuery(final int from, final int size, final String sort, final String order) {
    // Save as 1-index
    this.from = from < 1 ? 1 : from;
    // Prevent massive requests
    this.size = size == 0 ? DEFAULT_SIZE : size > MAX_SIZE ? MAX_SIZE : size;
    this.sort = sort;
    this.order = SortOrder.valueOf(order.toUpperCase());
  }

  public SearchQuery(String filters, String score, int from, int size, String sort, String order) {
    // Save as 1-index
    this.from = from < 1 ? 1 : from;
    // Prevent massive requests
    this.size = size == 0 ? DEFAULT_SIZE : size > MAX_SIZE ? MAX_SIZE : size;
    this.sort = sort;
    this.order = SortOrder.valueOf(order.toUpperCase());

    this.filters = filters == null ? null : jsonifyString(filters);
    this.score = score;
  }

  // For the user this.from should be 1-index, but ES is 0-index
  // Set here instead of cstr so it works with both get and post
  public final int getFrom() {
    return this.from == 0 ? 0 : this.from - 1;
  }

  JsonNode jsonifyString(String filters) {
    try {
      return new ObjectMapper().readValue(filters, JsonNode.class);
    } catch (IOException e) {
      throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse(Response.Status.BAD_REQUEST, e)).type(MediaType.APPLICATION_JSON_TYPE).build());
    }
  }
}
