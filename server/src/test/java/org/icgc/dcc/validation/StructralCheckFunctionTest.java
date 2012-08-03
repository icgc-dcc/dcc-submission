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

import java.util.Arrays;
import java.util.Iterator;

import junit.framework.Assert;

import org.icgc.dcc.validation.cascading.StructralCheckFunction;
import org.junit.Test;

import cascading.CascadingTestCase;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleListCollector;

public class StructralCheckFunctionTest {

  private final Fields LINE_FIELDS = new Fields("line");

  @Test
  public void test_operate_valid() {

    StructralCheckFunction function = new StructralCheckFunction(Arrays.asList("col1", "col2", "col3", "col4"));

    function.handleFileHeader(new Fields("col1", "col3", "col2", "col4"));
    TupleEntry[] tuples = new TupleEntry[] {//
        new TupleEntry(LINE_FIELDS, new Tuple("v.1.1\tv.1.3\tv.1.2\tv.1.4")),//
        new TupleEntry(LINE_FIELDS, new Tuple("v.2.1\tv.2.3\tv.2.2\tv.2.4")),//
        };

    Fields resultFields = new Fields("col1", "col3", "col2", "col4");
    Iterator<TupleEntry> iterator = callFunction(function, tuples, resultFields);

    checkTupleEntry(iterator, new TupleEntry(resultFields, new Tuple("v.1.1", "v.1.3", "v.1.2", "v.1.4")));
    checkTupleEntry(iterator, new TupleEntry(resultFields, new Tuple("v.2.1", "v.2.3", "v.2.2", "v.2.4")));

    Assert.assertFalse(iterator.hasNext());
  }

  @Test
  public void test_operate_missingColumns() {
    StructralCheckFunction function = new StructralCheckFunction(Arrays.asList("col1", "col2", "col3", "col4"));

    function.handleFileHeader(new Fields("col1", "col4"));
    TupleEntry[] tuples = new TupleEntry[] {//
        new TupleEntry(LINE_FIELDS, new Tuple("v.1.1\tv.1.4")),//
        new TupleEntry(LINE_FIELDS, new Tuple("v.2.1\tv.2.4")),//
        };

    Fields resultFields = new Fields("col1", "col4", "col2", "col3");
    Iterator<TupleEntry> iterator = callFunction(function, tuples, resultFields);

    checkTupleEntry(iterator, new TupleEntry(resultFields, new Tuple("v.1.1", "v.1.4", null, null)));
    checkTupleEntry(iterator, new TupleEntry(resultFields, new Tuple("v.2.1", "v.2.4", null, null)));

    Assert.assertFalse(iterator.hasNext());
  }

  private Iterator<TupleEntry> callFunction(StructralCheckFunction function, TupleEntry[] tuples, Fields resultFields) {
    TupleListCollector c = CascadingTestCase.invokeFunction(function, tuples, resultFields);
    Assert.assertEquals(c.size(), tuples.length);
    Iterator<TupleEntry> iterator = c.entryIterator();
    return iterator;
  }

  private void checkTupleEntry(Iterator<TupleEntry> iterator, TupleEntry expectedTupleEntry) {
    Assert.assertTrue(iterator.hasNext());
    TupleEntry next = iterator.next();
    Assert.assertEquals(4, next.size());
    Assert.assertEquals(//
        expectedTupleEntry.toString(),//
        next.toString());
  }
}
