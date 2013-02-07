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
package org.icgc.dcc.validation.cascading;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cascading.tuple.Fields;

import com.google.common.collect.ImmutableList;

/**
 * Utility class for working with cascading {@code Fields} objects.
 * <p>
 * TODO: remove some redundant/obsolete methods
 */
public final class FieldsUtils {

  private FieldsUtils() {
    // Prevent construction
  }

  /**
   * buildFieldNames(new Fields("a", "b", "c", "d")) // returns [a, b, c, d]
   */
  @SuppressWarnings("rawtypes")
  public static List<Comparable> getFieldComparables(Fields fields) {
    List<Comparable> fieldNames = buildMutableFieldNames(fields);
    return ImmutableList.<Comparable> copyOf(fieldNames);
  }

  /**
   * indicesOf(new Fields("a", "b", "c", "d"), new Fields("b", "y", "a", "z")) // returns [1, -1, 0, -1]
   */
  @SuppressWarnings("rawtypes")
  public static List<Integer> indicesOf(Fields fields, Fields subfields) {
    List<Comparable> fieldNames = getFieldComparables(fields);
    List<Integer> indices = new ArrayList<Integer>();
    for(int i = 0; i < subfields.size(); i++) {
      Comparable fieldName = subfields.get(i);
      indices.add(fieldNames.indexOf(fieldName));
    }
    return ImmutableList.<Integer> copyOf(indices);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
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

  public static List<String> getFieldNames(Fields fields) {
    List<String> fieldNames = new ArrayList<String>();
    for(int i = 0; i < fields.size(); i++) {
      fieldNames.add(fields.get(i).toString());
    }
    return fieldNames;
  }

  public static Fields prefixedFields(String prefix, String sep, String[] fields) {
    String[] prefixed = new String[fields.length];
    for(int i = 0; i < prefixed.length; i++) {
      prefixed[i] = prefix(prefix, sep, fields[i]);
    }
    return new Fields(prefixed);
  }

  public static Fields prefixedFields(String prefix, String sep, String field) {
    return new Fields(prefix(prefix, sep, field));
  }

  public final static String prefix(String prefix, String sep, String value) {
    checkNotNull(prefix);
    checkNotNull(value);
    return prefix + sep + value;
  }

  public static Fields unprefixFields(Fields fields, String sep) {
    String[] unprefixed = new String[fields.size()];
    for(int i = 0; i < unprefixed.length; i++) {
      unprefixed[i] = extractField(fields.get(i).toString(), sep);
    }
    return new Fields(unprefixed);
  }

  public static String extractField(String prefixedField, String sep) {
    int index = prefixedField.indexOf(sep);
    if(index < 0) throw new IllegalArgumentException();
    if(index + 1 > prefixedField.length()) throw new IllegalArgumentException();
    return prefixedField.substring(index + 1);
  }

}
