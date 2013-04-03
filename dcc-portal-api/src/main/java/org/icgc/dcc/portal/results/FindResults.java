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

package org.icgc.dcc.portal.results;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.index.get.GetField;
import org.icgc.dcc.portal.core.JsonUtils;

import java.util.Map;

import static org.icgc.dcc.portal.core.JsonUtils.MAPPER;

@Data
public class FindResults {

  private final String id;

  private final String type;

  private final ObjectNode fields;

  private final JsonNode source;

  public FindResults(GetResponse hit) {
    this.id = hit.getId();
    this.type = hit.getType();
    this.fields = hit.getFields() == null ? null : buildGetHitFields(hit.getFields());
    this.source = hit.getSourceAsString() == null ? null : buildGetHitSource(hit);
  }

  private JsonNode buildGetHitSource(GetResponse source) {
    return JsonUtils.readRequestString(source.getSourceAsString());
  }

  private ObjectNode buildGetHitFields(Map<String, GetField> fields) {
    ObjectNode jNode = MAPPER.createObjectNode();
    for (GetField field : fields.values()) {
      String name = field.getName();
      Object value = field.getValue();
      jNode.putPOJO(name, MAPPER.convertValue(value, JsonNode.class));
      // jNode.set(name, MAPPER.convertValue(value, JsonNode.class));
    }
    return jNode;
  }
}
