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

import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.visitor.RelationPlanningVisitor.RelationPlanElement.NoNullBuffer;
import org.junit.Test;

import cascading.CascadingTestCase;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleListCollector;

/**
 * 
 */
public class RelationPlanningVisitorTest extends CascadingTestCase {

  @Test
  public void test_validField() {
    String[] lhsFields = { "fk" };
    String rhs = "fileName";
    String[] rhsFields = { "pk" };

    NoNullBuffer buffer = new NoNullBuffer(lhsFields, rhs, rhsFields);

    Fields resultField = new Fields("_state");
    TupleEntry[] tuples =
        new TupleEntry[] { new TupleEntry(resultField, new Tuple("value", "value", new TupleState())) };

    TupleListCollector c = CascadingTestCase.invokeBuffer(buffer, tuples, resultField);
    assertEquals(c.size(), 0);
  }

  @Test
  public void test_null() {
    String[] lhsFields = { "fk" };
    String rhs = "fileName";
    String[] rhsFields = { "pk" };

    TupleState state = this.test_NoNullBuffer("value", null, lhsFields, rhs, rhsFields);
    assertTrue(state.isInvalid());
  }

  private TupleState test_NoNullBuffer(Object rhsTupleValue, Object lhsTupleValue, String[] lhsFields, String rhs,
      String[] rhsFields) {
    NoNullBuffer buffer = new NoNullBuffer(lhsFields, rhs, rhsFields);

    Fields resultField = new Fields("_state");
    TupleEntry[] tuples =
        new TupleEntry[] { new TupleEntry(resultField, new Tuple(rhsTupleValue, lhsTupleValue, new TupleState())) };

    TupleListCollector c = CascadingTestCase.invokeBuffer(buffer, tuples, resultField);

    Iterator<Tuple> iterator = c.iterator();

    Tuple t = iterator.next();

    TupleState state = (TupleState) t.getObject(0);

    return state;
  }
}
