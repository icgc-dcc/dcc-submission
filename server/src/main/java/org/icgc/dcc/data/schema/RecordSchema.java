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

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class RecordSchema extends HasFileSchemaSchema {

  private static final String FIELDS = "fields";

  private static final String METADATASCHEMA = "metadataSchema";

  private static final String COLLECTION = "collection";

  private final List<Schema> fields = Lists.newArrayList();

  private String metadataSchema;

  private String collection;

  public RecordSchema(String name) {
    super(name, Type.RECORD);
    // A record cannot be anonymous
    checkArgument(name != null);
  }

  @Override
  public void accept(SchemaVisitor visitor) {
    visitor.visit(this);
    for(Schema schema : fields) {
      schema.accept(visitor);
    }
  }

  @Override
  public RecordSchema asRecord() {
    return this;
  }

  public String getMetadataSchema() {
    return metadataSchema;
  }

  public String getCollection() {
    return collection;
  }

  public List<Schema> getFields() {
    return Collections.unmodifiableList(fields);
  }

  public List<Schema> getFields(Type type) {
    return getFields(EnumSet.of(type));
  }

  public List<Schema> getFields(final EnumSet<Schema.Type> types) {
    return ImmutableList.copyOf(Iterables.filter(fields, new Predicate<Schema>() {

      @Override
      public boolean apply(Schema input) {
        return types.contains(input.getType());
      }
    }));
  }

  @Override
  protected void fromJson(Parser parser, JsonNode node) {
    super.fromJson(parser, node);
    JsonNode fields = node.get(FIELDS);
    if(fields == null || fields.isArray() == false) {
      throw new RuntimeException("invalid schema. fields is not an array.");
    }
    for(JsonNode field : fields) {
      this.fields.add(parser.parseSchemaNode(field));
    }
    JsonNode meta = node.get(METADATASCHEMA);
    if(meta != null) {
      this.metadataSchema = meta.getTextValue();
    }
    JsonNode c = node.get(COLLECTION);
    if(c != null) {
      this.collection = c.getTextValue();
    }
  }

  @Override
  protected void toJson(JsonGenerator generator) throws IOException {
    super.toJson(generator);
    if(metadataSchema != null) {
      generator.writeStringField(METADATASCHEMA, metadataSchema);
    }
    if(collection != null) {
      generator.writeStringField(COLLECTION, collection);
    }
    if(fields.size() > 0) {
      generator.writeArrayFieldStart(FIELDS);
      for(Schema schema : fields) {
        ((BaseSchema) schema).toJson(generator);
      }
      generator.writeEndArray();
    }
    generator.writeEndObject();
  }

}
