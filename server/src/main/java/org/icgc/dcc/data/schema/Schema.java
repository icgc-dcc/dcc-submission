/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
import java.io.InputStream;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

public interface Schema {

  static final JsonFactory FACTORY = new JsonFactory(new ObjectMapper());

  static final ObjectMapper MAPPER = (ObjectMapper) FACTORY.getCodec();

  public enum Type {
    INTEGER, DECIMAL, ARRAY, TEXT, RECORD
  }

  public String getName();

  public Type getType();

  public RecordSchema asRecord();

  public ArraySchema asArray();

  public ValueSchema asValue();

  public void accept(SchemaVisitor visitor);

  public class Parser {

    public Schema parse(InputStream schema) throws JsonParseException, JsonProcessingException, IOException {
      JsonNode node = FACTORY.createJsonParser(schema).readValueAsTree();
      return parseSchemaNode(node);
    }

    public Schema parse(String schema) throws JsonParseException, JsonProcessingException, IOException {
      JsonNode node = new ObjectMapper().readTree(schema);

      return parseSchemaNode(node);
    }

    Schema parseSchemaNode(JsonNode node) {
      if(node.isObject()) {
        String name = readName(node);
        Type type = readType(node);
        BaseSchema schema;
        switch(type) {
        case RECORD:
          schema = new RecordSchema(name);
          break;
        case ARRAY:
          schema = new ArraySchema(name);
          break;
        default:
          schema = new ValueSchema(name, type);
          break;
        }
        schema.fromJson(this, node);
        return schema;
      } else {
        throw new RuntimeException("invalid schema");
      }
    }

    private String readName(JsonNode node) {
      JsonNode nameNode = node.get("name");
      return nameNode != null ? nameNode.getTextValue() : null;
    }

    private Type readType(JsonNode node) {
      String typeName = node.get(BaseSchema.TYPE).getTextValue();
      if(typeName != null) {
        try {
          return Type.valueOf(typeName.toUpperCase());
        } catch(IllegalArgumentException e) {
          // fallthrough
        }
      }
      throw new RuntimeException("invalid type: " + typeName);
    }

  }
}
