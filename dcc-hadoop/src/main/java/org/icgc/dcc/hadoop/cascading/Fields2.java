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
package org.icgc.dcc.hadoop.cascading;

import static cascading.tuple.Fields.NONE;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.util.Joiners.UNDERSCORE;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.core.util.Proposition;
import org.icgc.dcc.core.util.Separators;

import cascading.tuple.Fields;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

/**
 * Utility class for working with cascading {@code Fields} objects.
 * <p>
 * TODO: remove some redundant/obsolete methods + make this a decorator instead
 */
@NoArgsConstructor(access = PRIVATE)
public final class Fields2 {

  public static Fields NO_FIELDS = new Fields();
  public static Fields EMPTY_FIELDS = NO_FIELDS;

  private static final String DEFAULT_FIELD_SEPARATOR = Separators.DOLLAR;
  private static final String COUNT_SUFFIX = "count";
  private static final String REDUNDANT_PREFIX = "redundant";

  public static Fields checkFieldsCardinalityOne(Fields fields) {
    return checkFieldsCardinality(fields, 1);
  }

  public static Fields checkFieldsCardinalityTwo(Fields fields) {
    return checkFieldsCardinality(fields, 2);
  }

  public static Fields checkFieldsCardinalityThree(Fields fields) {
    return checkFieldsCardinality(fields, 3);
  }

  public static Fields checkFieldsCardinality(Fields fields, int expectedSize) {
    checkState(
        fields.size() == expectedSize,
        "Expecting only '%s' field(s), instead got '%s' ('%s')",
        expectedSize, fields.size(), fields);
    return fields;
  }

  public static Fields getField(Comparable<?> fieldComparable) {
    return fieldComparable instanceof Fields ?
        (Fields) fieldComparable :
        new Fields(fieldComparable);
  }

  public static Fields fields(Iterable<String> fieldNames) {
    return fields(newArrayList(fieldNames));
  }

  public static Fields fields(Collection<String> fieldNames) {
    return new Fields(toStringArray(fieldNames));
  }

  public static Fields field(String fieldName) {
    return new Fields(fieldName);
  }

  public static Fields fields(String... fieldNames) {
    return new Fields(fieldNames);
  }

  public static Fields argumentSelector(String... fieldNames) {
    return new Fields(fieldNames);
  }

  public static Fields outputSelector(String... fieldNames) {
    return new Fields(fieldNames);
  }

  public static Fields declaredFields(Collection<String> fieldNames) {
    return new Fields(toStringArray(fieldNames));
  }

  public static Fields declaredFields(String... fieldNames) {
    return new Fields(fieldNames);
  }

  public static Fields uniqueFields(String... fieldNames) {
    return new Fields(fieldNames);
  }

  public static Fields discardFields(String... fieldNames) {
    return new Fields(fieldNames);
  }

  public static Fields lhsFields(String... fieldNames) {
    return new Fields(fieldNames);
  }

  public static Fields rhsFields(String... fieldNames) {
    return new Fields(fieldNames);
  }

  public static Fields groupFields(String... fieldNames) {
    return new Fields(fieldNames);
  }

  public static Fields sortFields(String... fieldNames) {
    return new Fields(fieldNames);
  }

  public static Entry<Fields, Object> keyValuePair(String fieldName, Object value) {
    return new SimpleEntry<Fields, Object>(NAME_TO_FIELD.apply(fieldName), value);
  }

  public static Entry<Fields, Object> keyValuePair(Fields field, Object value) {
    return new SimpleEntry<Fields, Object>(field, value);
  }

  private static String[] toStringArray(Collection<String> fieldNames) {
    return fieldNames.toArray(new String[] {});
  }

  public static Fields getCountFieldCounterpart(Fields field) {
    return getCountFieldCounterpart(getFieldName(checkFieldsCardinalityOne(field)));
  }

  public static Fields getCountFieldCounterpart(String fieldName) {
    return new Fields(ADD_COUNT_SUFFIX.apply(fieldName));
  }

  public static Fields getCountFieldCounterpart(
      @NonNull final Enum<?> type,
      @NonNull final String fieldName) {
    return new Fields(ADD_COUNT_SUFFIX.apply(UNDERSCORE.join(type.name().toLowerCase(), fieldName)));
  }

