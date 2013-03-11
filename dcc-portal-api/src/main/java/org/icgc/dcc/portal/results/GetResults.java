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

import com.google.common.collect.ImmutableList;
import lombok.Data;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.index.get.GetField;
import org.icgc.dcc.portal.responses.ResponseHitField;

import java.util.Map;

@Data
public class GetResults {

  private final String id;
  private final String type;
  private final ImmutableList<ResponseHitField> fields;

  public GetResults(GetResponse hit) {
    this.id = hit.getId();
    this.type = hit.getType();
    this.fields = buildGetHitFields(hit.getFields());
  }

  private ImmutableList<ResponseHitField> buildGetHitFields(Map<String, GetField> fields) {
    ImmutableList.Builder<ResponseHitField> l = new ImmutableList.Builder<ResponseHitField>();
    for (GetField field : fields.values()) {
      String name = field.getName();
      Object value = field.getValues().toArray()[0];
      ResponseHitField rhf = new ResponseHitField(name, value);
      l.add(rhf);
    }
    return l.build();
  }
}
