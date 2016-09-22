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
package org.icgc.dcc.submission.server.web.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.icgc.dcc.submission.core.model.User;
import org.icgc.dcc.submission.server.security.SecurityConfig;
import org.icgc.dcc.submission.server.service.ProjectService;
import org.icgc.dcc.submission.server.service.UserService;
import org.icgc.dcc.submission.server.test.TestConfig;
import org.icgc.dcc.submission.server.web.WebSecurityConfig;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.google.common.base.Optional;

import lombok.val;

@RunWith(SpringRunner.class)
@Import({ TestConfig.class, SecurityConfig.class, WebSecurityConfig.class })
public abstract class ControllerTest {

  /**
   * Test dependencies.
   */
  @Autowired
  protected MockMvc mvc;
  @Autowired
  protected SubmissionProperties properties;

  /**
   * Test collaborators.
   */
  @MockBean
  protected ProjectService projectService;
  @MockBean
  protected UserService userService;

  @Before
  public final void init() {
    for (val user : properties.getAuth().getUsers()) {
      val value = new User();
      value.setUsername(user.getUsername());

      when(userService.getUserByUsername(user.getUsername())).thenReturn(Optional.of(value));
    }

    when(userService.getUserByUsername("unknown")).thenReturn(Optional.absent());
  }

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

  protected ResultMatcher contentEmpty() {
    return content().string("");
  }

  public static String password(String username) {
    return username + "spasswd";
  }

}
