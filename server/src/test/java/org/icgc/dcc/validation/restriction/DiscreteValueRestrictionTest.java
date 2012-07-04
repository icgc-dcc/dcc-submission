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
package org.icgc.dcc.validation.restriction;

import java.util.Iterator;

import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.restriction.DiscreteValuesRestriction.InValuesFunction;
import org.junit.Test;

import cascading.CascadingTestCase;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleListCollector;

/**
 * 
 */
public class DiscreteValueRestrictionTest extends CascadingTestCase {

  private static final String[] VALUES = { "A", "B", "C" };

  @Test
  public void test_discrete_value_restriction() {
    DiscreteValuesRestriction restriction = new DiscreteValuesRestriction("field", VALUES);

    assertEquals("in[field:[A, B, C]]", restriction.describe());

  }

  @Test
  public void test_valid_value() {
    TupleState state = this.test_discrete_function("A", VALUES);
    assertTrue(state.isValid());
  }

  @Test
  public void test_null() {
    TupleState state = this.test_discrete_function(null, VALUES);
    assertTrue(state.isInvalid());
  }

  @Test
  public void test_invalid_type() {
    TupleState state = this.test_discrete_function(35, VALUES);
    assertTrue(state.isInvalid());
  }

  @Test
  public void test_empty_string() {
    TupleState state = this.test_discrete_function("", VALUES);
    assertTrue(state.isInvalid());
  }

  private TupleState test_discrete_function(Object tupleValue, String[] values) {
    InValuesFunction function = new InValuesFunction(values);

    Fields incoming = new Fields("field", "_state");
    TupleEntry[] tuples = new TupleEntry[] { new TupleEntry(incoming, new Tuple(tupleValue, new TupleState())) };

    TupleListCollector c = CascadingTestCase.invokeFunction(function, tuples, incoming);

    Iterator<Tuple> iterator = c.iterator();

    Tuple t = iterator.next();

    assertEquals(tupleValue, t.getObject(0));
    TupleState state = (TupleState) t.getObject(1);

    return state;
  }
}
