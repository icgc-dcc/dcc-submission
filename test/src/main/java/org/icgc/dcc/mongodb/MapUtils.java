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
package org.icgc.dcc.mongodb;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;

import com.google.common.collect.Maps;

public class MapUtils {

  @SuppressWarnings("unchecked")
  public static TreeMap<String, Object> asTreeMap(Map<String, Object> map) throws IOException, JsonParseException,
      JsonMappingException {
    TreeMap<String, Object> treeMap = Maps.newTreeMap();
    for(Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
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
