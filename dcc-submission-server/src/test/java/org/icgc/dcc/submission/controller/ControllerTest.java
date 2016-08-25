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
package org.icgc.dcc.submission.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.icgc.dcc.submission.config.AbstractConfig;
import org.icgc.dcc.submission.config.WebSecurityConfig;
import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.icgc.dcc.submission.core.model.User;
import org.icgc.dcc.submission.service.UserService;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.google.common.base.Optional;

import lombok.val;

@RunWith(SpringRunner.class)
@Import(WebSecurityConfig.class)
public class ControllerTest {

  /**
   * Test utilities.
   */
  @Autowired
  protected MockMvc mvc;

  protected RequestPostProcessor admin() {
    return user("admin");
  }

  protected RequestPostProcessor unknown() {
    return user("unknown");
  }

  protected RequestPostProcessor richard() {
    return user("richard");
  }

  protected RequestPostProcessor ricardo() {
    return user("ricardo");
  }

  protected RequestPostProcessor user(String username) {
    return httpBasic(username, password(username));
  }

  public ResultMatcher emptyContent() {
    return content().string("");
  }

  @Configuration
  static class Config extends AbstractConfig {

    @Bean
    @ConfigurationProperties
    public SubmissionProperties submissionProperties() {
      return new SubmissionProperties();
    }

    @Bean
    public UserService userService(SubmissionProperties properties) {
      val userService = mock(UserService.class);
      for (val user : properties.getAuth().getUsers()) {
        val value = new User();
        value.setUsername(user.getUsername());

        when(userService.getUserByUsername(user.getUsername())).thenReturn(Optional.of(value));
      }

      return userService;
    }

  }

  public static String password(String username) {
    return username + "spasswd";
  }

}
