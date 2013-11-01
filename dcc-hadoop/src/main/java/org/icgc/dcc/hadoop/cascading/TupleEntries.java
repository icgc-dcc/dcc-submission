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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

/**
 * Utility class to help with the {@link TupleEntry} object from cascading.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TupleEntries {

  /**
   * Checks whether a {@link TupleEntry} contains a given {@link Fields}, based on the field name.
   * <p>
   * There doesn't seem to be a built-in way to do that in cascading as of version 2.1.5.
   */
  public static boolean contains(TupleEntry entry, String fieldName) {
    return entry.getFields().contains(new Fields(fieldName));
  }

  /**
   * Returns the list of {@link Fields} for a {@link TupleEntry}.
   */
  public static List<String> getFieldNames(TupleEntry entry) {
    return Fields2.getFieldNames(entry.getFields());
  }

  /**
   * Extracts a nested {@link Tuple} from a {@link TupleEntry} for a given {@link Fields}.
   */
  public static Tuple getTuple(TupleEntry entry, Fields field) {
    return getT(Tuple.class, entry, field);
  }

  /**
   * Extracts a nested {@link TupleEntry} from a {@link TupleEntry} for a given {@link Fields}.
   */
  public static TupleEntry getTupleEntry(TupleEntry entry, Fields field) {
    return getT(TupleEntry.class, entry, field);
  }

  /**
   * Extracts a nested object of type T from a {@link TupleEntry} for a given {@link Fields}.
   */
  private static <T> T getT(Class<T> clazz, TupleEntry entry, Fields field) {
    Object object =
        checkNotNull(entry, "Expecting non-null entry")
            .getObject(checkNotNull(field, "Expecting non-null field name for entry %s", entry));
    checkNotNull(object, "Expecting non-null object for field name %s", field);
    checkState(clazz.isInstance(object),
        "%s was expected to be of type %s, %s instead", field, clazz, object.getClass());
    @SuppressWarnings("unchecked")
    T t = (T) object;
    return t;
  }

  public static boolean hasValues(TupleEntry tupleEntry, String[] fields) {
    Tuple tuple = tupleEntry.selectTuple(new Fields(fields));
    return tuple.equals(Tuple.size(tuple.size())) == false;
  }

  /**
   * Gives a string containing the json representation of the tupleEntry (with possibly multiple levels of
   * tuple/tupleEntry nesting).
   * <p>
   * Very useful for debugging.
   */
  public static String toJson(TupleEntry tupleEntry) {
    Fields fields = tupleEntry.getFields();
    Tuple tuple = tupleEntry.getTuple();
  
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    for (int i = 0; i < fields.size(); i++) {
      Comparable<?> field = fields.get(i);
      Object object = tuple.getObject(i);
      String value;
      if (object instanceof TupleEntry) { // build sub-document
        value = toJson((TupleEntry) object);
      } else if (object instanceof Tuple) { // build array
        Tuple subTuple = (Tuple) object;
        StringBuilder subSb = new StringBuilder();
        subSb.append("[");
        for (int j = 0; j < subTuple.size(); j++) {
          Object subObject = subTuple.getObject(j);
          checkState(subObject instanceof TupleEntry); // by design; TODO: handle more cases?
          subSb.append((j == 0 ? "" : ", ") + toJson((TupleEntry) subObject));
        }
        subSb.append("]");
        value = subSb.toString();
      } else { // build primitive
        value = "\"" + (object == null ? "null" : object.toString()) + "\"";
      }
      sb.append((i == 0 ? "" : ", ") + "\"" + field + "\"" + ":" + value);
    }
  
    sb.append("}");
    return sb.toString();
  }

}
