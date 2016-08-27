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
package org.icgc.dcc.submission.server.service;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.icgc.dcc.submission.core.model.Outcome.ABORTED;
import static org.icgc.dcc.submission.core.model.Outcome.CANCELLED;
import static org.icgc.dcc.submission.core.model.Outcome.COMPLETED;
import static org.icgc.dcc.submission.core.model.Outcome.FAILED;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.icgc.dcc.submission.core.model.Outcome;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.SubmissionFileSystem;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.server.repository.CodeListRepository;
import org.icgc.dcc.submission.server.service.MailService;
import org.icgc.dcc.submission.server.service.ReleaseService;
import org.icgc.dcc.submission.server.service.ValidationService;
import org.icgc.dcc.submission.validation.ValidationExecutor;
import org.icgc.dcc.submission.validation.ValidationListener;
import org.icgc.dcc.submission.validation.core.Validation;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategyFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import lombok.SneakyThrows;
import lombok.val;

@RunWith(MockitoJUnitRunner.class)
public class ValidationServiceTest {

  /**
   * Test data.
   */
  final QueuedProject queuedProject = new QueuedProject("project", ImmutableList.<String> of("user@project.com"));

  /**
   * Class under test.
   */
  @InjectMocks
  ValidationService service;

  /**
   * Primary collaborators.
   */
  @Mock
  ReleaseService releaseService;
  @Mock
  CodeListRepository codeListRepository;
  @Mock
  ValidationExecutor executor;
  @Mock
  MailService mailService;
  @Mock
  SubmissionFileSystem submissionFileSystem;
  @Mock
  SubmissionPlatformStrategyFactory platformStrategyFactory;
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
    when(submission.getReport()).thenReturn(new Report());
    when(release.nextInQueue()).thenReturn(Optional.fromNullable(queuedProject));
    when(release.getState()).thenReturn(OPENED);
    when(release.getSubmission(anyString())).thenReturn(Optional.of(submission));

    when(context.getReport()).thenReturn(new Report());

    when(releaseService.getNextRelease()).thenReturn(release);
    when(releaseService.countOpenReleases()).thenReturn(1L);
    when(releaseService.getNextDictionary()).thenReturn(dictionary);
    when(codeListRepository.findCodeLists()).thenReturn(Collections.<CodeList> emptyList());
  }

  @Test
  @SneakyThrows
  public void test_pollValidation_completed() {
    // Setup: Add a no-op validator
    validators.add(validator);

    // Setup: Create returned future with context that has no errors
    val validation = createValidation(context, validator);
    validation.execute();

    // Setup: When submitted we will call the onSuccess callback
    mockExecutorCallback(onCompletion(validation));

    // Exercise
    service.pollValidation();

    // Verify: Ensure "aborted"
    verifyOutcome(COMPLETED);
  }

  @Test
  @SneakyThrows
  public void test_pollValidation_aborted() {
    // Setup: Add a no-op validator
    validators.add(validator);

    // Setup: Create returned future with context that has no errors
    val validation = createValidation(context, validator);

    // Setup: When submitted we will call the onSuccess callback
    mockExecutorCallback(onCompletion(validation));

    // Exercise
    service.pollValidation();

    // Verify: Ensure "aborted"
    verifyOutcome(ABORTED);
  }

  @Test
  @SneakyThrows
  public void test_pollValidation_cancelled() {
    // Setup: Add a no-op validator
    validators.add(validator);

    // Setup: Create returned future with context that has no errors
    val validation = createValidation(context, validator);

    // Setup: When submitted we will call the onFailure callback with a cancellation exception
    mockExecutorCallback(onCancelled(validation));

    // Exercise
    service.pollValidation();

    // Verify: Ensure "cancelled"
    verifyOutcome(CANCELLED);
  }

  @Test
  @SneakyThrows
  public void test_pollValidation_failed() {
    // Setup: Add a no-op validator
    validators.add(validator);

    // Setup: Create returned future with context that has no errors
    val validation = createValidation(context, validator);

    // Setup: When submitted we will call the onFailure callback with a runtime exception
    mockExecutorCallback(onFailure(validation, new RuntimeException()));

    // Exercise
    service.pollValidation();

    // Verify: Ensure "failed"
    verifyOutcome(FAILED);
  }

  private void mockExecutorCallback(Answer<Object> answer) {
    doAnswer(answer).when(executor).execute(
        any(Validation.class),
        any(ValidationListener.class));
  }

  private void verifyOutcome(Outcome outcome) {
    verify(releaseService).resolveSubmission(queuedProject, outcome, context.getReport());
  }

  private static Answer<Object> onCompletion(final Validation validation) {
    return invocation -> {
      ValidationListener listener = (ValidationListener) invocation.getArguments()[1];
      listener.onEnded(validation);

      return null;
    };
  }

  private static Answer<Object> onCancelled(final Validation validation) {
    return invocation -> {
      ValidationListener listener = (ValidationListener) invocation.getArguments()[1];
      listener.onCancelled(validation);

      return null;
    };
  }

  private static Answer<Object> onFailure(final Validation validation, final Throwable throwable) {
    return invocation -> {
      ValidationListener listener = (ValidationListener) invocation.getArguments()[1];
      listener.onFailure(validation, throwable);

      return null;
    };
  }

  private static Validation createValidation(ValidationContext context, Validator validator) {
    return new Validation(context, newArrayList(validator));
  }

}
