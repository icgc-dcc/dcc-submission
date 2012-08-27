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
import java.util.List;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import com.google.common.collect.Lists;

/**
 * A union of many {@code RecordSchema}. Note that this differs from Avro's {@code Union}: the value of a union in Avro
 * is one of the possible records, whereas a value of a {@code UnionSchema} is all {@code RecordSchema} merged into one.
 */
public class UnionSchema extends BaseSchema {

  private static final String RECORDS = "records";

  private final List<RecordSchema> records = Lists.newArrayList();

  public UnionSchema(String name) {
    super(name, Type.UNION);
    // A union cannot be anonymous
    checkArgument(name != null);
  }

  @Override
  public void accept(SchemaVisitor visitor) {
    visitor.visit(this);
    for(RecordSchema schema : records) {
      schema.accept(visitor);
    }
  }

  @Override
  public UnionSchema asUnion() {
    return this;
  }

  public List<RecordSchema> getRecords() {
    return Collections.unmodifiableList(records);
  }

  @Override
  protected void fromJson(Parser parser, JsonNode node) {
    JsonNode records = node.get(RECORDS);
    if(records == null || records.isArray() == false) {
      throw new RuntimeException("invalid schema. records is not an array.");
    }
    for(JsonNode record : records) {
      this.records.add(parser.parseSchemaNode(record).asRecord());
    }
  }

  @Override
  protected void toJson(JsonGenerator generator) throws IOException {
    super.toJson(generator);
    if(records.size() > 0) {
      generator.writeArrayFieldStart(RECORDS);
      for(RecordSchema schema : records) {
        schema.toJson(generator);
      }
      generator.writeEndArray();
    }
    generator.writeEndObject();
  }

}
