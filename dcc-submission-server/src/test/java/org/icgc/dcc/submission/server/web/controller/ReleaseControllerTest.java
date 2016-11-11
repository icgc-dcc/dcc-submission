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

import static org.icgc.dcc.common.core.model.FileTypes.FileType.BIOMARKER_TYPE;
import static org.icgc.dcc.common.test.json.JsonNodes.$;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import lombok.val;

import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.fs.SubmissionFile;
import org.icgc.dcc.submission.release.model.DetailedSubmission;
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

  private static final String RELEASE_NAME = "ICGC13";

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
    release = new Release(RELEASE_NAME);
    release.setDictionaryVersion("0.6e");
    release.setName(RELEASE_NAME);
    release.setReleaseDate();

    val projectOne = new Project("project1", "project one");
    val submissionOne = new Submission(projectOne.getKey(), projectOne.getName(), release.getName());
    submissionOne.setLastUpdated(release.getReleaseDate());
    val dSubmissionOne = new DetailedSubmission(submissionOne, projectOne);
    dSubmissionOne.setSubmissionFiles(ImmutableList.of(new SubmissionFile("/f1.txt", release.getReleaseDate(), 1,
        BIOMARKER_TYPE, false)));

    val projectTwo = new Project("project2", "project two");
    val submissionTwo = new Submission(projectTwo.getKey(), projectTwo.getName(), release.getName());
    submissionTwo.setLastUpdated(release.getReleaseDate());
    val dSubmissionTwo = new DetailedSubmission(submissionTwo, projectTwo);

    when(releaseService.getReleases()).thenReturn(ImmutableList.of(release));
    when(releaseService.getDetailedSubmissionsBySubject(eq(RELEASE_NAME), any(Authentication.class))).thenReturn(
        ImmutableList.of(dSubmissionOne, dSubmissionTwo));

    when(systemService.getTransferringFiles("project1")).thenReturn(ImmutableList.of("/f1.txt"));
  }

  @Test
  public void testGetReleases() throws Exception {
    val expectedJson = $("[{name:'ICGC13', state:'OPENED', releaseDate:"
        + release.getReleaseDate().getTime() + ",dictionaryVersion:'0.6e'}]").toString();
    mvc
        .perform(
            get("/ws/releases")
                .accept(MediaType.APPLICATION_JSON)
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(
            content().json(expectedJson, true));
  }

  @Test
  public void testGetSubmissions() throws Exception {
    val releaseDate = release.getReleaseDate().getTime();
    val expectedJson = $(
        "["
            + "{projectKey:'project1', projectName:'project one', locked:true, projectAlias:null, lastUpdated:"
            + releaseDate
            + ",state:'NOT_VALIDATED',report:{dataTypeReports:[]},releaseName:'ICGC13',submissionFiles:["
            + "{name:'/f1.txt',lastUpdate:" + releaseDate
            + ",size:1,fileType:'BIOMARKER_TYPE',transferring:true}]}"
            + ","
            + "{projectKey:'project2', projectName:'project two', locked:true, projectAlias:null, lastUpdated:"
            + releaseDate
            + ",state:'NOT_VALIDATED',report:{dataTypeReports:[]},releaseName:'ICGC13',submissionFiles:[]}"
            + "]").toString();

    mvc
        .perform(
            get("/ws/releases/ICGC13/submissions")
                .accept(MediaType.APPLICATION_JSON)
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(content().json(expectedJson, true));
  }

}
