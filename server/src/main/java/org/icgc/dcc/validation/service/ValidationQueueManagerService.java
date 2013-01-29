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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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

import org.icgc.dcc.release.NextRelease;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.QueuedProject;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.icgc.dcc.validation.FilePresenceException;
import org.icgc.dcc.validation.Plan;
import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.report.Outcome;
import org.icgc.dcc.validation.report.SchemaReport;
import org.icgc.dcc.validation.report.SubmissionReport;
import org.icgc.dcc.validation.report.ValidationErrorReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeListener;

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

  private static final int DEFAULT_MAX_VALIDATING = 1;

  private final int MAX_VALIDATING;

  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

  private final ReleaseService releaseService;

  private final ValidationService validationService;

  private final Config config;

  private ScheduledFuture<?> schedule;

  /**
   * Mutex for synchronizing parallel validation.
   */
  private final Object mutex = new Object();

  /**
   * To keep track of parallel validations.
   */
  private volatile int synchronizedCount = 0;

  @Inject
  public ValidationQueueManagerService(final ReleaseService releaseService, ValidationService validationService,
      Config config) {

    checkArgument(releaseService != null);
    checkArgument(validationService != null);
    checkArgument(config != null);

    this.config = config;
    this.releaseService = releaseService;
    this.validationService = validationService;
    this.MAX_VALIDATING =
        config.hasPath("validator.max_simultaneous") ? config.getInt("validator.max_simultaneous") : DEFAULT_MAX_VALIDATING;
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
        log.debug("polling");

        Optional<QueuedProject> optionalNextProject = Optional.<QueuedProject> absent();
        Optional<Throwable> criticalThrowable = Optional.<Throwable> absent();
        try {
          if(isRunning() && releaseService.hasNextRelease()) {
            optionalNextProject = processNext();
          }
        } catch(FilePresenceException e) {
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
            processCriticalThrowable(criticalThrowable.get(), optionalNextProject);
          }
        }

        log.debug("polled");
      }

      /**
       * Handles the next item in the queue if there are any.
       * <p>
       * Note that a checked exception {@code FilePresenceException} may be thrown and that section 14.19 of the JLS
       * guarantees that the mutex will be released in that case as well.
       * @throws FilePresenceException
       */
      private Optional<QueuedProject> processNext() throws FilePresenceException {
        Optional<QueuedProject> optionalNextProject = Optional.absent();

        NextRelease nextRelease = releaseService.getNextRelease();
        Release release = nextRelease.getRelease();
        if(release == null) {
          throw new ValidationServiceException("cannot access the next release");
        } else if(release.getState() != ReleaseState.OPENED) {
          log.info("Release was closed during validation; states not changed");
        } else {
          optionalNextProject = release.nextInQueue();
          if(optionalNextProject.isPresent()) {
            QueuedProject nextProject = optionalNextProject.get();
            String projectKey = nextProject.getKey();
            log.info("next in queue {}", projectKey);

            synchronized(mutex) {
              if(synchronizedCount < MAX_VALIDATING) { // critical
                synchronizedCount++; // critical
                checkState(synchronizedCount <= MAX_VALIDATING); // by design
              }
            }

            releaseService.dequeueToValidating(nextProject);
            Plan plan = validationService.prepareValidation(release, nextProject, new ValidationCascadeListener());
            validationService.startValidation(plan); // non-blocking
          }
        }
        return optionalNextProject;
      }

      private void processCriticalThrowable(Throwable t, Optional<QueuedProject> optionalNextProject) {
        log.error("a critical error occured while processing the validation queue", t);

        if(optionalNextProject != null && optionalNextProject.isPresent()) {
          QueuedProject project = optionalNextProject.get();
          try {
            resolve(project, SubmissionState.ERROR);
          } catch(Throwable t2) {
            log.error("a critical error occured while attempting to dequeue project {}", project.getKey());
          }
        } else {
          log.error("next project in queue not present, could not dequeue nor set submission state to {}",
              SubmissionState.ERROR);
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

  /**
   * Triggered by our application.
   */
  private void handleAbortedValidation(FilePresenceException e) {
    Plan plan = e.getPlan();
    if(plan.hasFileLevelErrors() == false) {
      throw new AssertionError(); // by design since this should be the condition for throwing the
      // FatalPlanningException
    }

    Map<String, TupleState> fileLevelErrors = plan.getFileLevelErrors();
    log.error("file errors (fatal planning errors):\n\t{}", fileLevelErrors);

    QueuedProject queuedProject = plan.getQueuedProject();
    checkArgument(queuedProject != null);
    String projectKey = queuedProject.getKey();
    log.info("about to dequeue project key {}", projectKey);
    resolve(queuedProject, SubmissionState.INVALID);

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
    setSubmissionReport(projectKey, report);
  }

  /**
   * Triggered by cascading (via listener on cascade).
   */
  public void handleCompletedValidation(Plan plan) {
    checkArgument(plan != null);

    QueuedProject queuedProject = plan.getQueuedProject();
    checkNotNull(queuedProject);
    String projectKey = queuedProject.getKey();
    log.info("Cascade finished normally for project {}", projectKey);

    log.info("Gathering report for project {}", projectKey);
    SubmissionReport report = new SubmissionReport();
    Outcome outcome = plan.collect(report);
    log.info("Gathered report for project {}", projectKey);

    // resolving submission
    if(outcome == Outcome.PASSED) {
      resolve(queuedProject, SubmissionState.VALID);
    } else {
      resolve(queuedProject, SubmissionState.INVALID);
    }
    setSubmissionReport(projectKey, report);

    log.info("Validation finished normally for project {}, time spent on validation is {} seconds", projectKey,
        plan.getDuration() / 1000.0);
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

  /**
   * Triggered by cascading (via listener on cascade).
   */
  public void handleUnexpectedException(QueuedProject project) {
    checkArgument(project != null);
    log.info("failed validation from unknown error - about to dequeue project key {}", project.getKey());
    resolve(project, SubmissionState.ERROR);
  }

  /**
   * Resolution can happen in 4 manners:<br/>
   * - 1. cascading listener's onComplete(): VALID or INVALID<br/>
   * - 2. catching of {@code FatalPlanningException} (typically a file-level error - see TODO about it): INVALID<br/>
   * - 3. cascading listener's onStopping(): ERROR<br/>
   * - 4. catching of an unknown exception: ERROR<br/>
   */
  private void resolve(QueuedProject project, SubmissionState state) {
    String key = project.getKey();
    log.info("resolving {}", key);
    releaseService.resolve(key, state);

    synchronized(mutex) {
      checkState(synchronizedCount > 0); // by design
      synchronizedCount--;
    }

    if(project.getEmails().isEmpty() == false) {
      this.email(project, state);
    }
    log.info("resolved {}", key);
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
        String fromEmail = this.config.getString("mail.from.email");
        msg.setFrom(new InternetAddress(fromEmail, fromEmail));

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

  /**
   * TODO: externalize? may be difficult because of it calls method from the enclosing class...
   */
  public class ValidationCascadeListener implements CascadeListener {
    Plan plan;

    public void setPlan(Plan plan) {
      this.plan = plan;
    }

    @Override
    public void onStarting(Cascade cascade) {
      // No-op for now, can add in external hook
      log.debug("CascadeListener onStarting");
    }

    @Override
    public void onStopping(Cascade cascade) {
      log.debug("CascadeListener onStopping");
      handleUnexpectedException(plan.getQueuedProject());
    }

    @Override
    public void onCompleted(Cascade cascade) {
      log.debug("CascadeListener onCompleted");
      handleCompletedValidation(plan);
    }

    @Override
    public boolean onThrowable(Cascade cascade, Throwable throwable) {
      log.debug("CascadeListener onThrowable");
      // No-op for now; false indicates that the throwable was not handled and needs to be re-thrown
      return false;
    }
  }
}
