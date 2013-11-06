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
package org.icgc.dcc.submission.normalization.steps;

import static org.icgc.dcc.submission.validation.cascading.CascadingTestUtils.checkOperationResults;

import java.util.Iterator;

import org.icgc.dcc.submission.validation.cascading.CascadingTestUtils;
import org.junit.Test;

import cascading.CascadingTestCase;
import cascading.operation.Function;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

public class AlleleMaskingTest extends CascadingTestCase {

  @Test
  public void test_cascading_SensitiveRowMarker() {
    Function<?> function = new AlleleMasking.SensitiveRowMarker();

    Fields inputFields =
        new Fields("f1", "f2")
            .append(AlleleMasking.REFERENCE_GENOME_ALLELE_FIELD)
            .append(AlleleMasking.MUTATED_FROM_ALLELE_FIELD)
            .append(Masking.NORMALIZER_MASKING_FIELD);

    String dummyValue = "dummy";
    TupleEntry[] entries = new TupleEntry[] {
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue, "A", "A", Masking.OPEN)),
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue, "A", "G", Masking.OPEN)),
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue, "T", "C", Masking.OPEN)),
        new TupleEntry(inputFields, new Tuple(dummyValue, dummyValue, "C", "C", Masking.OPEN))
    };
    Fields resultFields =
        AlleleMasking.REFERENCE_GENOME_ALLELE_FIELD
            .append(AlleleMasking.MUTATED_FROM_ALLELE_FIELD)
            .append(Masking.NORMALIZER_MASKING_FIELD);

    Tuple[] resultTuples = new Tuple[] {
        new Tuple("A", "A", Masking.OPEN), // Untouched
        new Tuple("A", "G", Masking.CONTROLLED), // Marked
        new Tuple("T", "C", Masking.CONTROLLED), // Marked
        new Tuple("C", "C", Masking.OPEN) // Untouched
    };

    Iterator<TupleEntry> iterator = CascadingTestUtils.invokeFunction(function, entries, resultFields);
    checkOperationResults(iterator, resultTuples);
  }

  @Test
  public void test_cascading_MaskedRowGenerator() {
    Function<?> function = new AlleleMasking.MaskedRowGenerator();

    Fields inputFields =
        new Fields("f1", "f2")
            .append(AlleleMasking.CONTROL_GENOTYPE_FIELD)
            .append(AlleleMasking.TUMOUR_GENOTYPE_FIELD)
            .append(AlleleMasking.REFERENCE_GENOME_ALLELE_FIELD)
            .append(AlleleMasking.MUTATED_FROM_ALLELE_FIELD)
            .append(AlleleMasking.MUTATED_TO_ALLELE_FIELD)
            .append(Masking.NORMALIZER_MASKING_FIELD);

    String dummyValue = "dummy";
    Tuple open = // Just passed through as is
        new Tuple(dummyValue, dummyValue, "A/A", "A/T", "A", "A", "T", Masking.OPEN);
    Tuple nonTrivial = // They differ -> masked
        new Tuple(dummyValue, dummyValue, "A/G", "A/T", "A", "G", "T", Masking.CONTROLLED);
    Tuple trivial = // reference genome allele equals mutation_to -> not masked
        new Tuple(dummyValue, dummyValue, "A/G", "A/A", "A", "G", "A", Masking.CONTROLLED);

    TupleEntry[] entries = new TupleEntry[] {
        new TupleEntry(inputFields, open),
        new TupleEntry(inputFields, nonTrivial),
        new TupleEntry(inputFields, trivial)
    };
    Fields resultFields = inputFields;

    Iterator<TupleEntry> iterator = CascadingTestUtils.invokeFunction(function, entries, resultFields);

    Tuple[] resultTuples = new Tuple[] {
        open, // Untouched
        nonTrivial, // Untouched
        new Tuple(
            dummyValue, dummyValue,
            null, null, // Erased
            "A",
            "A", // Changed to match reference genome allele
            "T",
            Masking.MASKED), // Marked as masked
        trivial, // Untouched
    };
    checkOperationResults(iterator, resultTuples);
  }
}
