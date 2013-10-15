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
package org.icgc.dcc.submission.core;

import static java.lang.String.format;
import static javax.mail.Message.RecipientType.TO;
import static org.icgc.dcc.submission.release.model.SubmissionState.ERROR;
import static org.icgc.dcc.submission.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALID;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.model.Feedback;
import org.icgc.dcc.submission.release.model.SubmissionState;

import com.google.inject.Inject;
import com.typesafe.config.Config;

@Slf4j
public class MailService {

  /**
   * Server property names.
   */
  public static final String MAIL_SMTP_HOST = "mail.smtp.host";
  public static final String MAIL_SMTP_SERVER = "smtp.oicr.on.ca";

  /**
   * Subject property name.
   */
  public static final String MAIL_VALIDATION_SUBJECT = "mail.subject";

  /**
   * From property names.
   */
  public static final String MAIL_NORMAL_FROM = "mail.from.email";
  public static final String MAIL_PROBLEM_FROM = "mail.from.email";

  /**
   * Recipient property names.
   */
  public static final String MAIL_ADMIN_RECIPIENT = "mail.admin.email";
  public static final String MAIL_MANUAL_SUPPORT_RECIPIENT = "mail.manual_support.email";
  public static final String MAIL_AUTOMATIC_SUPPORT_RECIPIENT = "mail.automatic_support.email";

  /**
   * Body property names.
   */
  public static final String MAIL_SIGNOFF_BODY = "mail.signoff_body";
  public static final String MAIL_ERROR_BODY = "mail.error_body";
  public static final String MAIL_VALID_BODY = "mail.valid_body";
  public static final String MAIL_INVALID_BODY = "mail.invalid_body";

  /**
   * Application config.
   */
  private final Config config;

  @Inject
  public MailService(Config config) {
    this.config = config;
  }

  public void sendAdminProblem(String message) {
    send(
        config.getString(MAIL_PROBLEM_FROM),
        config.getString(MAIL_ADMIN_RECIPIENT),
        message,
        message);
  }

  public void sendSupportProblem(String subject, String message) {
    send(
        config.getString(MAIL_PROBLEM_FROM),
        config.getString(MAIL_AUTOMATIC_SUPPORT_RECIPIENT),
        subject,
        message);
  }

  public void sendSignoff(String user, List<String> projectKeys, String nextReleaseName) {
    send(
        config.getString(MAIL_NORMAL_FROM),
        config.getString(MAIL_AUTOMATIC_SUPPORT_RECIPIENT),
        format("Signed off Projects: %s", projectKeys),
        template(MAIL_SIGNOFF_BODY, user, projectKeys, nextReleaseName));
  }

  public void sendFeedback(Feedback feedback) {
    send(
        feedback.getEmail(),
        config.getString(MAIL_MANUAL_SUPPORT_RECIPIENT),
        feedback.getSubject(),
        feedback.getMessage());
  }

  public void sendValidated(String releaseName, String projectKey, SubmissionState state, Set<Address> addresses) {
    try {
      Message message = message();
      message.setSubject(template(MAIL_VALIDATION_SUBJECT, projectKey, state));

      String fromEmail;
      if (state == ERROR) {
        fromEmail = config.getString(MAIL_PROBLEM_FROM);

        // Send email to admin when Error occurs
        addresses.add(address(config.getString(MAIL_ADMIN_RECIPIENT)));
        message.setText(template(MAIL_ERROR_BODY, projectKey, state));
      } else {
        fromEmail = config.getString(MAIL_NORMAL_FROM);
        if (state == VALID) {
          message.setText(template(MAIL_VALID_BODY, projectKey, state, projectKey, projectKey));
        } else if (state == INVALID) {
          message.setText(template(MAIL_INVALID_BODY, projectKey, state, projectKey, projectKey));
        }
      }

      message.setFrom(address(fromEmail));
      message.addRecipients(TO, recipients(addresses));

      Transport.send(message);
      log.info("Emails for '{}' sent to '{}'", projectKey, addresses);
    } catch (Exception e) {
      log.error("An error occured while emailing: ", e);
    }
  }

  private void send(String from, String recipient, String subject, String text) {
    try {
      Message message = message();
      message.setFrom(address(from));
      message.addRecipient(TO, new InternetAddress(recipient));
      message.setSubject(subject);
      message.setText(text);

      Transport.send(message);
      log.info("Emails for '{}' sent to '{}'", subject, recipient);
    } catch (Exception e) {
      log.error("An error occured while emailing: ", e);
    }
  }

  private Message message() {
    Properties props = new Properties();
    props.put(MAIL_SMTP_HOST, config.getString(MAIL_SMTP_HOST));

    return new MimeMessage(Session.getDefaultInstance(props, null));
  }

  private String template(String templateName, Object... arguments) {
    return format(config.getString(templateName), arguments);
  }

  private static InternetAddress address(String email) throws UnsupportedEncodingException {
    return new InternetAddress(email, email);
  }

  private static Address[] recipients(Set<Address> addresses) {
    return addresses.toArray(new Address[addresses.size()]);
  }

}
