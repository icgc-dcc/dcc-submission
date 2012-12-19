/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.validation.service;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.QueuedProject;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.icgc.dcc.validation.FatalPlanningException;
import org.icgc.dcc.validation.Plan;
import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.report.Outcome;
import org.icgc.dcc.validation.report.SchemaReport;
import org.icgc.dcc.validation.report.SubmissionReport;
import org.icgc.dcc.validation.report.ValidationErrorReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Inject;
import com.typesafe.config.Config;

/**
 * Manages validation queue that:<br>
 * - launches validation for queue submissions<br>
 * - updates submission states upon termination of the validation process
 */
public class ValidationQueueManagerService extends AbstractService {

  private static final Logger log = LoggerFactory.getLogger(ValidationQueueManagerService.class);

  private static final int POLLING_FREQUENCY_PER_SEC = 1;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  private final ReleaseService releaseService;

  private final ValidationService validationService;

  private final Config config;

  private ScheduledFuture<?> schedule;

  @Inject
  public ValidationQueueManagerService(final ReleaseService releaseService, ValidationService validationService,
      Config config) {

    checkArgument(releaseService != null);
    checkArgument(validationService != null);
    checkArgument(config != null);

    this.config = config;
    this.releaseService = releaseService;
    this.validationService = validationService;
  }

  @Override
  protected void doStart() {
    notifyStarted();
    startScheduler();
  }

  @Override
  protected void doStop() {
    stopScheduler();
    notifyStopped();
  }

  private void startScheduler() {
    log.info("polling queue every {} second", POLLING_FREQUENCY_PER_SEC);

    schedule = scheduler.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        Optional<QueuedProject> nextProject = Optional.<QueuedProject> absent();
        Optional<Throwable> criticalThrowable = Optional.<Throwable> absent();
        try {
          if(isRunning() && releaseService.hasNextRelease()) {
            nextProject = releaseService.getNextRelease().getNextInQueue();
            if(nextProject.isPresent()) {
              QueuedProject queuedProject = nextProject.get();
              log.info("next in queue {}", queuedProject);
              validateSubmission(queuedProject);
            }
          }
        } catch(FatalPlanningException e) { // potentially thrown by validateSubmission() upon file-level errors
          try {
            handleAbortedValidation(e);
          } catch(Throwable t) {
            criticalThrowable = Optional.fromNullable(t);
          }
        } catch(Throwable t) { // exception thrown within the run method are not logged otherwise (NullPointerException
                               // for instance)
          criticalThrowable = Optional.fromNullable(t);
        } finally {

          /*
           * When a scheduled job throws an exception to the executor, all future runs of the job are cancelled. Thus,
           * we should never throw an exception to our executor otherwise a server restart is necessary.
           */
          if(criticalThrowable.isPresent()) {
            Throwable t = criticalThrowable.get();
            log.error("a critical error occured while processing the validation queue", t);

            if(nextProject != null && nextProject.isPresent()) {
              QueuedProject project = nextProject.get();
              try {
                dequeue(project, SubmissionState.ERROR);
              } catch(Throwable t2) {
                log.error("a critical error occured while attempting to dequeue project {}", project.getKey());
              }
            } else {
              log.error("next project in queue not present, could not dequeue nor set submission state to {}",
                  SubmissionState.ERROR);
            }
          }
        }
      }

      /**
       * May throw unchecked FatalPlanningException upon file-level errors (too critical to continue)
       */
      private void validateSubmission(final QueuedProject queuedProject) throws FatalPlanningException {
        Release release = releaseService.getNextRelease().getRelease();
        if(release == null) {
          throw new ValidationServiceException("cannot access the next release");
        } else {
          if(release.getState() == ReleaseState.OPENED) {
            Plan plan = validationService.validate(release, queuedProject);
            handleCascadeStatus(plan, queuedProject);
          } else {
            log.info("Release was closed during validation; states not changed");
          }
        }
      }

