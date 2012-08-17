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
package org.icgc.dcc.validation.cascading;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cascading.tuple.Fields;

import com.google.common.collect.ImmutableList;

/**
 * Offers various utils methods to handle {@code Fields} (at least until we find cleaner cascading ways to do the same,
 * or that they offer more utils themselves)
 */
public class FieldsUtils {

  /*
   * buildFieldNames(new Fields("a", "b", "c", "d")) // returns [a, b, c, d]
   */
  @SuppressWarnings("rawtypes")
  public static List<Comparable> getFieldNames(Fields fields) {
    List<Comparable> fieldNames = buildMutableFieldNames(fields);
    return ImmutableList.<Comparable> copyOf(fieldNames);
  }

  /*
   * indicesOf(new Fields("a", "b", "c", "d"), new Fields("b", "y", "a", "z")) // returns [1, -1, 0, -1]
   */
  @SuppressWarnings("rawtypes")
  public static List<Integer> indicesOf(Fields fields, Fields subfields) {
    List<Comparable> fieldNames = getFieldNames(fields);
    List<Integer> indices = new ArrayList<Integer>();
    for(int i = 0; i < subfields.size(); i++) {
      Comparable fieldName = subfields.get(i);
      indices.add(fieldNames.indexOf(fieldName));
    }
    return ImmutableList.<Integer> copyOf(indices);
  }

  @SuppressWarnings("rawtypes")
  public static List<Comparable> buildSortedList(Fields fields) {
    List<Comparable> fieldNames = buildMutableFieldNames(fields);
    Collections.sort(fieldNames);
    return ImmutableList.<Comparable> copyOf(fieldNames);
  }

  @SuppressWarnings("rawtypes")
  public static Comparable[] concat(Comparable[] fields, Comparable... extra) {
    if(fields == null) return extra;
    Comparable[] concatenated = Arrays.copyOf(fields, fields.length + extra.length);
    for(int i = 0; i < extra.length; i++) {
      concatenated[i + fields.length] = extra[i];
    }
    return concatenated;
  }

  @SuppressWarnings("rawtypes")
  private static List<Comparable> buildMutableFieldNames(Fields fields) {
    List<Comparable> fieldNames = new ArrayList<Comparable>();
    for(int i = 0; i < fields.size(); i++) {
      fieldNames.add(fields.get(i));
    }
    return fieldNames;
  }
}
