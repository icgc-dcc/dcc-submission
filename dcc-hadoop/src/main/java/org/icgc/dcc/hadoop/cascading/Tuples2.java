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

import static lombok.AccessLevel.PRIVATE;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import cascading.operation.BaseOperation;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

import com.google.common.collect.ImmutableSet;

/**
 * Utility class to help with the {@link Tuple} object from cascading.
 */
@NoArgsConstructor(access = PRIVATE)
public final class Tuples2 {

  /**
   * Index of the first item in a {@link TupleEntry} (convenient for {@link BaseOperation}s).
   */
  static final int FIRST_ITEM = 0;

  /**
   * Index of the second item in a {@link TupleEntry} (convenient for {@link BaseOperation}s).
   */
  static final int SECOND_ITEM = FIRST_ITEM + 1;

  public static String getFirstString(Tuple tuple) {
    return getString(tuple, FIRST_ITEM);
  }

  public static String getSecondString(Tuple tuple) {
    return getString(tuple, SECOND_ITEM);
  }

  public static Object getFirstObject(Tuple tuple) {
    return getObject(tuple, FIRST_ITEM);
  }

  public static Object getSecondObject(Tuple tuple) {
    return getObject(tuple, SECOND_ITEM);
  }

  public static int getFirstInteger(Tuple tuple) {
    return getInteger(tuple, FIRST_ITEM);
  }

  public static int getSecondInteger(Tuple tuple) {
    return getInteger(tuple, SECOND_ITEM);
  }

  public static Tuple setFirstInteger(Tuple tuple, int value) {
    tuple.set(FIRST_ITEM, value);
    return tuple;
  }

  public static Tuple setSecondInteger(Tuple tuple, int value) {
    tuple.set(SECOND_ITEM, value);
    return tuple;
  }

  public static Tuple setFirstLong(Tuple tuple, long value) {
    tuple.set(FIRST_ITEM, value);
    return tuple;
  }

  public static Tuple setSecondLong(Tuple tuple, long value) {
    tuple.set(SECOND_ITEM, value);
    return tuple;
  }

  public static Tuple setFirstString(Tuple tuple, String value) {
    tuple.set(FIRST_ITEM, value);
    return tuple;
  }

  public static Tuple setSecondString(Tuple tuple, String value) {
    tuple.set(SECOND_ITEM, value);
    return tuple;
  }

  private static String getString(Tuple tuple, int index) {
    return tuple.getString(index);
  }

  private static int getInteger(Tuple tuple, int index) {
    return tuple.getInteger(index);
  }

  private static Object getObject(Tuple tuple, int index) {
    return tuple.getObject(index);
  }

  /**
   * Nests a tuple within a tuple.
   * <p>
   * Necessary because new Tuple(new Tuple()) is the copy constructor, not the way to nest a tuple under a tuple.
   */
  public static Tuple nestTuple(Tuple tuple) {
    return nestValue(tuple);
  }

  /**
   * See {@link #nestTuple(Tuple)} for rationale.
   */
  public static <T> Tuple nestValue(T value) {
    val nestingTuple = new Tuple();
    nestingTuple.add(value);
    return nestingTuple;
  }

  public static boolean isNullTuple(@NonNull Tuple tuple) {
    for (val value : tuple) {
      if (value != null) {
        return false;
      }
    }
    return true;
  }

  public static boolean isNullField(Tuple tuple, int fieldIndex) {
    return tuple.getObject(fieldIndex) == null;
  }

  public static List<Object> getObjects(Tuple tuple) {
    List<Object> objects = new ArrayList<Object>();
    for (int i = 0; i < tuple.size(); i++) {
      objects.add(tuple.getObject(i));
    }
    return objects;
  }

  /**
   * Determines whether or not 2 non-null tuples have the same content, with nulls matching nulls in terms of values.
   * <p>
   * This is mostly useful for tests.
   * <p>
   * TODO: consider handling nested tuples.
   */
  public static boolean sameContent(Tuple tuple1, Tuple tuple2) {
    if (tuple1 == null || tuple2 == null) {
      return false;
    }
    if (tuple1.size() != tuple2.size()) {
      return false;
    }
    for (int i = 0; i < tuple1.size(); i++) {
      Object object1 = tuple1.getObject(i);
      Object object2 = tuple2.getObject(i);
      if ((object1 == null && object2 != null) ||
          (object1 != null && object2 == null) ||
          (object1 != null && object2 != null && !object1.equals(object2))) {
        return false;
      }
    }
    return true;
  }

  public static Set<Tuple> sortTuples(Tuple tuple) {
    val tuples = new TreeSet<Tuple>();
    for (val object : tuple) {
      tuples.add(((TupleEntry) object).getTuple()); // By design
    }

    return ImmutableSet.<Tuple> copyOf(tuples);
  }

}
