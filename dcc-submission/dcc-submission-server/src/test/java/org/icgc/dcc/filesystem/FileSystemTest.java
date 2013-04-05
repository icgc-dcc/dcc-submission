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
package org.icgc.dcc.filesystem;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.User;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.junit.Before;

import com.typesafe.config.Config;

public class FileSystemTest {

  protected static final String USERNAME = "vegeta";

  protected static final String ROOT_DIR = "/tmp/my_root_fs_dir";

  protected static final String RELEASE_NAME = "ICGC4";

  protected static final String PROJECT_NAME = "dragon_balls_quest";

  protected static final String PROJECT_KEY = "DBQ";

  protected static final String FILENAME_1 = "cnsm__bla__bla__p__bla__bla.tsv";

  protected static final String FILENAME_2 = "cnsm__bla__bla__s__bla__bla.tsv";

  protected static final String FILEPATH_1 = ROOT_DIR + RELEASE_NAME + "/" + PROJECT_KEY + "/" + FILENAME_1;

  protected static final String FILEPATH_2 = ROOT_DIR + RELEASE_NAME + "/" + PROJECT_KEY + "/" + FILENAME_2;

  protected Release mockRelease;

  protected User mockUser;

  protected Project mockProject;

  protected Submission mockSubmission;

  protected Config mockConfig;

  static {
    setProperties();
  }

  /**
   * Sets key system properties before test initialization.
   */
  private static void setProperties() {
    // See http://stackoverflow.com/questions/7134723/hadoop-on-osx-unable-to-load-realm-info-from-scdynamicstore
    System.setProperty("java.security.krb5.realm", "OX.AC.UK");
    System.setProperty("java.security.krb5.kdc", "kdc0.ox.ac.uk:kdc1.ox.ac.uk");
  }

  @Before
  public void setUp() throws IOException {

    this.mockConfig = mock(Config.class);
    this.mockRelease = mock(Release.class);
    this.mockUser = mock(User.class);
    this.mockProject = mock(Project.class);
    this.mockSubmission = mock(Submission.class);

    when(this.mockConfig.getString(FsConfig.FS_ROOT)).thenReturn(ROOT_DIR);

    when(this.mockRelease.getName()).thenReturn(RELEASE_NAME);

    when(this.mockUser.getName()).thenReturn(USERNAME);

    when(this.mockProject.getName()).thenReturn(PROJECT_NAME);
    when(this.mockProject.getKey()).thenReturn(PROJECT_KEY);
    when(this.mockProject.hasUser(this.mockUser.getName())).thenReturn(true);

    when(this.mockSubmission.getState()).thenReturn(SubmissionState.SIGNED_OFF);

    when(this.mockRelease.getSubmission(this.mockProject.getKey())).thenReturn(this.mockSubmission);
    List<String> projectKeys = Arrays.asList(this.mockProject.getKey()); // must be separated from thenReturn call
                                                                         // (mockito bug:
                                                                         // http://code.google.com/p/mockito/issues/detail?id=53)
    when(this.mockRelease.getProjectKeys()).thenReturn(projectKeys);
  }
}
