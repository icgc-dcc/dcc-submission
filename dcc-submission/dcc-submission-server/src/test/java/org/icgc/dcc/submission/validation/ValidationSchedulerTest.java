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

import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.elasticsearch.common.collect.Lists.newArrayList;
import static org.icgc.dcc.submission.release.model.ReleaseState.OPENED;
import static org.icgc.dcc.submission.release.model.SubmissionState.ERROR;
import static org.icgc.dcc.submission.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.NOT_VALIDATED;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALID;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.concurrent.CancellationException;

import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.release.NextRelease;
import org.icgc.dcc.submission.release.ReleaseService;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.validation.platform.PlatformStrategyFactory;
import org.icgc.dcc.submission.validation.service.Validation;
import org.icgc.dcc.submission.validation.service.ValidationContext;
import org.icgc.dcc.submission.validation.service.ValidationExecutor;
import org.icgc.dcc.submission.validation.service.Validator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

@RunWith(MockitoJUnitRunner.class)
public class ValidationSchedulerTest {

  /**
   * Test data.
   */
  final QueuedProject queuedProject = new QueuedProject("project", "user@project.com");

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
  NextRelease nextRelease;
  @Mock
  Release release;
  @Mock
  Dictionary dictionary;

  /**
   * Class under test.
   */
  @InjectMocks
  ValidationScheduler scheduler;

  @Before
  public void setUp() {
    // Establish an open release with a single queued project
    when(release.getState()).thenReturn(OPENED);
    when(release.nextInQueue()).thenReturn(Optional.fromNullable(queuedProject));
    when(nextRelease.getRelease()).thenReturn(release);
    when(releaseService.resolveNextRelease()).thenReturn(nextRelease);
    when(releaseService.countOpenReleases()).thenReturn(1L);
    when(releaseService.getNextDictionary()).thenReturn(dictionary);
  }

  @Test
  @SneakyThrows
  public void test_runOneIteration_success_valid() {
    // Add a no-op validator
    validators.add(validator);

    // Create returned future with context that has no errors
    when(context.hasErrors()).thenReturn(false);
    val future = createSetFuture(context, validator);

    // When submitted we will return the "valid" future
    when(executor.execute(any(Validation.class))).thenReturn(future);

    // Exercise
    scheduler.runOneIteration();

    // Ensure valid
    verify(releaseService).resolve(queuedProject.getKey(), VALID);
  }

  @Test
  @SneakyThrows
  public void test_runOneIteration_success_invalid() {
    // Add a no-op validator
    validators.add(validator);

    // Create returned future with context that has errors
    when(context.hasErrors()).thenReturn(true);
    val future = createSetFuture(context, validator);

    // When submitted we will return the "invalid" future
    when(executor.execute(any(Validation.class))).thenReturn(future);

    // Exercise
    scheduler.runOneIteration();

    // Ensure valid
    verify(releaseService).resolve(queuedProject.getKey(), INVALID);
  }

  @Test
  @SneakyThrows
  public void test_runOneIteration_failure_cancel() {
    // Add a no-op validator
    validators.add(validator);

    // Create returned future with context that has errors
    val future = createExceptionFuture(new CancellationException());

    // When submitted we will return the "invalid" future
    when(executor.execute(any(Validation.class))).thenReturn(future);

    // Exercise
    scheduler.runOneIteration();

    // Ensure valid
    verify(releaseService).resolve(queuedProject.getKey(), NOT_VALIDATED);
  }

  @Test
  @SneakyThrows
  public void test_runOneIteration_failure_error() {
    // Add a no-op validator
    validators.add(validator);

    // Create returned future with context that has errors
    val future = createExceptionFuture(new RuntimeException());

    // When submitted we will return the "invalid" future
    when(executor.execute(any(Validation.class))).thenReturn(future);

    // Exercise
    scheduler.runOneIteration();

    // Ensure valid
    verify(releaseService).resolve(queuedProject.getKey(), ERROR);
  }

  private static ListenableFuture<Validation> createSetFuture(ValidationContext context, Validator validator) {
    val future = SettableFuture.<Validation> create();
    future.set(new Validation(context, newArrayList(validator)));

    return future;
  }

  private static ListenableFuture<Validation> createExceptionFuture(Throwable throwable) {
    val future = SettableFuture.<Validation> create();
    future.setException(throwable);

    return future;
  }

}
