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

import static org.fest.assertions.api.Assertions.assertThat;
import static org.icgc.dcc.submission.core.MailService.MAIL_ADMIN_RECIPIENT;
import static org.icgc.dcc.submission.core.MailService.MAIL_AUTOMATIC_SUPPORT_RECIPIENT;
import static org.icgc.dcc.submission.core.MailService.MAIL_ERROR_BODY;
import static org.icgc.dcc.submission.core.MailService.MAIL_INVALID_BODY;
import static org.icgc.dcc.submission.core.MailService.MAIL_MANUAL_SUPPORT_RECIPIENT;
import static org.icgc.dcc.submission.core.MailService.MAIL_NORMAL_FROM;
import static org.icgc.dcc.submission.core.MailService.MAIL_PROBLEM_FROM;
import static org.icgc.dcc.submission.core.MailService.MAIL_SIGNOFF_BODY;
import static org.icgc.dcc.submission.core.MailService.MAIL_SMTP_HOST;
import static org.icgc.dcc.submission.core.MailService.MAIL_VALIDATION_SUBJECT;
import static org.icgc.dcc.submission.core.MailService.MAIL_VALID_BODY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import javax.mail.Address;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import lombok.SneakyThrows;
import lombok.val;

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

    val message = verify(new Runnable() {

      @Override
      public void run() {
        mailService.sendSupportProblem(subject, text);
      }
    });

    assertThat(message.getSubject()).isEqualTo(subject);
    assertThat(message.getContent()).isEqualTo(text);
    assertThat(message.getFrom()).contains(address(value(MAIL_PROBLEM_FROM)));
    assertThat(message.getAllRecipients()).contains(address(value(MAIL_AUTOMATIC_SUPPORT_RECIPIENT)));
  }

  @SneakyThrows
  private static MimeMessage verify(Runnable runnable) {
    runnable.run();
    verifyStatic();

    val captor = ArgumentCaptor.forClass(MimeMessage.class);
    Transport.send(captor.capture());

    return captor.getValue();
  }

  private static Config mockConfig() {
    Config config = mock(Config.class);

    for (val name : new String[] { MAIL_SMTP_HOST, MAIL_NORMAL_FROM, MAIL_PROBLEM_FROM, MAIL_ADMIN_RECIPIENT, MAIL_MANUAL_SUPPORT_RECIPIENT, MAIL_AUTOMATIC_SUPPORT_RECIPIENT }) {
      when(config.getString(name)).thenReturn(name);
    }
    for (val name : new String[] { MAIL_VALIDATION_SUBJECT }) {
      when(config.getString(name)).thenReturn("%s:%s");
    }
    for (val name : new String[] { MAIL_VALID_BODY, MAIL_INVALID_BODY, MAIL_SIGNOFF_BODY, MAIL_ERROR_BODY }) {
      when(config.getString(name)).thenReturn("%s:%s:%s");
    }

    return config;
  }

  @SneakyThrows
  private static Address address(String email) {
    return new InternetAddress(email, email);
  }

  private String value(String key) {
    return config.getString(key);
  }

}
