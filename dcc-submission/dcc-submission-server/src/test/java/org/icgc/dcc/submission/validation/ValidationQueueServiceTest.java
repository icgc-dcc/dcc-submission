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
package org.icgc.dcc.submission.validation;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.release.NextRelease;
import org.icgc.dcc.submission.release.ReleaseService;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.validation.service.ValidationService;
import org.icgc.dcc.submission.validation.ValidationQueueService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.typesafe.config.Config;

@RunWith(MockitoJUnitRunner.class)
public class ValidationQueueServiceTest {

  @Mock
  private Release mockRelease;
  @Mock
  private NextRelease mockNextRelease;
  @Mock
  private ReleaseService mockReleaseService;
  @Mock
  private ValidationService mockValidationService;
  @Mock
  private Config mockConfig;
  @Mock
  private MailService mockMailService;

  private ValidationQueueService validationQueueService;

  @Before
  public void setUp() {
    when(mockRelease.getName()).thenReturn("release1");
    when(mockNextRelease.getRelease()).thenReturn(mockRelease);
    when(mockReleaseService.resolveNextRelease()).thenReturn(mockNextRelease);
    when(mockNextRelease.getQueued())
        .thenReturn(Arrays.asList("project1", "project2", "project3"))
        .thenReturn(Arrays.asList("project2", "project3"))
        .thenReturn(Arrays.asList("project3"))
        .thenReturn(new ArrayList<String>());

    validationQueueService =
        new ValidationQueueService(mockReleaseService, mockValidationService, mockMailService, mockConfig);
  }

  @Test
  public void test_startScheduler_valid() throws Exception {
    validationQueueService.startAsync();
    Thread.sleep(4000);
    validationQueueService.stopAsync();
  }
}
