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
package org.icgc.dcc.integration;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.MappingIterator;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

/**
 * General test utilities for working with JSON objects.
 */
public final class JsonUtils {

  private static final String ID_FIELD = "_id";

  private static final String OID_FIELD = "$oid";

  private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);

  private JsonUtils() {
    // Prevent construction
  }

  /**
   * Asserts semantic JSON equality between {@code expectedFile} and {@code actualFile} using a memory efficient
   * stream-based comparison of deserialized sequences of JSON objects, ignoring transient fields.
   * 
   * @param expectedFile
   * @param actualFile
   */
  public static void assertJsonFileEquals(File expectedFile, File actualFile) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      MappingIterator<JsonNode> expected = mapper.reader(JsonNode.class).readValues(expectedFile);
      MappingIterator<JsonNode> actual = mapper.reader(JsonNode.class).readValues(actualFile);

      while(actual.hasNext() && expected.hasNext()) {

        // ObjectJSON.toString() seems to be the way to get the json representation, but documentation is lacking...
        @SuppressWarnings("unchecked")
        TreeMap<String, Object> expectedMap = asTreeMap(mapper.readValue(expected.nextValue().toString(), Map.class));
        @SuppressWarnings("unchecked")
        TreeMap<String, Object> actualMap = asTreeMap(mapper.readValue(actual.nextValue().toString(), Map.class));

        assertEquals("JSON mismatch between expected JSON file:\n\t" + expectedFile + "\nand actual JSON file:\n\t"
            + actualFile + "\n", expectedMap.toString(), actualMap.toString());
      }

      // Ensure same number of elements
      assertEquals("Actual JSON file is missing objects", expected.hasNext(), false);
      assertEquals("Actual JSON file has additional objects", actual.hasNext(), false);
    } catch(IOException e) {
      Throwables.propagate(e);
    }
  }

  /**
   * Removes transient JSON properties that can change across runs (e.g. $oid).
   * 
   * @param jsonNode
   * @deprecated shouldn't be necessary anymore
   */
  @Deprecated
  public static void normalizeJsonNode(JsonNode jsonNode) {
    filterTree(jsonNode, null, ImmutableList.of("$oid"), Integer.MAX_VALUE);
  }

  /**
   * Filters the supplied {@code tree} subject to {@code includedProperties}, {@code excludedProperties} and
   * {@code maxDepth}. Inclusion takes priority over exclusion.
   * 
   * @param tree the JSON object to filter
   * @param includedProperties the properties to include
   * @param excludedProperties the properties to exclude
   * @param maxDepth maximum tree depth to filter
   */
  static void filterTree(JsonNode tree, List<String> includedProperties, List<String> excludedProperties, int maxDepth) {
    filterTreeRecursive(tree, includedProperties, excludedProperties, maxDepth, null);
  }

  private static void filterTreeRecursive(JsonNode tree, List<String> includedProperties,
      List<String> excludedProperties, int maxDepth, String key) {
    Iterator<Entry<String, JsonNode>> fieldsIter = tree.getFields();
    while(fieldsIter.hasNext()) {
      Entry<String, JsonNode> field = fieldsIter.next();
      String fullName = key == null ? field.getKey() : key + "." + field.getKey();

      boolean depthOk = field.getValue().isContainerNode() && maxDepth >= 0;
      boolean isIncluded = includedProperties != null && !includedProperties.contains(fullName);
      boolean isExcluded = excludedProperties != null && excludedProperties.contains(fullName);
      if((!depthOk && !isIncluded) || isExcluded) {
        fieldsIter.remove();
        continue;
      }

      filterTreeRecursive(field.getValue(), includedProperties, excludedProperties, maxDepth - 1, fullName);
    }
  }

  /**
   * Sorts document, reorder their fields and removes the $oid field (one json document per line).
   * <p>
   * Useful for comparisons in tests. TODO: rewrite entirely as part of DCC-709
   */
  @SuppressWarnings("unchecked")
  public static void normalizeDumpFile(File file) {
    File format;
    ObjectMapper mapper = new ObjectMapper();
    FileReader fileReader = null;
    BufferedReader bufferedReader = null;
    FileWriter fileWriter = null;
    BufferedWriter bufferedWriter = null;
    try {
      fileReader = new FileReader(file);
      bufferedReader = new BufferedReader(fileReader);

      format = new File(file.getAbsolutePath() + ".fmt");
      fileWriter = new FileWriter(format);
      bufferedWriter = new BufferedWriter(fileWriter);

      String line = null;
      while((line = bufferedReader.readLine()) != null) {
        processLine(mapper, bufferedWriter, mapper.readValue(line, Map.class));
      }
    } catch(JsonParseException e) {
      throw new RuntimeException(e);
    } catch(JsonMappingException e) {
      throw new RuntimeException(e);
    } catch(JsonGenerationException e) {
      throw new RuntimeException(e);
    } catch(FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch(IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        bufferedReader.close();
        fileReader.close();
        bufferedWriter.close();
        fileWriter.close();
      } catch(IOException e) {
        log.error("could not close readers/writers", e);
        throw new RuntimeException(e);
      }
    }

    File sort = new File(file.getAbsolutePath() + ".sort");
    try {
      String sortCommand = "sort " + format.getAbsolutePath() + " > " + sort.getAbsolutePath();
      Runtime.getRuntime().exec(new String[] { "sh", "-c", sortCommand }).waitFor();
    } catch(IOException e) {
      throw new RuntimeException(e);
    } catch(InterruptedException e) {
      throw new RuntimeException(e);
    }

    try {
      Files.copy(sort, file);
      format.delete();
      sort.delete();
    } catch(IOException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private static void processLine(ObjectMapper mapper, BufferedWriter bufferedWriter, Map<String, Object> map0)
      throws IOException, JsonParseException, JsonMappingException, JsonGenerationException {
    TreeMap<String, Object> treeMap = asTreeMap(map0); // for reordering
    Object object = treeMap.get(ID_FIELD);
    if(object != null && object instanceof Map) {
      Map<String, Object> map = (Map<String, Object>) object;
      if(map.containsKey(OID_FIELD)) { // changes for every run
        map.put("$oid", "[removed-for-comparisons]");
        // map.remove(OID_FIELD);
      }
    }
    StringWriter sw = new StringWriter();
    mapper.writeValue(sw, treeMap);
    sw.close();
    bufferedWriter.write(sw.toString());
    bufferedWriter.newLine();
  }

  /**
   * See http://stackoverflow.com/questions/9476426/serialized-json-with-sorted-keys-using-jackson.
   */
  @SuppressWarnings("unchecked")
  private static TreeMap<String, Object> asTreeMap(Map<String, Object> map) throws IOException, JsonParseException,
      JsonMappingException {
    TreeMap<String, Object> treeMap = Maps.newTreeMap();
    for(String key : map.keySet()) {
      Object value = map.get(key);
      if(value instanceof Map) {
        Map<String, Object> subMap = (Map<String, Object>) value;
        treeMap.put(key, asTreeMap(subMap));
      } else if(value instanceof List) {
        Map<String, Object> bufferMap = Maps.newTreeMap(); // so as to order it
        for(Object item : (List<Object>) value) {
          if(item instanceof Map) {
            TreeMap<String, Object> subSubMap = asTreeMap((Map<String, Object>) item);
            bufferMap.put(subSubMap.toString(), subSubMap);
          } else {
            bufferMap.put(item.toString(), item); // TODO: can it be null?
          }
        }
        treeMap.put(key, bufferMap.values());
      } else {
        treeMap.put(key, value); // possibly null
      }
    }
    return treeMap;
  }
}
