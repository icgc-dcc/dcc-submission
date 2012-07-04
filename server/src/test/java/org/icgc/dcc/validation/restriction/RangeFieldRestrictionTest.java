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
import org.icgc.dcc.validation.restriction.RangeFieldRestriction.RangeFunction;
import org.junit.Test;

import cascading.CascadingTestCase;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleListCollector;

/**
 * 
 */
public class RangeFieldRestrictionTest extends CascadingTestCase {

  @Test
  public void test_range_field_restriction() {
    RangeFieldRestriction restriction = new RangeFieldRestriction("number", new Integer(1), new Integer(10));

    assertEquals("range[1-10]", restriction.describe());

  }

  @Test
  public void test_range_lower_bound() {
    TupleState state = this.test_range_function(new Integer(1), new Integer(1), new Integer(10));
    assertTrue(state.isValid());
  }

  @Test
  public void test_range_upper_bound() {
    TupleState state = this.test_range_function(new Integer(10), new Integer(1), new Integer(10));
    assertTrue(state.isValid());
  }

  @Test
  public void test_range_out_bound() {
    TupleState state = this.test_range_function(new Integer(0), new Integer(1), new Integer(10));
    assertTrue(state.isInvalid());
  }

  @Test
  public void test_range_negative() {
    TupleState state = this.test_range_function(new Integer(-1), new Integer(1), new Integer(10));
    assertTrue(state.isInvalid());
  }

  @Test
  public void test_empty_string() {
    TupleState state = this.test_range_function("", new Integer(1), new Integer(10));
    assertTrue(state.isInvalid());
  }

  private TupleState test_range_function(Object tupleValue, Number minValue, Number maxValue) {
    RangeFunction function = new RangeFunction(minValue, maxValue);

    Fields incoming = new Fields("number", "_state");
    TupleEntry[] tuples = new TupleEntry[] { new TupleEntry(incoming, new Tuple(tupleValue, new TupleState())) };

    TupleListCollector c = CascadingTestCase.invokeFunction(function, tuples, incoming);

    Iterator<Tuple> iterator = c.iterator();

    Tuple t = iterator.next();

    assertEquals(tupleValue, t.getObject(0));
    TupleState state = (TupleState) t.getObject(1);

    return state;
  }
}