  public static Fields getTemporaryCountByFields(
      @NonNull final Fields countByFields,
      @NonNull final Enum<?> type) {
    Fields temporaryCountByFields = NONE;
    for (val fieldName : getFieldNames(countByFields)) {
      temporaryCountByFields = temporaryCountByFields.append(
          getCountFieldCounterpart(type, fieldName));
    }

    return temporaryCountByFields;
  }

  public static Fields getRedundantFieldCounterparts(Fields fields) {
    Fields redundantFields = NONE;
    for (val fieldName : getFieldNames(fields)) {
      redundantFields = redundantFields.append(getRedundantFieldCounterpart(fieldName));
    }

    return redundantFields;
  }

  public static Fields getRedundantFieldCounterpart(Fields field) {
    return getRedundantFieldCounterpart(getFieldName(checkFieldsCardinalityOne(field)));
  }

  public static Fields getRedundantFieldCounterpart(String fieldName) {
    return new Fields(ADD_REDUNDANT_PREFIX.apply(fieldName));
  }

  public static Fields getRedundantFieldCounterpart(
      @NonNull final Enum<?> type,
      @NonNull final Fields field) {
    return getRedundantFieldCounterpart(type, getFieldName(checkFieldsCardinalityOne(field)));
  }

  public static Fields getRedundantFieldCounterpart(
      @NonNull final Enum<?> type,
      @NonNull final String fieldName) {
    return new Fields(ADD_REDUNDANT_PREFIX.apply(UNDERSCORE.join(type.name().toLowerCase(), fieldName)));
  }

