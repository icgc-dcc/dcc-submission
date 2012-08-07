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
package org.icgc.dcc.validation.report;

import java.util.Iterator;

import org.icgc.dcc.validation.report.AggregateReportingPlanElement.AggregateSummaryFunction;
import org.icgc.dcc.validation.report.AggregateReportingPlanElement.CompletenessBuffer;
import org.icgc.dcc.validation.report.AggregateReportingPlanElement.FieldToValueFunction;
import org.icgc.dcc.validation.report.BaseStatsReportingPlanElement.FieldSummary;
import org.icgc.dcc.validation.report.FrequencyPlanElement.FrequencySummaryBuffer;
import org.junit.Test;

import cascading.CascadingTestCase;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleListCollector;

import com.google.common.collect.Maps;

public class ReportingPlanElementTest extends CascadingTestCase {

  @Test
  public void test_FieldToValueFunction_operate() {

    Fields argumentFields = new Fields("col1", "col2", "col3", "col4");
    FieldToValueFunction function = new FieldToValueFunction(argumentFields.size());
    Fields resultFields = new Fields("field", "value");

    TupleEntry[] tuples = new TupleEntry[] {//
        new TupleEntry(argumentFields, new Tuple("v.1.1", "v.1.2", "v.1.3", "v.1.4")),//
        new TupleEntry(argumentFields, new Tuple("v.2.1", "v.2.2", "v.2.3", "v.2.4")),//
        new TupleEntry(argumentFields, new Tuple("v.3.1", "v.3.2", "v.3.3", "v.3.4")),//
        new TupleEntry(argumentFields, new Tuple("v.4.1", "v.4.2", "v.4.3", "v.4.4")) //
        };

    TupleListCollector c = CascadingTestCase.invokeFunction(function, tuples, resultFields);
    int expectedSize = argumentFields.size() * tuples.length;
    assertEquals(c.size(), expectedSize);
    Iterator<Tuple> iterator = c.iterator();

    checkTuple(iterator, new Tuple("col1", "v.1.1"));
    checkTuple(iterator, new Tuple("col2", "v.1.2"));
    checkTuple(iterator, new Tuple("col3", "v.1.3"));
    checkTuple(iterator, new Tuple("col4", "v.1.4"));

    checkTuple(iterator, new Tuple("col1", "v.2.1"));
    checkTuple(iterator, new Tuple("col2", "v.2.2"));
    checkTuple(iterator, new Tuple("col3", "v.2.3"));
    checkTuple(iterator, new Tuple("col4", "v.2.4"));

    checkTuple(iterator, new Tuple("col1", "v.3.1"));
    checkTuple(iterator, new Tuple("col2", "v.3.2"));
    checkTuple(iterator, new Tuple("col3", "v.3.3"));
    checkTuple(iterator, new Tuple("col4", "v.3.4"));

    checkTuple(iterator, new Tuple("col1", "v.4.1"));
    checkTuple(iterator, new Tuple("col2", "v.4.2"));
    checkTuple(iterator, new Tuple("col3", "v.4.3"));
    checkTuple(iterator, new Tuple("col4", "v.4.4"));

    assertFalse(iterator.hasNext());
  }

