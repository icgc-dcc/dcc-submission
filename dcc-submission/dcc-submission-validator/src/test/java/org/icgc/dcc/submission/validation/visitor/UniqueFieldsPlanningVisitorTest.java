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
package org.icgc.dcc.submission.validation.visitor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.visitor.UniqueFieldsPlanningVisitor.UniqueFieldsPlanElement.CountBuffer;
import org.junit.Test;

import cascading.CascadingTestCase;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleListCollector;

public class UniqueFieldsPlanningVisitorTest {

  @Test
  public void test_CountBuffer_identifiesNonUnique() {
    List<String> fields = new ArrayList<String>();
    fields.add("id");
    CountBuffer buffer = new CountBuffer(fields);
    Fields incoming = new Fields("id", "_state");

    // Note that CountBuffer expects grouped input, so the actual values will already be the same in each group
    TupleEntry[] tuples =
        new TupleEntry[] { new TupleEntry(incoming, new Tuple("1", new TupleState())), new TupleEntry(incoming,
            new Tuple("1", new TupleState())) };

    TupleListCollector c = CascadingTestCase.invokeBuffer(buffer, tuples, incoming);

    Iterator<Tuple> iterator = c.iterator();

    Tuple t = iterator.next();
    TupleState state = (TupleState) t.getObject(1);
    assertTrue(state.isValid());

    t = iterator.next();
    state = (TupleState) t.getObject(1);
    assertFalse(state.isValid());
  }

  @Test
  public void test_CountBuffer_passesUnique() {
    List<String> fields = new ArrayList<String>();
    fields.add("id");
    CountBuffer buffer = new CountBuffer(fields);
    Fields incoming = new Fields("id", "_state");

    TupleEntry[] tuples = new TupleEntry[] { new TupleEntry(incoming, new Tuple("1", new TupleState())) };

    TupleListCollector c = CascadingTestCase.invokeBuffer(buffer, tuples, incoming);

    Iterator<Tuple> iterator = c.iterator();

    Tuple t = iterator.next();
    TupleState state = (TupleState) t.getObject(1);
    assertTrue(state.isValid());
  }
}
