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

import static org.icgc.dcc.hadoop.cascading.Cascades.cascadingSerialize;

import java.util.Iterator;

import org.icgc.dcc.submission.validation.cascading.CascadingTestUtils;
import org.icgc.dcc.submission.validation.cascading.CascadingFunctions.AvailableDataTypes;
import org.icgc.dcc.submission.validation.cascading.CascadingFunctions.CloneField;
import org.icgc.dcc.submission.validation.cascading.CascadingFunctions.Counter;
import org.icgc.dcc.hadoop.cascading.CascadingFunctions;
import org.icgc.dcc.hadoop.import org.icgc.dcc.submission.validation.cascading.CascadingFunctions.ReplaceNulls;
cascading.CascadingFunctions.EmitNothing;
import org.junit.Test;

import cascading.CascadingTestCase;
import cascading.operation.Function;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

public class CascadingFunctionsTest extends CascadingTestCase {

  private static final Fields DUMMY_FIELD = new Fields("");

  @Test
  public void test_serialization() {
    cascadingSerialize(new EmitNothing());
    cascadingSerialize(new CascadingFunctions.Counter(null, 1L));
    cascadingSerialize(new CascadingFunctions.CloneField(DUMMY_FIELD, DUMMY_FIELD));
    cascadingSerialize(new CascadingFunctions.AvailableDataTypes(DUMMY_FIELD));
    cascadingSerialize(new CascadingFunctions.ReplaceNulls(false));
  }

  @Test
  public void test_cascading_EmitNothing() {
    Function<?> function = new CascadingFunctions.EmitNothing();

    Fields inputFields = new Fields("f1", "f2");
    String dummyValue = "dummy";
    TupleEntry[] entries = new TupleEntry[] {
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue)),
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue)),
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue))
    };
    Fields resultFields = inputFields;

    Iterator<TupleEntry> iterator = CascadingTestUtils.invokeFunction(function, entries, resultFields);
    assertFalse(iterator.hasNext());
  }

  private enum DummyCounterType {
    DUMMY
  }

  /**
   * TODO: how to test counters?
   */
  @Test
  public void test_cascading_Counter() {
    Function<?> function = new CascadingFunctions.Counter(DummyCounterType.DUMMY, 1);

    Fields inputFields = new Fields("f1", "f2");
    String dummyValue = "dummy";
    TupleEntry[] entries = new TupleEntry[] {
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue)),
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue)),
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue))
    };
    Fields resultFields = inputFields;

    Iterator<TupleEntry> iterator = CascadingTestUtils.invokeFunction(function, entries, resultFields);
    for (int i = 0; i < entries.length; i++) {
      assertTrue(iterator.hasNext());
      iterator.next();
    }
    assertFalse(iterator.hasNext());
  }
}
