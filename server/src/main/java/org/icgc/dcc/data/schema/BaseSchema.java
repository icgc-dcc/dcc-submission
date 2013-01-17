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
import java.io.Serializable;
import java.io.StringWriter;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public abstract class BaseSchema implements Schema, Serializable {

  private static transient JsonFactory factory;

  static final String NAME = "name";

  static final String TYPE = "type";

  private final String name;

  private final Type type;

  protected BaseSchema(String name, Type type) {
    checkArgument(type != null);
    this.name = name;
    this.type = type;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public RecordSchema asRecord() {
    throw new IllegalStateException("not a RecordSchema");
  }

  @Override
  public ArraySchema asArray() {
    throw new IllegalStateException("not an ArraySchema");
  }

  @Override
  public ValueSchema asValue() {
    throw new IllegalStateException("not a ValueSchema");
  }

  @Override
  public String toString() {
    StringWriter sw = new StringWriter();
    try {
      JsonGenerator generator = factory().createJsonGenerator(sw);
      generator.useDefaultPrettyPrinter();
      toJson(generator);
      generator.close();
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
    return sw.toString();
  }

  protected abstract void fromJson(Parser parser, JsonNode node);

  protected void toJson(JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    if(name != null) {
      generator.writeStringField(NAME, name);
    }
    generator.writeStringField(TYPE, type.toString().toLowerCase());
  }

  private static JsonFactory factory() {
    if(factory == null) {
      factory = new JsonFactory(new ObjectMapper());
    }
    return factory;
  }

}
