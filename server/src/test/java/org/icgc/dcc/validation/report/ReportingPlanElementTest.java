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

import org.icgc.dcc.validation.report.BaseReportingPlanElement.FieldSummary;
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
