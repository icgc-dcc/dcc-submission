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
package org.icgc.dcc.submission.service;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.submission.release.model.SubmissionState.ERROR;
import static org.icgc.dcc.submission.release.model.SubmissionState.NOT_VALIDATED;
import static org.icgc.dcc.submission.service.MailService.MAIL_ENABLED;
import static org.icgc.dcc.submission.service.MailService.MAIL_ERROR_BODY;
import static org.icgc.dcc.submission.service.MailService.MAIL_FROM;
import static org.icgc.dcc.submission.service.MailService.MAIL_INVALID_BODY;
import static org.icgc.dcc.submission.service.MailService.MAIL_NOTIFICATION_RECIPIENT;
import static org.icgc.dcc.submission.service.MailService.MAIL_NOT_VALIDATED_BODY;
import static org.icgc.dcc.submission.service.MailService.MAIL_SIGNOFF_BODY;
import static org.icgc.dcc.submission.service.MailService.MAIL_SMTP_HOST;
import static org.icgc.dcc.submission.service.MailService.MAIL_SUPPORT_RECIPIENT;
import static org.icgc.dcc.submission.service.MailService.MAIL_VALIDATION_SUBJECT;
import static org.icgc.dcc.submission.service.MailService.MAIL_VALID_BODY;
import static org.icgc.dcc.submission.service.MailService.NOTIFICATION_SUBJECT_PREFEX;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import java.util.List;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.submission.core.report.Report;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Transport.class)
public class MailServiceTest {

  Config config;

  /**
   * Class under test.
   */
  MailService mailService;

  @Before
  public void setUp() {
    mockStatic(Transport.class);

    this.config = mockConfig();
    this.mailService = new MailService(config);
  }

  @Test
  @SneakyThrows
  public void test_sendSupportProblem() {
    val subject = "subject";
    val text = "text";

    mailService.sendSupportProblem(subject, text);
    sleepUninterruptibly(1, SECONDS);

    verifyStatic(times(2));

    val messages = getMessages();
    assertThat(messages.size()).isEqualTo(2);

    val m1 = messages.get(0);
    val m2 = messages.get(1);
    val notifyMessage = isNotification(m1) ? m1 : m2;
    val supportMessage = isNotification(m1) ? m2 : m1;

    assertThat(notifyMessage.getFrom()).contains(address(get(MAIL_FROM)));
    assertThat(notifyMessage.getAllRecipients()).contains(address(get(MAIL_NOTIFICATION_RECIPIENT)));

    assertThat(supportMessage.getFrom()).contains(address(get(MAIL_FROM)));
    assertThat(supportMessage.getAllRecipients()).contains(address(get(MAIL_SUPPORT_RECIPIENT)));
    assertThat(supportMessage.getSubject()).endsWith(subject);
    assertThat(supportMessage.getContent()).isEqualTo(text);
  }

  @Test
  @SneakyThrows
  public void test_sendValidated_with_ERROR_state() {
    val releaseName = "releaseName";
    val projectKey = "projectKey";
    val state = ERROR;
    val emails = newArrayList("email@domain.com");
    val addresses = newHashSet(address("email@domain.com"));
    val report = new Report();

    mailService.sendValidationResult(releaseName, projectKey, emails, state, report);
    sleepUninterruptibly(1, SECONDS);

    verifyStatic(times(2));

    val messages = getMessages();
    assertThat(messages.size()).isEqualTo(2);

    val m1 = messages.get(0);
    val m2 = messages.get(1);
    val notifyMessage = isNotification(m1) ? m1 : m2;
    val supportMessage = isNotification(m1) ? m2 : m1;

    assertThat(notifyMessage.getFrom()).contains(address(get(MAIL_FROM)));
    assertThat(notifyMessage.getAllRecipients()).contains(address(get(MAIL_NOTIFICATION_RECIPIENT)));

    assertThat(supportMessage.getFrom()).contains(address(get(MAIL_FROM)));
    assertThat(supportMessage.getAllRecipients()).containsAll(addresses);
    assertThat(supportMessage.getSubject()).endsWith(template(MAIL_VALIDATION_SUBJECT, projectKey, state));
    assertThat(supportMessage.getContent()).isEqualTo(template(MAIL_ERROR_BODY, projectKey, state));
  }

