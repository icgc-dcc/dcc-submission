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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.validation.MalformedSubmissionException;
import org.icgc.dcc.submission.validation.SubmissionSemanticsException;
import org.icgc.dcc.submission.validation.checker.FirstPassValidator;
import org.icgc.dcc.submission.validation.core.Plan;
import org.icgc.dcc.submission.validation.core.ValidationListener;
import org.icgc.dcc.submission.validation.planner.Planner;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.platform.PlatformStrategyFactory;
import org.icgc.dcc.submission.validation.primary.PrimaryValidator;
import org.icgc.dcc.submission.validation.report.SubmissionReportContext;
import org.icgc.dcc.submission.validation.semantic.ReferenceGenomeValidator;

import cascading.cascade.Cascade;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;

/**
 * Deprecated service that wraps validation call for the {@code ValidationQueueService}.
 * <p>
 * Use {@link PrimaryValidator} in the next refactoring.
 * */
@Slf4j
@RequiredArgsConstructor(onConstructor = @_(@Inject))
public class ValidationService {

  @NonNull
  private final Planner planner;
  @NonNull
  private final DccFileSystem dccFileSystem;
  @NonNull
  private final PlatformStrategyFactory platformStrategyFactory;

  public Plan prepareValidation(Release release, Dictionary dictionary, QueuedProject queuedProject,
      ValidationListener listener)
      throws MalformedSubmissionException, SubmissionSemanticsException {
    log.info("Preparing cascade for project '{}'", queuedProject.getKey());
    val projectKey = queuedProject.getKey();
    val releaseFilesystem = dccFileSystem.getReleaseFilesystem(release);
    val submissionDirectory = releaseFilesystem.getSubmissionDirectory(projectKey);

    ValidationContext context =
        new DefaultValidationContext(new SubmissionReportContext(), projectKey, queuedProject.getEmails(), release,
            dictionary, dccFileSystem, platformStrategyFactory);

    log.info("Checking validation for '{}'", projectKey);
    checkerValidation(context);

    log.info("Semantic validation for '{}'", projectKey);
    semanticValidation(context);

    Plan plan = planValidation(queuedProject, submissionDirectory, context.getPlatformStrategy(), dictionary, listener);
    listener.setPlan(plan);

    log.info("Prepared cascade for project {}", projectKey);
    return plan;
  }

  /**
   * Plans and connects the cascade running the validation.
   * <p>
   * Note that emptying of the .validation dir happens right before launching the cascade in {@link Plan#startCascade()}
   */
  @VisibleForTesting
  @SneakyThrows
  public Plan planValidation(QueuedProject queuedProject, SubmissionDirectory submissionDirectory,
      PlatformStrategy platformStategy, Dictionary dictionary, ValidationListener listener) {
    val projectKey = queuedProject.getKey();
    log.info("Planning cascade for project {}...", projectKey);
    Plan plan = planner.plan(queuedProject, submissionDirectory, platformStategy, dictionary);

    log.info("Planned cascade for project {}", projectKey);
    log.info(" # internal flows: {}", Iterables.size(plan.getInternalFlows()));
    log.info(" # external flows: {}", Iterables.size(plan.getExternalFlows()));

    log.info("Connecting cascade for project {}", projectKey);
    plan.connect(platformStategy);
    log.info("Connected cascade for project {}", projectKey);

    if (listener != null) {
      plan.addCascadeListener(listener);
    }

    return plan;
  }

  /**
   * Starts validation in a asynchronous manner.
   * <p>
   * {@code Plan} contains the {@code Cascade}.<br/>
   * This is a non-blocking call, completion is handled by
   * <code>{@link ValidationCascadeListener#onCompleted(Cascade)}</code>
   */
  public void startValidation(Plan plan) {
    log.info("Starting validation on project {}", plan.getProjectKey());
    plan.startCascade();
  }

  private void checkerValidation(ValidationContext context) throws MalformedSubmissionException {
    val validator = new FirstPassValidator(dccFileSystem, context.getDictionary(), context.getSubmissionDirectory());

    log.info("Starting well-formedness validation on project {}...", context.getProjectKey());
    validator.validate(context);
    if (context.hasErrors()) {
      throw new MalformedSubmissionException(
          new QueuedProject(context.getProjectKey(), context.getEmails()),
          context.getSubmissionReport());
    }
  }

  private void semanticValidation(ValidationContext context) throws SubmissionSemanticsException {
    val validator = new ReferenceGenomeValidator();

    log.info("Starting reference genome validation on project {}...", context.getProjectKey());
    validator.validate(context);
    if (context.hasErrors()) {
      throw new SubmissionSemanticsException(
          new QueuedProject(context.getProjectKey(), context.getEmails()),
          context.getSubmissionReport());
    }
  }

}
