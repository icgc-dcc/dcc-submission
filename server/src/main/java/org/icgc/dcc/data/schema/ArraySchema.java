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
package org.icgc.dcc.data.schema;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

public class ArraySchema extends BaseSchema {

  private static final String ITEMS = "items";

  private Schema items;

  public ArraySchema(String name) {
    super(name, Type.ARRAY);
  }

  public Schema getItems() {
    return items;
  }

  @Override
  public void accept(SchemaVisitor visitor) {
    visitor.visit(this);
    items.accept(visitor);
  }

  @Override
  public ArraySchema asArray() {
    return this;
  }

  @Override
  protected void fromJson(Parser parser, JsonNode node) {
    JsonNode itemsNode = node.get(ITEMS);
    if(itemsNode == null || itemsNode.isObject() == false) {
      throw new RuntimeException("invalid schema. items is not a schema.");
    }
    this.items = parser.parseSchemaNode(node.get(ITEMS));
  }

  @Override
  protected void toJson(JsonGenerator generator) throws IOException {
    super.toJson(generator);
    generator.writeFieldName(ITEMS);
    ((BaseSchema) items).toJson(generator);
    generator.writeEndObject();
  }

}
