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
package org.icgc.dcc.validation;

import java.util.Iterator;

import org.icgc.dcc.dictionary.model.ValueType;
import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.cascading.ValidationFields;
import org.icgc.dcc.validation.visitor.ValueTypePlanningVisitor.ValueTypePlanElement.ValueTypeFunction;
import org.junit.Test;

import cascading.CascadingTestCase;
import cascading.operation.Function;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleListCollector;

public class ValueTypeFunctionTest extends CascadingTestCase {

  protected static final String FIELD_NAME = "my_field";

  @Test
  public void test_operate_integerTests() {

    @SuppressWarnings("rawtypes")
    Function function = new ValueTypeFunction(ValueType.INTEGER);
    ValidationFields resultFields = new ValidationFields(FIELD_NAME);
    TupleEntry[] argumentsArray = new TupleEntry[] {//
        new TupleEntry(resultFields, new Tuple("2", new TupleState())),// ok: basic
        new TupleEntry(resultFields, new Tuple("-4", new TupleState())),// ok: negative
        new TupleEntry(resultFields, new Tuple("23409823049", new TupleState())),// ok: big
        new TupleEntry(resultFields,
            new Tuple("2340982304459834958743985794835787398579878539487359", new TupleState())),// wrong: too big
        new TupleEntry(resultFields, new Tuple("abc", new TupleState())),// wrong: string
        new TupleEntry(resultFields, new Tuple("3.5", new TupleState())),// wrong: decimal
        new TupleEntry(resultFields, new Tuple("", new TupleState())),// wrong: empty
        };

    TupleListCollector tupleListCollector = CascadingTestCase.invokeFunction(function, argumentsArray, resultFields);
    Iterator<TupleEntry> entryIterator = tupleListCollector.entryIterator();
    handleValid(entryIterator.next(), FIELD_NAME, new Long(2));
    handleValid(entryIterator.next(), FIELD_NAME, new Long(-4));
    handleValid(entryIterator.next(), FIELD_NAME, new Long(23409823049L));
    handleInvalid(entryIterator.next(), FIELD_NAME, "2340982304459834958743985794835787398579878539487359");
    handleInvalid(entryIterator.next(), FIELD_NAME, "abc");
    handleInvalid(entryIterator.next(), FIELD_NAME, "3.5");
    handleInvalid(entryIterator.next(), FIELD_NAME, "");
  }

  @Test
  public void test_operate_decimalTests() {

    @SuppressWarnings("rawtypes")
    Function function = new ValueTypeFunction(ValueType.DECIMAL);
    ValidationFields resultFields = new ValidationFields(FIELD_NAME);
    TupleEntry[] argumentsArray =
        new TupleEntry[] {//
        new TupleEntry(resultFields, new Tuple("2.5", new TupleState())),// ok: basic
        new TupleEntry(resultFields, new Tuple("2", new TupleState())),// ok: integer
        new TupleEntry(resultFields, new Tuple("-4.3", new TupleState())),// ok: negative
        new TupleEntry(resultFields, new Tuple("23409823049.3439", new TupleState())),// ok: big
        new TupleEntry(
            resultFields,
            new Tuple(
                // wrong: too big
                "10000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.5",
                new TupleState())),//
        new TupleEntry(resultFields, new Tuple("abc", new TupleState())),// wrong: string
        new TupleEntry(resultFields, new Tuple("", new TupleState())),// wrong: empty
        };
    TupleListCollector tupleListCollector = CascadingTestCase.invokeFunction(function, argumentsArray, resultFields);
    Iterator<TupleEntry> entryIterator = tupleListCollector.entryIterator();
    handleValid(entryIterator.next(), FIELD_NAME, new Double(2.5));
    handleValid(entryIterator.next(), FIELD_NAME, new Double(2));
    handleValid(entryIterator.next(), FIELD_NAME, new Double(-4.3));
    handleValid(entryIterator.next(), FIELD_NAME, new Double(23409823049.3439));
    handleInvalid(
        entryIterator.next(),
        FIELD_NAME,
        "10000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000.5");
    handleInvalid(entryIterator.next(), FIELD_NAME, "abc");
    handleInvalid(entryIterator.next(), FIELD_NAME, "");
  }

  private void handleValid(TupleEntry tupleEntry, String fieldName, Object expected) {
    assertEquals(2, tupleEntry.size());
    TupleState state = ValidationFields.state(tupleEntry);
    assertTrue("state = " + state, state.isValid());
    assertEquals(expected, tupleEntry.getObject(fieldName));
  }

  private void handleInvalid(TupleEntry tupleEntry, String fieldName, String expected) {
    assertEquals(2, tupleEntry.size());
    TupleState state = ValidationFields.state(tupleEntry);
    assertTrue("state = " + state, state.isInvalid());
    assertEquals(expected, tupleEntry.getObject(fieldName));
  }

}
