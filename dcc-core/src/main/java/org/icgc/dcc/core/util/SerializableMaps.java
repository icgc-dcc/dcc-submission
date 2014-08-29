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

import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Maps.uniqueIndex;

import java.util.Map;
import java.util.Set;

import lombok.val;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;

/**
 * Serializable versions of some guava {@link Maps} methods.
 */
public class SerializableMaps {

  public static <K1, V1, K2, V2> Map<K2, V2> transformMap(
      Map<K1, V1> map,
      Function<K1, K2> keyFunction,
      Function<V1, V2> valueFunction) {

    return transformValues(
        transformKeys(map, keyFunction),
        valueFunction);
  }

  /**
   * The missing List&ltT&gt -> Map&ltk(T), v(T)&gt all-in-one conversion.
   */
  public static <T, K, V> Map<K, V> transformListToMap(
      Iterable<T> iterable,
      Function<T, K> keyFunction,
      Function<T, V> valueFunction) {

    return transformValues(
        uniqueIndex(
            iterable,
            keyFunction),
        valueFunction);
  }

  public static <K1, K2, V> Map<K2, V> transformKeys(
      Map<K1, V> inputMap,
      Function<K1, K2> function) {
    Map<K2, V> map = newLinkedHashMap();
    for (val entry : inputMap.entrySet()) {
      map.put(function.apply(entry.getKey()), entry.getValue());
    }

    return map;
  }

  public static <K, V1, V2> Map<K, V2> transformValues(
      Map<K, V1> inputMap,
      Function<? super V1, V2> function) {
    Map<K, V2> map = newLinkedHashMap();
    for (val entry : inputMap.entrySet()) {
      map.put(entry.getKey(), function.apply(entry.getValue()));
    }

    return map;
  }

  public static <K, V> Map<K, V> asMap(
      Set<K> inputSet,
      Function<K, V> function) {
    Map<K, V> map = newLinkedHashMap();
    for (val k : inputSet) {
      map.put(k, function.apply(k));
    }

    return map;
  }

  public static <K, V> Map<K, V> filterKeys(
      Map<K, V> inputMap,
      Predicate<K> predicate) {
    Map<K, V> map = newLinkedHashMap();
    for (val k : inputMap.keySet()) {
      if (predicate.apply(k)) {
        map.put(k, inputMap.get(k));
      }
    }

    return map;
  }

}
