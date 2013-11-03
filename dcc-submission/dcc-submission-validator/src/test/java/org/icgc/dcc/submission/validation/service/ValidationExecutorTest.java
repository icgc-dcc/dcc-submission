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
package org.icgc.dcc.submission.validation.service;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

public class ValidationExecutorTest {

  static final int MAX_VALIDATING = 3;

  ValidationExecutor executor;

  @Before
  public void setUp() {
    executor = new ValidationExecutor(MAX_VALIDATING);
  }

  @After
  public void tearDown() {
    executor.stop();
  }

  @Test
  public void testExecute() {
    for (int i = 1; i <= MAX_VALIDATING; i++) {
      val projectKey = "project" + i;
      val validation = createValidation(projectKey);

      executor.execute(validation);
    }
    sleepUninterruptibly(1, SECONDS);

    val cancelled = executor.cancel("project1");
    assertThat(cancelled).isTrue();
    sleepUninterruptibly(1, SECONDS);
  }

  @Test(expected = ValidationRejectedException.class)
  public void testRejected() {
    int oneMoreThanMax = MAX_VALIDATING + 1;
    for (int i = 1; i <= oneMoreThanMax; i++) {
      val projectKey = "project" + i;
      val validation = createValidation(projectKey);

      executor.execute(validation);
    }
  }

  private static Validation createValidation(String projectKey) {
    val context = createValidationContext(projectKey);
    val validator = new TestValidator();
    val validators = Lists.<Validator> newArrayList(validator);

    return new Validation(context, validators);
  }

  private static ValidationContext createValidationContext(String projectKey) {
    ValidationContext context = mock(ValidationContext.class);
    when(context.getProjectKey()).thenReturn(projectKey);

    return context;
  }

  @Slf4j
  private static class TestValidator implements Validator {

    @SneakyThrows
    @Override
    public void validate(ValidationContext context) {
      log.info("Executing '{}'...", context.getProjectKey());
      for (;;) {
        if (Thread.currentThread().isInterrupted()) {
          throw new InterruptedException("Task '" + context.getProjectKey() + "' was interrupted");
        }
      }
    }

  }

}
