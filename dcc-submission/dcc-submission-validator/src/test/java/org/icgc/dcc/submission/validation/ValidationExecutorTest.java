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
package org.icgc.dcc.submission.validation;

import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CancellationException;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.core.Validation;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;

public class ValidationExecutorTest {

  /**
   * Represents {@code validation.max_simutaneous}.
   */
  static final int MAX_VALIDATING = 3;

  /**
   * Class under test.
   */
  ValidationExecutor executor;

  @Before
  public void setUp() {
    // Roll-up sleeves
    executor = new ValidationExecutor(MAX_VALIDATING);
  }

  @After
  public void tearDown() {
    // Wash hands
    executor.shutdown();
  }

  @Test
  public void testExecute() {
    // "Promises" for results
    val futures = Lists.<ListenableFuture<Validation>> newArrayList();
    for (int i = 1; i <= MAX_VALIDATING; i++) {
      // Setup: Create the ith validation container
      val projectKey = "project" + i;
      val validation = createValidation(projectKey);

      // Exercise: Should "immediately" begin asynchronously
      val future = executor.execute(validation);
      futures.add(future);
    }

    // Sync: Wait for tasks to start running (timing not guaranteed)
    sleepUninterruptibly(1, SECONDS);

    // Verify: Ensure that things are running concurrently
    assertThat(executor.getActiveCount()).isEqualTo(MAX_VALIDATING);

    // Verify: Ensure all futures produced are in flight
    for (val future : futures) {
      assertThat(future.isDone()).isFalse();
      assertThat(future.isCancelled()).isFalse();
    }
  }

  @Test(expected = ValidationRejectedException.class)
  public void testRejected() {
    int oneMoreThanMax = MAX_VALIDATING + 1;
    for (int i = 1; i <= oneMoreThanMax; i++) {
      // Setup: Create the ith validation container
      val projectKey = "project" + i;
      val validation = createValidation(projectKey);

      // Exercise: Should be rejected on the last iteration
      executor.execute(validation);
    }
  }

  @Test
  public void testCancel() {
    // Setup: Create the validation container
    val projectKey = "project";
    val validation = createValidation(projectKey);

    // Setup: Start async validation
    val future = executor.execute(validation);

    // Exercise: Should succeed
    val firstCancelled = executor.cancel(projectKey);
    assertThat(firstCancelled).isTrue();
    assertThat(future.isCancelled()).isTrue();

    // Exercise: Should fail to find it
    val secondCancelled = executor.cancel(projectKey);
    assertThat(secondCancelled).isFalse();
    assertThat(future.isCancelled()).isTrue();

    // Verify: Things that happen upon "completion"
    addCallback(future, new FutureCallback<Validation>() {

      @Override
      public void onSuccess(Validation result) {
        // Nope
        fail("Validation should not have succeeded after it has been cancelled");
      }

      @Override
      public void onFailure(Throwable t) {
        // Yep
        assertThat(t).isInstanceOf(CancellationException.class).as("Unexpected exception type");
      }

    });
  }

  private static Validation createValidation(String projectKey) {
    // Very real
    val context = createValidationContext(projectKey);
    val validator = new TestValidator();
    val validators = Lists.<Validator> newArrayList(validator);

    return new Validation(context, validators);
  }

  private static ValidationContext createValidationContext(String projectKey) {
    // Can't use @Mock since we are 1:m
    val context = mock(ValidationContext.class);
    when(context.getProjectKey()).thenReturn(projectKey);

    return context;
  }

  /**
   * A simple "interruptable" validator.
   */
  @Slf4j
  private static class TestValidator implements Validator {

    @Override
    public String getName() {
      return "Test Validator";
    }

    @SneakyThrows
    @Override
    synchronized public void validate(ValidationContext context) {
      log.info("Executing '{}'...", context.getProjectKey());

      // Wait forever with monitor, but allow interruption
      wait();
    }

  }

}
