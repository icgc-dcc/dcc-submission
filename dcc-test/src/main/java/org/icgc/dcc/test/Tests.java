/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.test;

import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.util.EtlConventions.JOB_ID_JOINER;
import static org.icgc.dcc.core.util.Extensions.JSON;
import static org.icgc.dcc.core.util.Joiners.DASH;
import static org.icgc.dcc.core.util.Joiners.EXTENSION;
import static org.icgc.dcc.core.util.Joiners.PATH;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.icgc.dcc.core.Component;

/**
 * Utility methods and constants for tests.
 */
@NoArgsConstructor(access = PRIVATE)
public final class Tests {

  public static final String TEST = "test";
  public static final String RELEASE = "release";

  public static final String TARGET_DIR_NAME = "target";
  private static final String FIXTURES = "fixtures";

  public static final String INPUT_DIR_NAME = "input";
  public static final String OUTPUT_DIR_NAME = "output";
  public static final String REFERENCE_DIR_NAME = "references";

  public static final String CONF_DIR_NAME = "conf";
  public static final String DATA_DIR_NAME = "data";
  public static final String FS_DIR_NAME = "fs";
  public static final String MONGO_DIR_NAME = "mongo";

  public static final String OS_TMP_DIR = "/tmp";

  public static final String LOCALHOST = "localhost";

  public static final String MAVEN_TEST_RESOURCES_DIR = "src/test/resources";
  public static final String TEST_FIXTURES_DIR = PATH.join(MAVEN_TEST_RESOURCES_DIR, FIXTURES);

  public static final int TEST_RELEASE_NUMBER = 17;
  public static final int TEST_PATCH_NUMBER = 0;
  public static final int TEST_RUN_NUMBER = 0;

  public static final String PROJECT1 = "project1";
  public static final String PROJECT2 = "project2";

  public static final String PROJECTS_JSON_FILE_NAME = EXTENSION.join("projects", JSON);

  public static final String TEST_HOST = "localhost";
  public static final int MONGO_PORT = 27017;
  public static final int ELASTIC_SEARCH_PORT = 9300;

  public static String getTestJobId(@NonNull final Component component) {
    return JOB_ID_JOINER.join(getTestReleaseName(component), TEST_PATCH_NUMBER, TEST_RUN_NUMBER);
  }

  public static String getTestReleaseName(@NonNull final Component component) {
    return getTestReleasePrefix(component) + TEST_RELEASE_NUMBER;
  }

  public static String getTestReleasePrefix(@NonNull final Component component) {
    return DASH.join(component.getId(), TEST, RELEASE);
  }

  public static String getTestWorkingDir(@NonNull final Component component) {
    return PATH.join(OS_TMP_DIR, component.getId());
  }

}
