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

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.icgc.dcc.submission.release.model.ReleaseSubmissionView;
import org.icgc.dcc.submission.server.service.ReleaseService;
import org.icgc.dcc.submission.server.service.SystemService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

@WebMvcTest(ReleaseController.class)
public class ReleaseControllerTest extends ControllerTest {

  /**
   * Test data.
   */
  ReleaseSubmissionView release;

  /**
   * Test collaborators.
   */
  @MockBean
  private ReleaseService releaseService;
  @MockBean
  private SystemService systemService;

  @Before
  public void setUp() {
    release = new ReleaseSubmissionView("ICGC13");
    release.setDictionaryVersion("0.6e");
    release.setName("ICGC13");
    release.setReleaseDate();
    // release.addSubmission(new Submission("project1", "project one", release.getName()));
    // release.addSubmission(new Submission("project2", "project two", release.getName()));

    when(releaseService.getReleasesBySubject(any(Authentication.class))).thenReturn(newArrayList(release));
  }

  @Test
  public void testGetReleases() throws Exception {
    mvc
        .perform(
            get("/ws/releases")
                .accept(MediaType.APPLICATION_JSON)
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    "[{\"name\":\"ICGC13\",\"state\":\"OPENED\",\"releaseDate\":"
                        + release.getReleaseDate().getTime()
                        + ",\"dictionaryVersion\":\"0.6e\",\"submissions\":[{\"projectKey\":\"project1\",\"projectName\":\"project one\",\"releaseName\":\"ICGC13\"},"
                        + "{\"projectKey\":\"project2\",\"projectName\":\"project two\",\"releaseName\":\"ICGC13\"}]}]"));
  }

}
