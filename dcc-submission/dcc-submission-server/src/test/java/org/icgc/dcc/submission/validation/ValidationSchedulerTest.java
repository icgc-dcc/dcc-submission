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

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;
import static org.icgc.dcc.submission.validation.ValidationOutcome.CANCELLED;
import static org.icgc.dcc.submission.validation.ValidationOutcome.FAILED;
import static org.icgc.dcc.submission.validation.ValidationOutcome.SUCCEEDED;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CancellationException;

import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.release.model.DataTypeState;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.service.ReleaseService;
import org.icgc.dcc.submission.validation.core.SubmissionReport;
import org.icgc.dcc.submission.validation.core.Validation;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.platform.PlatformStrategyFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class ValidationSchedulerTest {

  /**
   * Test data.
   */
  final QueuedProject queuedProject = new QueuedProject("project", ImmutableList.<String> of("user@project.com"));

  /**
   * Class under test.
   */
  @InjectMocks
  ValidationScheduler scheduler;

  /**
   * Primary collaborators.
   */
  @Mock
  ReleaseService releaseService;
  @Mock
  ValidationExecutor executor;
  @Mock
  MailService mailService;
  @Mock
  DccFileSystem dccFileSystem;
  @Mock
  PlatformStrategyFactory platformStrategyFactory;
  @Spy
  Set<Validator> validators = newLinkedHashSet();

  /**
   * Secondary collaborators.
   */
  @Mock
  Validator validator;
  @Mock
  ValidationContext context;
  @Mock
  Release release;
  @Mock
  Submission submission;
  @Mock
  Dictionary dictionary;

  @Before
  public void setUp() {
    // Establish an open release with a single queued project
    when(release.getState()).thenReturn(OPENED);
    when(release.nextInQueue()).thenReturn(Optional.fromNullable(queuedProject));
    when(releaseService.getNextRelease()).thenReturn(release);
    when(releaseService.countOpenReleases()).thenReturn(1L);
    when(releaseService.getNextDictionary()).thenReturn(dictionary);
    when(releaseService.getSubmission(anyString(), anyString())).thenReturn(submission);
    when(releaseService.getSubmission(anyString())).thenReturn(submission);
    when(submission.getDataState()).thenReturn(Collections.<DataTypeState> emptyList());
    when(submission.getReport()).thenReturn(new SubmissionReport());
    when(context.getSubmissionReport()).thenReturn(new SubmissionReport());
  }

  @Test
  @SneakyThrows
  public void test_runOneIteration_succeeded() {
    // Setup: Add a no-op validator
    validators.add(validator);

    // Setup: Create returned future with context that has no errors
    val validation = createValidation(context, validator);

    // Setup: When submitted we will call the onSuccess callback
    mockExecutorCallback(onSuccess(validation));

    // Exercise
    scheduler.runOneIteration();

    // Verify: Ensure "succeeded"
    verifyOutcome(SUCCEEDED);
  }

  @Test
  @SneakyThrows
  public void test_runOneIteration_cancelled() {
    // Setup: Add a no-op validator
    validators.add(validator);

    // Setup: When submitted we will call the onFailure callback with a cancellation exception
    mockExecutorCallback(onFailure(new CancellationException()));

    // Exercise
    scheduler.runOneIteration();

    // Verify: Ensure "cancelled"
    verifyOutcome(CANCELLED);
  }

  @Test
  @SneakyThrows
  public void test_runOneIteration_failed() {
    // Setup: Add a no-op validator
    validators.add(validator);

    // Setup: When submitted we will call the onFailure callback with a runtime exception
    mockExecutorCallback(onFailure(new RuntimeException()));

    // Exercise
    scheduler.runOneIteration();

    // Verify: Ensure "failed"
    verifyOutcome(FAILED);
  }

  private void mockExecutorCallback(Answer<Object> answer) {
    doAnswer(answer).when(executor).execute(
        any(Validation.class),
        any(Runnable.class),
        any(FutureCallback.class));
  }

  private void verifyOutcome(ValidationOutcome outcome) {
    verify(releaseService).resolveSubmission(queuedProject, outcome, context.getSubmissionReport());
  }

  private static Answer<Object> onSuccess(final Validation validation) {
    return new Answer<Object>() {

      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        val completedCallback = (FutureCallback<Validation>) invocation.getArguments()[2];
        completedCallback.onSuccess(validation);

        return null;
      }

    };
  }

  private static Answer<Object> onFailure(final Throwable throwable) {
    return new Answer<Object>() {

      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        val completedCallback = (FutureCallback<Validation>) invocation.getArguments()[2];
        completedCallback.onFailure(throwable);

        return null;
      }

    };
  }

  private static Validation createValidation(ValidationContext context, Validator validator) {
    return new Validation(context, newArrayList(validator));
  }

}
