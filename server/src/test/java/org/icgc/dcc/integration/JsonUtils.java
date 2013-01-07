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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingIterator;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;

/**
 * General test utilities for working with JSON objects.
 */
public final class JsonUtils {

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
        JsonNode expectedJsonNode = expected.nextValue();
        JsonNode actualJsonNode = actual.nextValue();

        // Remove transient fields
        normalizeJsonNode(expectedJsonNode);
        normalizeJsonNode(actualJsonNode);

        assertEquals(
            "JSON mismatch between expected JSON file " + expectedFile + " and actual JSON file " + actualFile,
            expectedJsonNode, actualJsonNode);
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
   */
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

  private JsonUtils() {
    // Prevent construction
  }

}
