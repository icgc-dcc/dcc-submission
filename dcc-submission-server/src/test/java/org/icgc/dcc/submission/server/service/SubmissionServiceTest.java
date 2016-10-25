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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import lombok.val;

import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.server.repository.SubmissionRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;

import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class SubmissionServiceTest {

  private static final String PROJECT_ONE_KEY = "p1";
  private static final String RELEASE_NAME = "release1";
  private static final String USERNAME = "ricardo";

  @Mock
  MailService mailService;
  @Mock
  ProjectService projectService;
  @Mock
  SubmissionRepository submissionRepository;

  @Mock
  Authentication authentication;

  @InjectMocks
  SubmissionService submissionService;

  Project project1 = new Project(PROJECT_ONE_KEY, "project one");
  Submission submission1 = new Submission(project1.getKey(), project1.getName(), RELEASE_NAME);

  @Before
  public void setUp() {
    when(authentication.getName()).thenReturn(USERNAME);
    when(projectService.getProjectKeysByUser(USERNAME)).thenReturn(ImmutableList.of(project1));
  }

  @Test
  public void testFindSubmissionStatesByReleaseNameAndSubject() throws Exception {
    when(
        submissionRepository.findSubmissionStateByReleaseNameAndProjectKeys(RELEASE_NAME,
            ImmutableList.of(PROJECT_ONE_KEY)))
        .thenReturn(ImmutableList.of(submission1));

    val submissions = submissionService.findSubmissionStatesByReleaseNameAndSubject(RELEASE_NAME, authentication);
    assertThat(submissions).containsOnly(submission1);
  }

  @Test
  public void testFindSubmissionsBySubject() throws Exception {
    when(
        submissionRepository.findSubmissionsByReleaseNameAndProjectKey(RELEASE_NAME,
            ImmutableList.of(PROJECT_ONE_KEY)))
        .thenReturn(ImmutableList.of(submission1));

    val submissions = submissionService.findSubmissionsBySubject(RELEASE_NAME, authentication);
    assertThat(submissions).containsOnly(submission1);
  }

}
