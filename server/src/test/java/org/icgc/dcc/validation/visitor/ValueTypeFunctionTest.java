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

import org.icgc.dcc.dictionary.model.ValueType;
import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.cascading.ValidationFields;
import org.icgc.dcc.validation.visitor.ValueTypePlanningVisitor.ValueTypePlanElement.ValueTypeFunction;
import org.junit.Test;

import cascading.CascadingTestCase;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleListCollector;

public class ValueTypeFunctionTest extends CascadingTestCase {

  @Test
  public void test_ValueTypeInteger_withInteger() {
    TupleState state = this.test_ValueTypeFunction("1", ValueType.INTEGER);
    assertTrue(state.isValid());
  }

  @Test
  public void test_ValueTypeInteger_withNegativeInteger() {
    TupleState state = this.test_ValueTypeFunction("-1", ValueType.INTEGER);
    assertTrue(state.isValid());
  }

  @Test
  public void test_ValueTypeInteger_withDouble() {
    TupleState state = this.test_ValueTypeFunction("1.0", ValueType.INTEGER);
    assertTrue(state.isInvalid());
  }

  @Test
  public void test_ValueTypeInteger_withNegativeDouble() {
    TupleState state = this.test_ValueTypeFunction("-1.0", ValueType.INTEGER);
    assertTrue(state.isInvalid());
  }

  @Test
  public void test_ValueTypeInteger_withString() {
    TupleState state = this.test_ValueTypeFunction("string", ValueType.INTEGER);
    assertTrue(state.isInvalid());
  }

  @Test
  public void test_ValueTypeInteger_withEmptyString() {
    TupleState state = this.test_ValueTypeFunction("", ValueType.INTEGER);
    assertTrue(state.isInvalid());
  }

  @Test
  public void test_ValueTypeInteger_withNull() {
    TupleState state = this.test_ValueTypeFunction(null, ValueType.INTEGER);
    assertTrue(state.isValid());
  }

  @Test
  public void test_ValueTypeDecimal_withInteger() {
    TupleState state = this.test_ValueTypeFunction("1", ValueType.DECIMAL);
    assertTrue(state.isValid());
  }

  @Test
  public void test_ValueTypeDecimal_withNegativeInteger() {
    TupleState state = this.test_ValueTypeFunction("-1", ValueType.DECIMAL);
    assertTrue(state.isValid());
  }

  @Test
  public void test_ValueTypeDecimal_withDouble() {
    TupleState state = this.test_ValueTypeFunction("1.0", ValueType.DECIMAL);
    assertTrue(state.isValid());
  }

  @Test
  public void test_ValueTypeDecimal_withNegativeDouble() {
    TupleState state = this.test_ValueTypeFunction("-1.0", ValueType.DECIMAL);
    assertTrue(state.isValid());
  }

  @Test
  public void test_ValueTypeDecimal_withString() {
    TupleState state = this.test_ValueTypeFunction("string", ValueType.DECIMAL);
    assertTrue(state.isInvalid());
  }

  @Test
  public void test_ValueTypeDecimal_withEmptyString() {
    TupleState state = this.test_ValueTypeFunction("", ValueType.DECIMAL);
    assertTrue(state.isInvalid());
  }

  @Test
  public void test_ValueTypeDecimal_withNull() {
    TupleState state = this.test_ValueTypeFunction(null, ValueType.DECIMAL);
    assertTrue(state.isValid());
  }

  private TupleState test_ValueTypeFunction(Object tupleValue, ValueType valueType) {
    ValueTypeFunction function = new ValueTypeFunction(valueType);

    ValidationFields resultFields = new ValidationFields("field");
    TupleEntry[] tuples = new TupleEntry[] { new TupleEntry(resultFields, new Tuple(tupleValue, new TupleState())) };

    TupleListCollector c = CascadingTestCase.invokeFunction(function, tuples, resultFields);

    Iterator<Tuple> iterator = c.iterator();

    Tuple t = iterator.next();
    assertEquals(2, t.size());
    TupleState state = (TupleState) t.getObject(1);

    return state;
  }

}
