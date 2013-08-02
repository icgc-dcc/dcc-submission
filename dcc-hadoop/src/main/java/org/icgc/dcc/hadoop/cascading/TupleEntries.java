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

import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

/**
 * Utility class to help with the {@link TupleEntry} object from cascading.
 */
public class TupleEntries {

  /**
   * Returns the list of {@link Fields} for a {@link TupleEntry}.
   */
  public static List<String> getFieldNames(TupleEntry entry) {
    return Fields2.getFieldNames(entry.getFields());
  }

  /**
   * 
   */
  public static Tuple getTuple(TupleEntry entry, Fields fieldName) {
    return getT(Tuple.class, entry, fieldName);
  }

  /**
   * @param entry
   * @return
   */
  private static <T> T getT(Class<T> clazz, TupleEntry entry, Fields fieldName) {
    Object object =
        checkNotNull(entry, "Expecting non-null entry").getObject(
            checkNotNull(fieldName, "Expecting non-null field name"));
    checkNotNull(object, "");
    checkState(clazz.isInstance(object), "%s was expected to be of type %s, %s instead",
        fieldName, clazz, object.getClass());
    @SuppressWarnings("unchecked")
    T object2 = (T) object;
    return object2;
  }

  /**
   * @param entry
   * @return
   */
  public static TupleEntry getTupleEntry(TupleEntry entry, Fields fieldName) {
    Object object = entry.getObject(fieldName);
    checkState(object instanceof TupleEntry, "");
    return (TupleEntry) object;
  }
}
