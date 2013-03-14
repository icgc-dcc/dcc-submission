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

package org.icgc.dcc.portal.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import lombok.Data;
import org.icgc.dcc.portal.core.JsonUtils;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.xml.bind.annotation.XmlRootElement;

import static org.icgc.dcc.portal.core.JsonUtils.MAPPER;

@Data
@XmlRootElement(name = "RequestSearchQuery")
public class RequestSearchQuery {

  static final int DEFAULT_SIZE = 10;

  static final int MAX_SIZE = 100;

  @JsonProperty
  JsonNode filters;

  @JsonProperty
  String[] fields;

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
  String order;

  public RequestSearchQuery(String filters, String fields, int from, int size, String sort, String order) {
    // Save as 0-base index where 0 and 1 are 0
    this.from = from < 2 ? 0 : from - 1;
    // Prevent massive requests
    this.size = size < 1 ? DEFAULT_SIZE : size > MAX_SIZE ? MAX_SIZE : size;
    this.sort = sort;
    this.order = order.toUpperCase();

    this.filters = Strings.isNullOrEmpty(filters) ? MAPPER.createObjectNode() : JsonUtils.readRequestString(filters);
    this.fields = Strings.isNullOrEmpty(fields) ? new String[] {} : fields.split(",\\ ?");
  }
}
