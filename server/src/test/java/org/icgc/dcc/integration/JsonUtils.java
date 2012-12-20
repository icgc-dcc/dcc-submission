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

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.codehaus.jackson.JsonNode;

/**
 * General tools for working with JSON
 */
public final class JsonUtils {

  public static void filterTree(JsonNode o, List<String> includedProperties, List<String> excludedProperties,
      int maxDepth) {
    filterTreeRecursive(o, includedProperties, excludedProperties, maxDepth, null);
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
