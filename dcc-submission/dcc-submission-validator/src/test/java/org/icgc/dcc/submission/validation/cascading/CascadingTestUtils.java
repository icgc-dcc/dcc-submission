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
package org.icgc.dcc.submission.validation.cascading;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.icgc.dcc.hadoop.cascading.Tuples2.sameContent;

import java.util.Iterator;

import cascading.CascadingTestCase;
import cascading.operation.Buffer;
import cascading.operation.Function;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

/**
 * Create dcc-test-hadoop (see DCC-2415)
 */
public class CascadingTestUtils {

  public static Iterator<TupleEntry> invokeFunction(Function<?> function, TupleEntry[] entries, Fields resultFields) {
    return CascadingTestCase
        .invokeFunction(
            function,
            entries,
            resultFields)
        .entryIterator();
  }

  public static Iterator<TupleEntry> invokeBuffer(Buffer<?> buffer, TupleEntry[] entries, Fields resultFields) {
    return CascadingTestCase
        .invokeBuffer(
            buffer,
            entries,
            resultFields)
        .entryIterator();
  }

  public static void checkOperationResults(Iterator<TupleEntry> iterator, Tuple[] resultTuples) {
    for (int i = 0; i < resultTuples.length; i++) {
      assertThat(iterator.hasNext());
      TupleEntry entry = iterator.next();
      Tuple actualTuple = entry.getTuple();
      Tuple expectedTuple = resultTuples[i];
      assertTrue(
          String.format("%s != %s",
              actualTuple,
              expectedTuple),
          sameContent(
              entry.getTuple(),
              expectedTuple));
    }
    assertFalse(iterator.hasNext());
  }
}
