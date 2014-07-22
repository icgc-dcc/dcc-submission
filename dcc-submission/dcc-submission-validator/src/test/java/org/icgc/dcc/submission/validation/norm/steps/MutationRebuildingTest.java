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
package org.icgc.dcc.submission.validation.norm.steps;

import static org.icgc.dcc.submission.validation.cascading.CascadingTestUtils.checkOperationResults;

import java.util.Iterator;

import org.icgc.dcc.submission.validation.cascading.CascadingTestUtils;
import org.junit.Test;

import cascading.operation.Function;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

public class MutationRebuildingTest {

  @Test
  public void test_cascading_MutationRebuilder() {
    Function<?> function = new MutationRebuilding.MutationRebuilder();

    Fields inputFields =
        new Fields("f1", "f2")
            .append(MutationRebuilding.MUTATED_FROM_ALLELE_FIELD)
            .append(MutationRebuilding.MUTATED_TO_ALLELE_FIELD);

    String dummyValue = "dummy";
    TupleEntry[] entries = new TupleEntry[] {
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue, "A", "G")),
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue, "A", "C")),
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue, "T", "C"))
    };
    Fields resultFields = MutationRebuilding.MUTATION_FIELD;

    Tuple[] resultTuples = new Tuple[] {
        new Tuple("A>G"),
        new Tuple("A>C"),
        new Tuple("T>C")
    };

    Iterator<TupleEntry> iterator = CascadingTestUtils.invokeFunction(function, entries, resultFields);
    checkOperationResults(iterator, resultTuples);
  }
}
