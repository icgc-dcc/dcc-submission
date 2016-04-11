/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.rgv;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE;
import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.core.report.ErrorType.REFERENCE_GENOME_INSERTION_ERROR;
import static org.icgc.dcc.submission.core.report.ErrorType.REFERENCE_GENOME_MISMATCH_ERROR;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.Callable;

import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import lombok.SneakyThrows;
import lombok.val;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceGenomeValidatorConcurrencyTest extends BaseReferenceGenomeValidatorTest {

  @Test
  public void testConcurrent() throws Exception {
    val testFile = TEST_FILE_NAME;
    val n = 20;
    val executor = createExecutor(n);

    val callables = Lists.<Callable<ValidationContext>> newArrayList();

    for (int i = 0; i < n; i++) {
      val context = mockContext();

      callables.add(new Callable<ValidationContext>() {

        @Override
        public ValidationContext call() throws Exception {
          // Execute
          validator.validate(context);

          return context;
        }

      });
    }

    val results = getResults(executor, callables);
    for (val context : results.get()) {
      // Verify
      verify(context, times(1)).reportError(eq(
          error()
              .fileName(testFile)
              .fieldNames(SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE)
              .lineNumber(2)
              .type(REFERENCE_GENOME_MISMATCH_ERROR)
              .value("Expected: A, Actual: C")
              .params("GRCh37")
              .build()));
      verify(context, times(1)).reportError(eq(
          error()
              .fileName(testFile)
              .fieldNames(SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE)
              .lineNumber(3)
              .type(REFERENCE_GENOME_MISMATCH_ERROR)
              .value("Expected: T, Actual: C")
              .params("GRCh37")
              .build()));
      verify(context, times(1)).reportError(eq(
          error()
              .fileName(testFile)
              .fieldNames(SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE)
              .lineNumber(4)
              .type(REFERENCE_GENOME_MISMATCH_ERROR)
              .value("Expected: T, Actual: G")
              .params("GRCh37")
              .build()));
      verify(context, times(1)).reportError(eq(
          error()
              .fileName(testFile)
              .fieldNames(SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE)
              .lineNumber(5)
              .type(REFERENCE_GENOME_INSERTION_ERROR)
              .value("Expected: -, Actual: A")
              .params("GRCh37")
              .build()));
    }
  }

  private static ListeningExecutorService createExecutor(final int n) {
    return listeningDecorator(newFixedThreadPool(n));
  }

  @SneakyThrows
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static ListenableFuture<List<ValidationContext>> getResults(ListeningExecutorService executor,
      List<Callable<ValidationContext>> callables) {
    return allAsList((List) executor.invokeAll(callables));
  }

}
