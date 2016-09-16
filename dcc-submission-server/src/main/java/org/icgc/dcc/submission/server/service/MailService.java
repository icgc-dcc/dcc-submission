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

import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.icgc.dcc.submission.core.model.Feedback;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.core.state.State;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MailService {

  /**
   * Server property names.
   */
  public static final String MAIL_SMTP_HOST = "mail.smtp.host";
  public static final String MAIL_SMTP_PORT = "mail.smtp.port";
  public static final String MAIL_SMTP_TIMEOUT = "mail.smtp.timeout";
  public static final String MAIL_SMTP_CONNECTION_TIMEOUT = "mail.smtp.connectiontimeout";

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
  private final SubmissionProperties properties;

  public void sendSupportFeedback(@NonNull Feedback feedback) {
    sendNotification(format("Feedback from %s - '%s'", feedback.getEmail(), feedback.getSubject()),
        feedback.getMessage());

    send(
        feedback.getEmail(),
        properties.getMail().getSupportEmail(),
        feedback.getSubject(),
        feedback.getMessage());
  }

  public void sendSupportProblem(@NonNull String subject, @NonNull String message) {
    sendNotification(format("Support issue - '%s'", subject));

    send(
        properties.getMail().getFromEmail(),
        properties.getMail().getSupportEmail(),
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

  public void sendSignoff(@NonNull String user, @NonNull Iterable<String> projectKeys,
      @NonNull String nextReleaseName) {
    sendNotification(
        format("Signed off Projects: %s", projectKeys),
        template(properties.getMail().getSignoffBody(), user, projectKeys, nextReleaseName));
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
      message.setFrom(address(properties.getMail().getFromEmail()));
      message.addRecipients(TO, addresses(emails));
      message.setSubject(template(properties.getMail().getSubject(), projectKey, state, report));
      message.setText(getResult(releaseName, projectKey, state));

      send(message);
    } catch (Exception e) {
      log.error("An error occured while emailing: ", e);
    }
  }

  public Boolean isEnabled() {
    return properties.getMail().getEnabled();
  }

  private String getResult(String releaseName, String projectKey, State state) {
    val mail = properties.getMail();
    // @formatter:off
    return
      state == ERROR         ? template(mail.getErrorBody(),        projectKey, state)                         : 
      state == INVALID       ? template(mail.getInvalidBody(),      projectKey, state, releaseName, projectKey) :
      state == NOT_VALIDATED ? template(mail.getNotValidatedBody(), projectKey, state, releaseName, projectKey) :
      state == VALID         ? template(mail.getValidBody(),        projectKey, state, releaseName, projectKey) :
                               format("Unexpected validation state '%s' prevented loading email text.", state);
    // @formatter:on
  }

  private void sendNotification(String subject, String message) {
    send(
        properties.getMail().getFromEmail(),
        properties.getMail().getNotificationEmail(),
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
          log.error("Error sending email '{}' to {}", message.getSubject(),
              Arrays.toString(message.getAllRecipients()));
          log.error("Exception:", t);
        }
      }
    });

  }

  private Message message() {
    val props = new Properties();
    props.put(MAIL_SMTP_HOST, properties.getMail().getSmtpHost());
    props.put(MAIL_SMTP_PORT, properties.getMail().getSmtpPort());
    props.put(MAIL_SMTP_TIMEOUT, properties.getMail().getSmtpTimeout());
    props.put(MAIL_SMTP_CONNECTION_TIMEOUT, properties.getMail().getSmtpConnectionTimeout());

    return new MimeMessage(Session.getDefaultInstance(props, null));
  }

  private String formatSubject(String text) {
    return format("[%s] %s", getHostName(), text);
  }

  private String template(String body, Object... arguments) {
    return format(body, arguments);
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
