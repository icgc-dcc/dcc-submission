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
package org.icgc.dcc.submission.validation.primary.restriction;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.icgc.dcc.submission.validation.cascading.ValidationFields.STATE_FIELD_NAME;

import java.util.List;

import lombok.val;

import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.primary.restriction.ScriptRestriction.InvalidScriptException;
import org.icgc.dcc.submission.validation.primary.restriction.ScriptRestriction.ScriptFunction;
import org.junit.Test;

import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

public class ScriptRestrictionTest extends BaseRestrictionTest {

  final static int NUMBER = 0;

  @Test
  public void test_ScriptRestriction_describe() {
    ScriptRestriction restriction = new ScriptRestriction("x", NUMBER, "x > 0");

    assertThat(restriction.describe()).isEqualTo("script[x:x > 0]");
  }

  @Test
  public void test_ScriptFunction_pass() {
    val results = invokeFunction(
        script("x > 0 && x < y && y <= z"),
        description("x, y, z are increasing"),
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
        description("x is zero"),
        row("x", 1));

    assertThat(results).hasSize(1);

    val result = results.get(0);

    val x = result.getObject(0);
    assertThat(x).isEqualTo(1);

    val state = (TupleState) result.getObject(1);
    assertTrue(state.isInvalid());
  }

  @Test
  public void test_ScriptFunction_runtime_exception() {
    val results = invokeFunction(
        script("x.toString() == null"),
        description("null check"),
        row("x", null));

    assertThat(results).hasSize(1);

    val result = results.get(0);

    val x = result.getObject(0);
    assertThat(x).isNull();

    val state = (TupleState) result.getObject(1);
    assertTrue(state.isInvalid());
  }

  @Test(expected = InvalidScriptException.class)
  public void test_ScriptFunction_compile_error_return_type() {
    invokeFunction(
        script("1"),
        description("x is one"),
        row("x", 1));
  }

  @Test
  public void test_ScriptFunction_compile_branch() {
    val results = invokeFunction(
        script("if ([1,2,3,4] contains mutation_type) { true } else { false }"),
        description("mutation_type"),
        row("mutation_type", 1));

    assertThat(results).hasSize(1);

    val result = results.get(0);

    val mutation_type = result.getObject(0);
    assertThat(mutation_type).isEqualTo(1);

    val state = (TupleState) result.getObject(1);
    assertTrue(state.isValid());
  }

  private static List<Tuple> invokeFunction(String script, String description, Object... values) {
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
    val function = new ScriptFunction("fieldName", NUMBER, script);
    val results = invokeFunction(function, tuples, fields);

    return newArrayList(results.iterator());
  }

  private static String script(String value) {
    return value;
  }

  private static String description(String value) {
    return value;
  }

  private static Object[] row(Object... values) {
    return values;
  }

}