  @Test
  @SneakyThrows
  public void test_sendValidated_with_NOT_VALIDATED_state() {
    val releaseName = "releaseName";
    val projectKey = "projectKey";
    val state = NOT_VALIDATED;
    val emails = newArrayList("email@domain.com");
    val addresses = newHashSet(address("email@domain.com"));
    val report = new Report();

    mailService.sendValidationResult(releaseName, projectKey, emails, state, report);
    sleepUninterruptibly(1, SECONDS);

    verifyStatic(times(2));

    val messages = getMessages();
    assertThat(messages.size()).isEqualTo(2);

    val m1 = messages.get(0);
    val m2 = messages.get(1);
    val notifyMessage = isNotification(m1) ? m1 : m2;
    val supportMessage = isNotification(m1) ? m2 : m1;

    assertThat(notifyMessage.getFrom()).contains(address(get(MAIL_FROM)));
    assertThat(notifyMessage.getAllRecipients()).contains(address(get(MAIL_NOTIFICATION_RECIPIENT)));

    assertThat(supportMessage.getFrom()).contains(address(get(MAIL_FROM)));
    assertThat(supportMessage.getAllRecipients()).containsAll(addresses);
    assertThat(supportMessage.getSubject()).endsWith(template(MAIL_VALIDATION_SUBJECT, projectKey, state));
    assertThat(supportMessage.getContent()).isEqualTo(
        template(MAIL_NOT_VALIDATED_BODY, projectKey, state, releaseName, projectKey));
  }

  private static boolean isNotification(MimeMessage message) throws MessagingException {
    // Low-tech but works
    return message.getSubject().contains(NOTIFICATION_SUBJECT_PREFEX);
  }

  @SneakyThrows
  private static List<MimeMessage> getMessages() {
    val captor = ArgumentCaptor.forClass(MimeMessage.class);
    Transport.send(captor.capture());

    return captor.getAllValues();
  }

  private static Config mockConfig() {
    Config config = mock(Config.class);

    when(config.hasPath(MAIL_ENABLED)).thenReturn(true);
    when(config.getBoolean(MAIL_ENABLED)).thenReturn(true);
    for (val name : new String[] { MAIL_SMTP_HOST, MAIL_FROM, MAIL_SUPPORT_RECIPIENT, MAIL_NOTIFICATION_RECIPIENT }) {
      when(config.hasPath(name)).thenReturn(true);
      when(config.getString(name)).thenReturn(name);
    }
    for (val name : new String[] { MAIL_VALIDATION_SUBJECT, MAIL_ERROR_BODY }) {
      when(config.hasPath(name)).thenReturn(true);
      when(config.getString(name)).thenReturn("%s:%s");
    }
    for (val name : new String[] { MAIL_SIGNOFF_BODY }) {
      when(config.hasPath(name)).thenReturn(true);
      when(config.getString(name)).thenReturn("%s:%s:%s");
    }
    for (val name : new String[] { MAIL_NOT_VALIDATED_BODY, MAIL_INVALID_BODY, MAIL_VALID_BODY }) {
      when(config.hasPath(name)).thenReturn(true);
      when(config.getString(name)).thenReturn("%s:%s:%s:%s");
    }

    return config;
  }

  @SneakyThrows
  private static Address address(String email) {
    return new InternetAddress(email, email);
  }

  private String get(String key) {
    return config.getString(key);
  }

  private String template(String templateName, Object... arguments) {
    return format(get(templateName), arguments);
  }

}
