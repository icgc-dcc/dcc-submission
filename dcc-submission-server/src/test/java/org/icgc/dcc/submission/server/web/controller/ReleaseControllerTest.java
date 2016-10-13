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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import lombok.val;

import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.server.service.ReleaseService;
import org.icgc.dcc.submission.server.service.SubmissionService;
import org.icgc.dcc.submission.server.service.SystemService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

import com.google.common.collect.ImmutableList;

@WebMvcTest(ReleaseController.class)
public class ReleaseControllerTest extends ControllerTest {

  /**
   * Test data.
   */
  Release release;

  /**
   * Test collaborators.
   */
  @MockBean
  private ReleaseService releaseService;
  @MockBean
  private SubmissionService submissionService;
  @MockBean
  private SystemService systemService;

  @Before
  public void setUp() {
    release = new Release("ICGC13");
    release.setDictionaryVersion("0.6e");
    release.setName("ICGC13");
    release.setReleaseDate();
    val submissionOne = new Submission("project1", "project one", release.getName());
    val submissionTwo = new Submission("project2", "project two", release.getName());
    submissionOne.setLastUpdated(release.getReleaseDate());
    submissionTwo.setLastUpdated(release.getReleaseDate());

    when(releaseService.getReleases()).thenReturn(ImmutableList.of(release));
    when(submissionService.findSubmissionsBySubject(any(Authentication.class))).thenReturn(
        ImmutableList.of(submissionOne, submissionTwo));
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
                .json(
                    "[{\"name\":\"ICGC13\",\"state\":\"OPENED\",\"releaseDate\":"
                        + release.getReleaseDate().getTime()
                        + ",\"dictionaryVersion\":\"0.6e\"}]",
                    true));
  }

  @Test
  public void testGetSubmissions() throws Exception {
    val releaseDate = release.getReleaseDate().getTime();
    mvc
        .perform(
            get("/ws/releases/submissions")
                .accept(MediaType.APPLICATION_JSON)
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    "[{\"projectKey\":\"project1\",\"projectName\":\"project one\",\"releaseName\":\"ICGC13\","
                        + "\"lastUpdated\":" + releaseDate
                        + ",\"state\":\"NOT_VALIDATED\",\"report\":{\"dataTypeReports\":[]}}"
                        + ","
                        + "{\"projectKey\":\"project2\",\"projectName\":\"project two\",\"releaseName\":\"ICGC13\","
                        + "\"lastUpdated\":" + releaseDate
                        + ",\"state\":\"NOT_VALIDATED\",\"report\":{\"dataTypeReports\":[]}}]",
                    true));
  }

}
