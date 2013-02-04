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

package org.icgc.dcc.search;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@XmlRootElement(name = "SearchQuery")
public class SearchQuery {

  private static final int DEFAULT_SIZE = 10;
  private static final int MAX_SIZE = 100;

  @JsonProperty
  private Query query;

  @JsonProperty
  private Filters filters;

  @JsonProperty
  private Facets facets;

  @Min(1)
  @JsonProperty
  private int from;

  @Min(1)
  @Max(100)
  @JsonProperty
  private int size;

  @JsonProperty
  private String sort;

  @JsonProperty
  private String order;

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
    // param enum thing
    this.order = order;
  }

  // For the user this.from should be 1-index, but ES is 0-index
  // Set here instead of cstr so it works with both get and post
  public final int getFrom() {
    return this.from == 0 ? 0 : this.from - 1;
  }

  @Data
  @XmlRootElement(name = "Query")
  private static class Query {
    @JsonProperty
    private String name;
  }

  @Data
  @XmlRootElement(name = "Filters")
  private static class Filters {
    @JsonProperty
    private String name;
  }

  @Data
  @XmlRootElement(name = "Facets")
  private static class Facets {
    @JsonProperty
    private String name;
  }
}