  @Test
  public void test_AggregateSummaryFunction_operate() {

    Fields argumentFields = new Fields("field", "nulls", "populated", "min", "max", "avg", "stddev");
    AggregateSummaryFunction function = new AggregateSummaryFunction(true, true);
    Fields resultFields = new Fields("report");

    TupleEntry[] tuples = new TupleEntry[] {//
        new TupleEntry(argumentFields, new Tuple("col1", 0, 20, 5, 15, 10, 5)),//
        new TupleEntry(argumentFields, new Tuple("col2", 3, 17, 50, 150, 100, 50)),//
        };

    TupleListCollector c = CascadingTestCase.invokeFunction(function, tuples, resultFields);
    assertEquals(2, c.size());

    FieldSummary fieldSummary1 = new FieldSummary();
    fieldSummary1.field = "col1";
    fieldSummary1.nulls = 0;
    fieldSummary1.populated = 20;
    fieldSummary1.summary = Maps.newLinkedHashMap();
    fieldSummary1.summary.put("min", 5);
    fieldSummary1.summary.put("max", 15);
    fieldSummary1.summary.put("avg", 10);
    fieldSummary1.summary.put("stddev", 5);

    FieldSummary fieldSummary2 = new FieldSummary();
    fieldSummary2.field = "col2";
    fieldSummary2.nulls = 3;
    fieldSummary2.populated = 17;
    fieldSummary2.summary = Maps.newLinkedHashMap();
    fieldSummary2.summary.put("min", 50);
    fieldSummary2.summary.put("max", 150);
    fieldSummary2.summary.put("avg", 100);
    fieldSummary2.summary.put("stddev", 50);

    Iterator<Tuple> iterator = c.iterator();
    checkFieldSummaryTuple(iterator, fieldSummary1.toString());
    checkFieldSummaryTuple(iterator, fieldSummary2.toString());

    assertFalse(iterator.hasNext());
  }

  @Test
  public void test_FrequencySummaryBuffer_operate() {

    Fields argumentFields = new Fields("value", "freq");
    FrequencySummaryBuffer buffer = new FrequencySummaryBuffer();
    Fields resultFields = new Fields("report");

    TupleEntry group = new TupleEntry(argumentFields, new Tuple("col1"));
    TupleEntry[] tuples = new TupleEntry[] {//
        new TupleEntry(argumentFields, new Tuple(null, 10)),//
        new TupleEntry(argumentFields, new Tuple("male", 22)),//
        new TupleEntry(argumentFields, new Tuple("female", 33)),//
        };

    TupleListCollector c = CascadingTestCase.invokeBuffer(buffer, group, tuples, resultFields);
    assertEquals(1, c.size());

    FieldSummary fieldSummary = new FieldSummary();
    fieldSummary.field = "col1";
    fieldSummary.nulls = 10;
    fieldSummary.populated = 55;
    fieldSummary.summary = Maps.newLinkedHashMap();
    fieldSummary.summary.put("male", 22);
    fieldSummary.summary.put("female", 33);

    Iterator<Tuple> iterator = c.iterator();
    checkFieldSummaryTuple(iterator, fieldSummary.toString());

    assertFalse(iterator.hasNext());
  }

  @Test
  public void test_CompletenessBuffer_operate() {

    Fields argumentFields = new Fields("field", "nulls", "populated", "min", "max", "avg", "stddev");
    CompletenessBuffer buffer = new CompletenessBuffer();
    Fields resultFields = new Fields("nulls", "populated");

    // TODO: emulate grouping?
    TupleEntry[] tuples = new TupleEntry[] {//
        new TupleEntry(argumentFields, new Tuple("v1")),//
        new TupleEntry(argumentFields, new Tuple((String) null)),//
        new TupleEntry(argumentFields, new Tuple((String) null)),//
        new TupleEntry(argumentFields, new Tuple("v2")),//
        new TupleEntry(argumentFields, new Tuple("v3")),//
        };

    TupleListCollector c = CascadingTestCase.invokeBuffer(buffer, tuples, resultFields);
    assertEquals(1, c.size());

    Iterator<Tuple> iterator = c.iterator();
    checkTuple(iterator, new Tuple(2, 3));

    assertFalse(iterator.hasNext());
  }

  private void checkTuple(Iterator<Tuple> iterator, Tuple expectedTuple) {
    assertTrue(iterator.hasNext());
    Tuple next = iterator.next();
    assertEquals(2, next.size());
    assertEquals(expectedTuple, next);
  }

  private void checkFieldSummaryTuple(Iterator<Tuple> iterator, String expectedTupleString) {
    assertTrue(iterator.hasNext());
    Tuple next = iterator.next();
    assertEquals(1, next.size());
    assertEquals(expectedTupleString, next.toString());
  }
}
