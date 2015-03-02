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
package org.icgc.dcc.submission.service;

import static java.lang.String.format;
import static javax.mail.Message.RecipientType.TO;
import static org.icgc.dcc.submission.release.model.SubmissionState.ERROR;
import static org.icgc.dcc.submission.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.NOT_VALIDATED;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALID;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.model.Feedback;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.core.state.State;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.typesafe.config.Config;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class MailService {

  /**
   * Server property names.
   */
  public static final String MAIL_ENABLED = "mail.enabled";
  public static final String MAIL_SMTP_HOST = "mail.smtp.host";
  public static final String MAIL_SMTP_PORT = "mail.smtp.port";
  public static final String MAIL_SMTP_TIMEOUT = "mail.smtp.timeout";
  public static final String MAIL_SMTP_CONNECTION_TIMEOUT = "mail.smtp.connectiontimeout";

  /**
   * Subject property name.
   */
  public static final String MAIL_VALIDATION_SUBJECT = "mail.subject";

  /**
   * From property names.
   */
  public static final String MAIL_FROM = "mail.from.email";

  /**
   * Recipient property names.
   */
  public static final String MAIL_SUPPORT_RECIPIENT = "mail.support.email";
  public static final String MAIL_NOTIFICATION_RECIPIENT = "mail.notification.email";

  /**
   * Body property names.
   */
  public static final String MAIL_ERROR_BODY = "mail.error_body";
  public static final String MAIL_NOT_VALIDATED_BODY = "mail.not_validated_body";
  public static final String MAIL_INVALID_BODY = "mail.invalid_body";
  public static final String MAIL_VALID_BODY = "mail.valid_body";
  public static final String MAIL_SIGNOFF_BODY = "mail.signoff_body";

  /**
   * Prefix used in the subject of a notification email.
   */
  public static final String NOTIFICATION_SUBJECT_PREFEX = "Notification: ";

  /**
   * Executor used in sending emails asynchronously.
   */
  private final Executor executor = Executors.newCachedThreadPool(
      new ThreadFactoryBuilder()
          .setNameFormat("mail-%s") // For logging
          .setPriority(Thread.MIN_PRIORITY) // For niceness
          .build());

  /**
   * Application config.
   */
  @NonNull
  private final Config config;

  public void sendSupportFeedback(@NonNull Feedback feedback) {
    sendNotification(format("Feedback from %s - '%s'", feedback.getEmail(), feedback.getSubject()),
        feedback.getMessage());

    send(
        feedback.getEmail(),
        to(MAIL_SUPPORT_RECIPIENT),
        feedback.getSubject(),
        feedback.getMessage());
  }

  public void sendSupportProblem(@NonNull String subject, @NonNull String message) {
    sendNotification(format("Support issue - '%s'", subject));

    send(
        from(MAIL_FROM),
        to(MAIL_SUPPORT_RECIPIENT),
        subject,
        message);
  }

  public void sendFileTransferred(@NonNull String user, @NonNull String path) {
    sendNotification(format("User '%s' finished transferring file '%s'",
        user, path));
  }

  public void sendFileRemoved(@NonNull String user, @NonNull String path) {
    sendNotification(format("User '%s' removed file '%s'",
        user, path));
  }

  public void sendFileRenamed(@NonNull String user, @NonNull String path, @NonNull String newPath) {
    sendNotification(format("User '%s' renamed file '%s' to '%s'",
        user, path, newPath));
  }

  public void sendSignoff(@NonNull String user, @NonNull Iterable<String> projectKeys, @NonNull String nextReleaseName) {
    sendNotification(
        format("Signed off Projects: %s", projectKeys),
        template(MAIL_SIGNOFF_BODY, user, projectKeys, nextReleaseName));
  }

  public void sendValidationStarted(@NonNull String releaseName, @NonNull String projectKey,
      @NonNull List<String> emails) {
    sendNotification(format("Validation started for release '%s' project '%s' (on behalf of '%s')",
        releaseName, projectKey, emails));
  }

  public void sendValidationFinsished(@NonNull String releaseName, @NonNull String projectKey,
      @NonNull List<String> emails, @NonNull State state, @NonNull Report report) {
    sendNotification(format(
        "Validation finished for release '%s' project '%s' (on behalf of '%s') with state '%s'",
        releaseName, projectKey, emails, state));
  }

  public void sendValidationResult(@NonNull String releaseName, @NonNull String projectKey,
      @NonNull List<String> emails, @NonNull State state, @NonNull Report report) {
    if (!isEnabled()) {
      log.info("Mail not enabled. Skipping...");
      return;
    }

    sendValidationFinsished(releaseName, projectKey, emails, state, report);

    try {
      val message = message();
      message.setFrom(address(get(MAIL_FROM)));
      message.addRecipients(TO, addresses(emails));
      message.setSubject(template(MAIL_VALIDATION_SUBJECT, projectKey, state, report));
      message.setText(getResult(releaseName, projectKey, state));

      send(message);
    } catch (Exception e) {
      log.error("An error occured while emailing: ", e);
    }
  }

  private String getResult(String releaseName, String projectKey, State state) {
    // @formatter:off
    return
      state == ERROR         ? template(MAIL_ERROR_BODY,           projectKey, state)                         : 
      state == INVALID       ? template(MAIL_INVALID_BODY,         projectKey, state, releaseName, projectKey) :
      state == NOT_VALIDATED ? template(MAIL_NOT_VALIDATED_BODY,   projectKey, state, releaseName, projectKey) :
      state == VALID         ? template(MAIL_VALID_BODY,           projectKey, state, releaseName, projectKey) :
                               format("Unexpected validation state '%s' prevented loading email text.", state);
    // @formatter:on
  }

  private void sendNotification(String subject, String message) {
    send(
        from(MAIL_FROM),
        to(MAIL_NOTIFICATION_RECIPIENT),
        NOTIFICATION_SUBJECT_PREFEX + subject,
        message);
  }

  private void sendNotification(String subject) {
    sendNotification(subject, subject);
  }

  private void send(final String from, final String recipient, final String subject, final String text) {
    if (!isEnabled()) {
      log.info("Mail not enabled. Skipping...");
      return;
    }

    try {
      val message = message();
      message.setFrom(address(from));
      message.addRecipient(TO, address(recipient));
      message.setSubject(formatSubject(subject));
      message.setText(text);

      send(message);
    } catch (Exception e) {
      log.error("An error occured while emailing: ", e);
    }
  }

  /**
   * Sends the supplied {@code message} asynchronously using JavaMail.
   * 
   * @param message the message to send
   */
  private void send(final Message message) {
    executor.execute(new Runnable() {

      @Override
      @SneakyThrows
      public void run() {
        try {
          log.info("Sending email '{}' to {}...", message.getSubject(), Arrays.toString(message.getAllRecipients()));
          Transport.send(message);
          log.info("Sent email '{}' to {}", message.getSubject(), Arrays.toString(message.getAllRecipients()));
        } catch (Throwable t) {
          log.error("Error sending email '{}' to {}", message.getSubject(), Arrays.toString(message.getAllRecipients()));
          log.error("Exception:", t);
        }
      }
    });

  }

  private Message message() {
    val props = new Properties();
    props.put(MAIL_SMTP_HOST, get(MAIL_SMTP_HOST));
    props.put(MAIL_SMTP_PORT, get(MAIL_SMTP_PORT, "25"));
    props.put(MAIL_SMTP_TIMEOUT, get(MAIL_SMTP_TIMEOUT, "5000"));
    props.put(MAIL_SMTP_TIMEOUT, get(MAIL_SMTP_CONNECTION_TIMEOUT, "5000"));

    return new MimeMessage(Session.getDefaultInstance(props, null));
  }

  private String formatSubject(String text) {
    return format("[%s] %s", getHostName(), text);
  }

  private String template(String templateName, Object... arguments) {
    return format(get(templateName), arguments);
  }

  private String from(String name) {
    return get(name);
  }

  private String to(String name) {
    return get(name);
  }

  private String get(String name) {
    return config.getString(name);
  }

  private String get(String name, String defaultValue) {
    return config.hasPath(name) ? config.getString(name) : defaultValue;
  }

  private boolean isEnabled() {
    return config.hasPath(MAIL_ENABLED) ? config.getBoolean(MAIL_ENABLED) : true;
  }

  private static Address[] addresses(List<String> emails) {
    val addresses = new Address[emails.size()];
    for (int i = 0; i < emails.size(); i++) {
      try {
        addresses[i] = address(emails.get(i));
      } catch (UnsupportedEncodingException e) {
        log.error("Illegal Address: " + e + " in " + emails);
      }
    }

    return addresses;
  }

  private static InternetAddress address(String email) throws UnsupportedEncodingException {
    return new InternetAddress(email, email);
  }

  private static String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      // Best effort
      return "unknown host";
    }
  }

}
