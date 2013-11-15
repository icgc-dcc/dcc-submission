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
package org.icgc.dcc.submission.validation.semantic;

import static com.google.common.util.concurrent.Futures.allAsList;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.icgc.dcc.submission.validation.core.ErrorType.REFERENCE_GENOME_ERROR;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;

import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceGenomeValidatorConcurrencyTest {

  private static final String TEST_DIR = "src/test/resources/fixtures/validation/rgv";

  private ReferenceGenomeValidator validator;

  @Before
  public void setup() {
    validator = new ReferenceGenomeValidator("/tmp/GRCh37.fasta");
  }

  @Test
  @SneakyThrows
  public void testConcurrent() throws IOException {
    val fileName = "ssm_p.txt";
    val n = 100;
    val executor = createExecutor(n);

    val callables = Lists.<Callable<ValidationContext>> newArrayList();
    for (int i = 0; i < n; i++) {
      val context = createContext(fileName);

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
      verify(context, times(7)).reportError(
          eq(fileName),
          anyLong(),
          eq("reference_genome_allele"),
          anyString(),
          eq(REFERENCE_GENOME_ERROR),
          anyVararg());
    }
  }

  private static ListeningExecutorService createExecutor(final int n) {
    return listeningDecorator(newFixedThreadPool(n));
  }

  @SneakyThrows
  private static ValidationContext createContext(String fileName) {
    val context = mock(ValidationContext.class);
    val fileSystem = FileSystem.getLocal(new Configuration());
    when(context.getFileSystem()).thenReturn(fileSystem);

    // Setup: Establish input for the test
    val ssmPrimaryFile = Optional.<Path> of(new Path(TEST_DIR, fileName));
    when(context.getSsmPrimaryFile()).thenReturn(ssmPrimaryFile);

    return context;
  }

  @SneakyThrows
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private static ListenableFuture<List<ValidationContext>> getResults(ListeningExecutorService executor,
      List<Callable<ValidationContext>> callables) {
    return allAsList((List) executor.invokeAll(callables));
  }
}