  /**
   * There does not seem to be a built-in way...
   */
  public static Fields cloneFields(Fields fields) {
    Fields clone = new Fields();
    for (int i = 0; i < fields.size(); i++) {
      clone = clone.append(new Fields(fields.get(i)));
    }
    return clone;
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
   * indexOf(new Fields("a", "b", "c", "d"), new Fields("c")) // returns 2
   */
  public static Integer indexOf(Fields fields, Fields subfield) {
    checkState(subfield.size() == 1, "Expecting only 1 field, instead got '%s'", subfield);
    val indices = indicesOf(fields, subfield);
    return indices.get(0);
  }

  /**
   * indicesOf(new Fields("a", "b", "c", "d"), new Fields("b", "y", "a", "z")) // returns [1, -1, 0, -1]
   */
  @SuppressWarnings("rawtypes")
  public static List<Integer> indicesOf(Fields fields, Fields subfields) {
    List<Comparable> fieldNames = getFieldComparables(fields);
    List<Integer> indices = new ArrayList<Integer>();
    for (int i = 0; i < subfields.size(); i++) {
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
    if (fields == null) return extra;
    Comparable[] concatenated = Arrays.copyOf(fields, fields.length + extra.length);
    for (int i = 0; i < extra.length; i++) {
      concatenated[i + fields.length] = extra[i];
    }
    return concatenated;
  }

  @SuppressWarnings("rawtypes")
  private static List<Comparable> buildMutableFieldNames(Fields fields) {
    List<Comparable> fieldNames = new ArrayList<Comparable>();
    for (int i = 0; i < fields.size(); i++) {
      fieldNames.add(fields.get(i));
    }
    return fieldNames;
  }

  public static List<String> getFieldNames(Fields fields) {
    List<String> fieldNames = new ArrayList<String>();
    for (int i = 0; i < fields.size(); i++) {
      fieldNames.add(fields.get(i).toString());
    }
    return fieldNames;
  }

  public static String prefixedFieldName(FileType fileType, String fieldName) {
    return prefixedFieldName(fileType.getId(), fieldName);
  }

  public static String prefixedFieldName(String prefix, String fieldName) {
    return prefixedFieldName(prefix, DEFAULT_FIELD_SEPARATOR, fieldName);
  }

  public static String prefixedFieldName(FileType fileType, String sep, String fieldName) {
    return prefixedFieldName(fileType.getId(), sep, fieldName);
  }

  public static String prefixedFieldName(String prefix, String sep, String fieldName) {
    return prefix(prefix, sep, fieldName);
  }

  public static Fields prefixedFields(FileType fileType, String fieldName) {
    return prefixedFields(fileType, new Fields(fieldName));
  }

  public static Fields prefixedFields(FileType fileType, Fields fields) {
    return prefixedFields(fileType.getId(), fields);
  }

  public static Fields prefixedFields(String prefix, Fields fields) {
    return prefixedFields(prefix, DEFAULT_FIELD_SEPARATOR, fields);
  }

  public static Fields prefixedFields(String prefix, String separator, Fields fields) {
    Fields prefixedFields = new Fields();
    for (String fieldName : getFieldNames(fields)) {
      String newFieldName = prefix(prefix, separator, fieldName);
      prefixedFields = prefixedFields.append(new Fields(newFieldName));
    }
    return prefixedFields;
  }

  public static Fields prefixedFields(String prefix, String sep, String[] fields) {
    String[] prefixed = new String[fields.length];
    for (int i = 0; i < prefixed.length; i++) {
      prefixed[i] = prefix(prefix, sep, fields[i]);
    }
    return new Fields(prefixed);
  }

  public static Fields prefixedFields(String prefix, String field) {
    return prefixedFields(prefix, DEFAULT_FIELD_SEPARATOR, field);
  }

  public static Fields prefixedFields(String prefix, String sep, String field) {
    return new Fields(prefix(prefix, sep, field));
  }

  public final static String prefix(String prefix, String value) {
    return prefix(prefix, DEFAULT_FIELD_SEPARATOR, value);
  }

  public final static String prefix(String prefix, String sep, String value) {
    checkNotNull(prefix);
    checkNotNull(value);
    return prefix + sep + value;
  }

  public static Fields unprefixFields(Fields fields) {
    return unprefixFields(fields, DEFAULT_FIELD_SEPARATOR);
  }

  public static Fields unprefixFields(Fields fields, String sep) {
    String[] unprefixed = new String[fields.size()];
    for (int i = 0; i < unprefixed.length; i++) {
      unprefixed[i] = extractField(fields.get(i).toString(), sep);
    }
    return new Fields(unprefixed);
  }

  public static String extractField(String prefixedField, String sep) {
    int index = prefixedField.indexOf(sep);
    if (index < 0) throw new IllegalArgumentException();
    if (index + 1 > prefixedField.length()) throw new IllegalArgumentException();
    return prefixedField.substring(index + 1);
  }

  public static String getPrefix(String prefixedFieldName) {
    return getPrefix(prefixedFieldName, DEFAULT_FIELD_SEPARATOR);
  }

  public static String getPrefix(String prefixedFieldName, String sep) {
    return Splitter.on(sep).split(prefixedFieldName).iterator().next();
  }

  /**
   * Returns the actual field name.
   */
  public static String getFieldName(Fields field) {
    return checkFieldsCardinalityOne(field).print().replace("['", "").replace("']", "");
  }

  public static Fields appendFieldIfApplicable(
      @NonNull final Fields fields, // TODO: decorator around that Fields rather
      @NonNull final Proposition proposition,
      @NonNull final Fields conditionedFields) {

    return fields.append(
        proposition.evaluate() ?
            conditionedFields :
            NO_FIELDS);
  }

  private static final Function<String, Fields> NAME_TO_FIELD = new Function<String, Fields>() {

    @Override
    public Fields apply(String fieldName) {
      return new Fields(fieldName);
    }

  };

  private static final Function<String, String> ADD_COUNT_SUFFIX = new Function<String, String>() {

    @Override
    public String apply(String fieldName) {
      return UNDERSCORE.join(fieldName, COUNT_SUFFIX);
    }

  };

  private static final Function<String, String> ADD_REDUNDANT_PREFIX = new Function<String, String>() {

    @Override
    public String apply(String fieldName) {
      return UNDERSCORE.join(REDUNDANT_PREFIX, fieldName);
    }

  };

  public static Fields swapTwoFields(@NonNull final Fields twoFields) {
    checkFieldsCardinalityTwo(twoFields);

    return getField(twoFields.get(1))
        .append(getField(twoFields.get(0)));
  }

}
