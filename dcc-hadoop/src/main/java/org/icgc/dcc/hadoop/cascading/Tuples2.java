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

import java.util.ArrayList;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import cascading.tuple.Tuple;

/**
 * Utility class to help with the {@link Tuple} object from cascading.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Tuples2 {

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
}
