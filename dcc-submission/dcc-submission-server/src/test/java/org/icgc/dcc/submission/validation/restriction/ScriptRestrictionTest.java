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
package org.icgc.dcc.submission.validation.restriction;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.icgc.dcc.submission.validation.cascading.ValidationFields.STATE_FIELD_NAME;

import java.util.List;

import lombok.val;

import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.restriction.ScriptRestriction.ScriptFunction;
import org.junit.Test;

import cascading.operation.OperationException;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

public class ScriptRestrictionTest extends BaseRestrictionTest {

  @Test
  public void test_ScriptRestriction_describe() {
    ScriptRestriction restriction = new ScriptRestriction("x", "x > 0");

    assertThat(restriction.describe()).isEqualTo("script[x:x > 0]");
  }

  @Test
  public void test_ScriptFunction_pass() {
    val results = invokeFunction(
        script("x > 0 && x < y && y <= z"),
        row(
            "x", 1,
            "y", 2,
            "z", 3));

    assertThat(results).hasSize(1);

    val result = results.get(0);
    assertThat(result.getObject(0)).isEqualTo(1);
    assertThat(result.getObject(1)).isEqualTo(2);
    assertThat(result.getObject(2)).isEqualTo(3);

    val state = (TupleState) result.getObject(3);
    assertTrue(state.isValid());
  }

  @Test
  public void test_ScriptFunction_fail() {
    val results = invokeFunction(
        script("x == 0"),
        row("x", 1));

    assertThat(results).hasSize(1);

    val result = results.get(0);

    val x = result.getObject(0);
    assertThat(x).isEqualTo(1);

    val state = (TupleState) result.getObject(1);
    assertTrue(state.isInvalid());
  }

  @Test(expected = OperationException.class)
  public void test_ScriptFunction_error_return_type() {
    invokeFunction(
        script("1"),
        row("x", 1));
  }

  private static List<Tuple> invokeFunction(String script, Object... values) {
    checkArgument(values.length % 2 == 0);

    // Pairwise splitting
    Fields fields = new Fields();
    Tuple tuple = new Tuple();
    for (int i = 0; i < values.length; i += 2) {
      fields = fields.append(new Fields(values[i].toString()));
      tuple = tuple.append(new Tuple(values[i + 1]));
    }

    // Simulate upstream produced state
    fields = fields.append(new Fields(STATE_FIELD_NAME));
    tuple = tuple.append(new Tuple(new TupleState()));

    // Simulate a singleton tuple stream
    val tupleEntry = new TupleEntry(fields, tuple);
    val tuples = new TupleEntry[] { tupleEntry };
    val function = new ScriptFunction(script);
    val results = invokeFunction(function, tuples, fields);

    return newArrayList(results.iterator());
  }

  private static String script(String value) {
    return value;
  }

  private static Object[] row(Object... values) {
    return values;
  }

}
