/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.validation;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;

import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.release.NextRelease;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.validation.factory.LocalCascadingStrategyFactory;
import org.icgc.dcc.validation.service.ValidationQueueManagerService;
import org.icgc.dcc.validation.service.ValidationService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Optional;

public class ValidationQueueManagerServiceTest {

  private Release mockRelease;

  private NextRelease mockNextRelease;

  private ReleaseService mockReleaseService;

  private DictionaryService mockDictionaryService;

  private ValidationService mockValidationService;

  private ValidationQueueManagerService validationQueueManagerService;

  private DccFileSystem mockDccFileSystem;

  private ProjectService mockProjectService;

  @Before
  public void setUp() {
    mockRelease = mock(Release.class);
    mockNextRelease = mock(NextRelease.class);
    mockReleaseService = mock(ReleaseService.class);
    mockDictionaryService = mock(DictionaryService.class);
    mockValidationService = mock(ValidationService.class);
    mockDccFileSystem = mock(DccFileSystem.class);
    mockProjectService = mock(ProjectService.class);

    when(mockRelease.getName()).thenReturn("release1");
    when(mockNextRelease.getRelease()).thenReturn(mockRelease);
    when(mockReleaseService.getNextRelease()).thenReturn(mockNextRelease);
    when(mockNextRelease.getQueued()).thenReturn(Arrays.asList("project1", "project2", "project3"))
        .thenReturn(Arrays.asList("project2", "project3")).thenReturn(Arrays.asList("project3"))
        .thenReturn(new ArrayList<String>());
    when(mockReleaseService.dequeue(anyString(), anyBoolean())).thenReturn(Optional.<String> of("project1"));

    validationQueueManagerService =
        new ValidationQueueManagerService(mockReleaseService, mockDictionaryService, mockValidationService,
            mockDccFileSystem, mockProjectService, new LocalCascadingStrategyFactory());
  }

  @Ignore
  @Test
  public void test_handleSuccessfulValidation_invalidProjectKey() {
    validationQueueManagerService.handleSuccessfulValidation("project0", null);
  }

  @Test
  public void test_handleFailedValidation_invalidProjectKey() {
    validationQueueManagerService.handleFailedValidation("project0");
  }

  @Test
  public void test_startScheduler_valid() throws Exception {
    validationQueueManagerService.start();
    Thread.sleep(4000);
    validationQueueManagerService.stop();
  }
}
