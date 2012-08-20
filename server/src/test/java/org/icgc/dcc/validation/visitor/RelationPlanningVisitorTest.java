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
package org.icgc.dcc.validation.visitor;

import java.util.Iterator;

import org.icgc.dcc.validation.cascading.ValidationFields;
import org.icgc.dcc.validation.visitor.RelationPlanningVisitor.RelationPlanElement.NoNullBuffer;
import org.junit.Test;

import cascading.CascadingTestCase;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleListCollector;

import com.google.common.collect.ObjectArrays;

/**
 * 
 */
public class RelationPlanningVisitorTest extends CascadingTestCase {

  private final String lhs = "referencing";

  private final String rhs = "referenced";

  private final String[] lhsFields = { "offset", "fk1", "fk2", "fk3" };

  private final String[] rhsFields = { "pk1", "pk2", "pk3" };

  private final Fields inputFields = new Fields(ObjectArrays.concat(lhsFields, rhsFields, String.class));

  @Test
  public void test_operate_valid() {

    NoNullBuffer buffer = new NoNullBuffer(lhs, rhs, lhsFields, rhsFields);

    TupleEntry[] tuples =
        new TupleEntry[] { new TupleEntry(new Fields(ObjectArrays.concat(lhsFields, rhsFields, String.class)),
            new Tuple("0", "value1", "value2", "value3",//
                "value1", "value2", "value3")) };

    Fields resultField = new Fields("_state");
    TupleListCollector c = CascadingTestCase.invokeBuffer(buffer, tuples, resultField);
    assertEquals(0, c.size());
  }

  @Test
  public void test_operate_invalid() {
    NoNullBuffer buffer = new NoNullBuffer(lhs, rhs, lhsFields, rhsFields);

    TupleEntry[] tuples = new TupleEntry[] {//
        new TupleEntry(inputFields,//
            new Tuple("0", "value21", "value22", "value23",//
                null, null, null)) };

    Fields resultField = new Fields("_state");
    TupleListCollector c = CascadingTestCase.invokeBuffer(buffer, tuples, resultField);
    assertEquals(1, c.size());
    assertTrue(ValidationFields.state(c.entryIterator().next()).isInvalid());
  }

  @Test
  public void test_operate_mix() {
    NoNullBuffer buffer = new NoNullBuffer(lhs, rhs, lhsFields, rhsFields);

    TupleEntry[] tuples = new TupleEntry[] {//
        new TupleEntry(inputFields, new Tuple(//
            "0", "value1", "value2", "value3",//
            "value1", "value2", "value3")),//
        new TupleEntry(inputFields, new Tuple(//
            "0", "value21", "value22", "value23",//
            null, null, null)),//
        new TupleEntry(inputFields, new Tuple(//
            "0", "value11", "value12", "value13",//
            "value11", "value12", "value13")),//
        new TupleEntry(inputFields, new Tuple(//
            "0", "value41", "value42", "value43",//
            null, null, null)), };

    Fields resultField = new Fields("_state");
    TupleListCollector c = CascadingTestCase.invokeBuffer(buffer, tuples, resultField);
    assertEquals(2, c.size());

    Iterator<TupleEntry> entryIterator = c.entryIterator();
    assertTrue(ValidationFields.state(entryIterator.next()).isInvalid());
    assertTrue(ValidationFields.state(entryIterator.next()).isInvalid());
  }

  // TODO: add test for conditional FK
}
