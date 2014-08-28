/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.core.util;

import static com.google.common.base.Preconditions.checkState;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.util.Joiners.INDENT;
import static org.icgc.dcc.core.util.Splitters.NEWLINE;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;

/**
 * Common object mappers.
 */
@NoArgsConstructor(access = PRIVATE)
public final class Jackson {

  public static final ObjectMapper DEFAULT = new ObjectMapper();
  public static final ObjectWriter PRETTY_WRITTER = DEFAULT.writerWithDefaultPrettyPrinter();

  /**
   * Do not expose outside of this class, see {@link #getRootArray(URL)}.
   */
  private static final ObjectReader JSON_NODE_READER = DEFAULT.reader(JsonNode.class);

  @SneakyThrows
  public static JsonNode readFile(@NonNull final File file) {
    return DEFAULT.readTree(file);
  }

  /**
   * TODO: use convert() rather.
   */
  public static Map<String, String> asMap(@NonNull final ObjectNode objectNode) {
    val builder = new ImmutableMap.Builder<String, String>();
    Iterator<String> fieldNames = objectNode.fieldNames();
    while (fieldNames.hasNext()) {
      val fieldName = fieldNames.next();
      val jsonNode = objectNode.get(fieldName);
      checkState(jsonNode.getNodeType() != JsonNodeType.OBJECT && jsonNode.getNodeType() != JsonNodeType.ARRAY);
      builder.put(
          fieldName,
          jsonNode.asText());
    }

    return builder.build();
  }

  public static String formatPrettyJson(String jsonString) {
    return formatPrettyJson(toJsonNode(jsonString));
  }

  @SneakyThrows
  public static String formatPrettyJson(Object object) {
    return PRETTY_WRITTER.writeValueAsString(object);
  }

  public static ObjectNode getRootObject(@NonNull final String path) {
    return getRootObject(URLs.getUrlFromPath(path));
  }

  @SneakyThrows
  public static ObjectNode getRootObject(@NonNull final File file) {
    return getRootObject(URLs.getUrl(file));
  }

  public static ObjectNode getRootObject(@NonNull final URL url) {
    return asObjectNode(getJsonNode(url));
  }

  public static ArrayNode getRootArray(@NonNull final String path) {
    return getRootArray(new File(path));
  }

  public static ArrayNode getRootArray(@NonNull final File file) {
    return getRootArray(URLs.getUrl(file.toURI()));
  }

  /**
   * Jackson doesn't seem to provide a "ArrayMapper" nor an "ObjectMapper#readValues" method, so we use this workaround
   * instead.
   */
  public static ArrayNode getRootArray(@NonNull final URL url) {
    return getArrayNode(readValues(
        JSON_NODE_READER, url));
  }

  public static <T> JsonNode to(T t) {
    return DEFAULT.convertValue(t, JsonNode.class);
  }

  public static <T> T from(JsonNode jsonNode, Class<T> type) {
    return DEFAULT.convertValue(jsonNode, type);
  }

  public static <T> List<T> from(ArrayNode arrayNode, Class<T> type) {
    return DEFAULT.convertValue(
        arrayNode,
        new TypeReference<List<T>>() {});
  }

  public static <T> String formatPrettyLog(String message, T t) {
    return INDENT.join(
        message,
        INDENT.join(
            NEWLINE.split(
                formatPrettyJson(t))));
  }

  @SneakyThrows
  private static JsonNode getJsonNode(final URL url) {
    return DEFAULT.readTree(url);
  }

  public static ObjectNode asObjectNode(@NonNull final JsonNode node) {
    checkState(node.isObject(),
        "Expecting a node of type '%s', instead go: '%s'",
        JsonNodeType.OBJECT, node.getNodeType());
    return (ObjectNode) node;
  }

  public static ArrayNode asArrayNode(@NonNull final JsonNode node) {
    checkState(node.isArray(),
        "Expecting a node of type '%s', instead go: '%s'",
        JsonNodeType.ARRAY, node.getNodeType());
    return (ArrayNode) node;
  }

  @SneakyThrows
  private static JsonNode toJsonNode(@NonNull final String jsonString) {
    return DEFAULT.readTree(jsonString);
  }

  private static ArrayNode getArrayNode(@NonNull final MappingIterator<JsonNode> it) {
    val array = DEFAULT.createArrayNode();
    while (it.hasNext()) {
      array.add(it.next());
    }

    return array;
  }

  @SneakyThrows
  private static MappingIterator<JsonNode> readValues(
      @NonNull final ObjectReader reader,
      @NonNull final URL url) {
    return reader.readValues(url);
  }

}