      private void handleCascadeStatus(final Plan plan, final QueuedProject project) {
        if(plan.getCascade().getCascadeStats().isSuccessful()) {
          handleCompletedValidation(project, plan);
        } else {
          handleUnexpectedException(project);
        }
      }

    }, POLLING_FREQUENCY_PER_SEC, POLLING_FREQUENCY_PER_SEC, TimeUnit.SECONDS);
  }

  private void stopScheduler() {
    try {
      boolean cancel = schedule.cancel(true);
      log.info("attempt to cancel returned {}", cancel);
    } finally {
      scheduler.shutdown();
    }
  }

  public void handleAbortedValidation(FatalPlanningException e) {
    QueuedProject project = e.getProject();
    Plan plan = e.getPlan();
    if(plan.hasFileLevelErrors() == false) {
      throw new AssertionError(); // by design since this should be the condition for throwing the
      // FatalPlanningException
    }

    Map<String, TupleState> fileLevelErrors = plan.getFileLevelErrors();
    log.error("file errors (fatal planning errors):\n\t{}", fileLevelErrors);

    log.info("about to dequeue project key {}", project.getKey());
    dequeue(project, SubmissionState.INVALID);

    checkArgument(project != null);
    SubmissionReport report = new SubmissionReport();

    List<SchemaReport> schemaReports = new ArrayList<SchemaReport>();
    for(String schema : fileLevelErrors.keySet()) {
      SchemaReport schemaReport = new SchemaReport();
      Iterator<TupleState.TupleError> es = fileLevelErrors.get(schema).getErrors().iterator();
      List<ValidationErrorReport> errReport = Lists.newArrayList();
      while(es.hasNext()) {
        errReport.add(new ValidationErrorReport(es.next()));
      }
      schemaReport.addErrors(errReport);
      schemaReport.setName(schema);
      schemaReports.add(schemaReport);
    }
    report.setSchemaReports(schemaReports);
    setSubmissionReport(project.getKey(), report);
  }

  public void handleCompletedValidation(QueuedProject project, Plan plan) {
    checkArgument(project != null);

    SubmissionReport report = new SubmissionReport();
    Outcome outcome = plan.collect(report);

    log.info("completed validation - about to dequeue project key {}/set submission its state", project.getKey());
    if(outcome == Outcome.PASSED) {
      dequeue(project, SubmissionState.VALID);
    } else {
      dequeue(project, SubmissionState.INVALID);
    }
    setSubmissionReport(project.getKey(), report);
  }

  private void setSubmissionReport(String projectKey, SubmissionReport report) {
    log.info("starting report collecting on project {}", projectKey);

    Release release = releaseService.getNextRelease().getRelease();

    Submission submission = this.releaseService.getSubmission(release.getName(), projectKey);

    submission.setReport(report);

    // persist the report to DB
    this.releaseService.updateSubmissionReport(release.getName(), projectKey, submission.getReport());
    log.info("report collecting finished on project {}", projectKey);
  }

  public void handleUnexpectedException(QueuedProject project) {
    checkArgument(project != null);
    log.info("failed validation from unknown error - about to dequeue project key {}", project.getKey());
    dequeue(project, SubmissionState.ERROR);
  }

  private void dequeue(QueuedProject project, SubmissionState state) {
    if(project.getEmails().isEmpty() == false) {
      this.email(project, state);
    }

    Optional<QueuedProject> dequeuedProject = releaseService.dequeue(project.getKey(), state);
    if(dequeuedProject.isPresent() == false) {
      log.warn("could not dequeue project {}, maybe the queue was emptied in the meantime?", project.getKey());
    }
  }

  private void email(QueuedProject project, SubmissionState state) {
    Properties props = new Properties();
    props.put("mail.smtp.host", this.config.getString("mail.smtp.host"));
    Session session = Session.getDefaultInstance(props, null);
    Release release = releaseService.getNextRelease().getRelease();

    Set<Address> aCheck = Sets.newHashSet();

    for(String email : project.getEmails()) {
      try {
        Address a = new InternetAddress(email);
        aCheck.add(a);
      } catch(AddressException e) {
        log.error("Illigal Address: " + e);
      }
    }

    if(aCheck.isEmpty() == false) {
      try {
        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(this.config.getString("mail.from.email"), this.config
            .getString("mail.from.email")));

        msg.setSubject(String.format(this.config.getString("mail.subject"), project.getKey(), state));
        if(state == SubmissionState.ERROR) {
          // send email to admin when Error occurs
          Address adminEmailAdd = new InternetAddress(this.config.getString("mail.admin.email"));
          aCheck.add(adminEmailAdd);
          msg.setText(String.format(this.config.getString("mail.error_body"), project.getKey(), state));
        } else if(state == SubmissionState.VALID) {
          msg.setText(String.format(this.config.getString("mail.valid_body"), project.getKey(), state,
              release.getName(), project.getKey()));
        } else if(state == SubmissionState.INVALID) {
          msg.setText(String.format(this.config.getString("mail.invalid_body"), project.getKey(), state,
              release.getName(), project.getKey()));
        }

        Address[] addresses = new Address[aCheck.size()];

        int i = 0;
        for(Address email : aCheck) {
          addresses[i++] = email;
        }
        msg.addRecipients(Message.RecipientType.TO, addresses);

        Transport.send(msg);
        log.error("Emails for {} sent to {}: ", project.getKey(), aCheck);

      } catch(AddressException e) {
        log.error("an error occured while emailing: ", e);
      } catch(MessagingException e) {
        log.error("an error occured while emailing: ", e);
      } catch(UnsupportedEncodingException e) {
        log.error("an error occured while emailing: ", e);
      }
    }
  }

}
