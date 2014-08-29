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
package org.icgc.dcc.submission.reporter;

import static org.icgc.dcc.core.Component.REPORTER;
import static org.icgc.dcc.core.DccResources.getCodeListsDccResource;
import static org.icgc.dcc.core.DccResources.getDictionaryDccResource;
import static org.icgc.dcc.core.util.Jackson.formatPrettyJson;
import static org.icgc.dcc.core.util.Joiners.PATH;
import static org.icgc.dcc.hadoop.fs.FileSystems.getDefaultLocalFileSystem;
import static org.icgc.dcc.test.Tests.CONF_DIR_NAME;
import static org.icgc.dcc.test.Tests.DATA_DIR_NAME;
import static org.icgc.dcc.test.Tests.MAVEN_TEST_RESOURCES_DIR;
import static org.icgc.dcc.test.Tests.PROJECT1;
import static org.icgc.dcc.test.Tests.PROJECT2;
import static org.icgc.dcc.test.Tests.PROJECTS_JSON_FILE_NAME;
import static org.icgc.dcc.test.Tests.TEST_PATCH_NUMBER;
import static org.icgc.dcc.test.Tests.getTestReleasePrefix;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.Component;
import org.icgc.dcc.core.util.Optionals;
import org.icgc.dcc.hadoop.cascading.CascadingContext;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * TODO: add checks
 */
@Slf4j
public class ReporterTest {

  private static final Component TESTED_COMPONENT = REPORTER;
  private static final String TEST_RELEASE_NAME = getTestReleasePrefix(TESTED_COMPONENT) + TEST_PATCH_NUMBER;
  private static final String DEFAULT_PARENT_TEST_DIR = PATH.join(MAVEN_TEST_RESOURCES_DIR, DATA_DIR_NAME);
  private static final String TEST_CONF_DIR = PATH.join(MAVEN_TEST_RESOURCES_DIR, CONF_DIR_NAME);

  @Test
  public void test_reporter() {
    val projectKeys = ImmutableSet.of(PROJECT1, PROJECT2);

    val outputDirPath = Reporter.report(
        TEST_RELEASE_NAME,
        Optionals.of(projectKeys),
        DEFAULT_PARENT_TEST_DIR,
        PATH.join(TEST_CONF_DIR, PROJECTS_JSON_FILE_NAME),
        getDictionaryDccResource(),
        getCodeListsDccResource(),
        CascadingContext
            .getLocal()
            .getConnectors()
            .getDefaultProperties());

    val fileSystem = getDefaultLocalFileSystem();
    for (val projectKey : projectKeys) {
      val documents = ReporterCollector.getJsonProjectDataTypeEntity(
          fileSystem, outputDirPath, TEST_RELEASE_NAME, projectKey);
      log.info("Content for '{}': '{}'", projectKey, formatPrettyJson(documents));
    }
    for (val projectKey : projectKeys) {
      val documents = ReporterCollector.getJsonProjectSequencingStrategy(
          fileSystem, outputDirPath, TEST_RELEASE_NAME, projectKey, ImmutableMap.<String, String> of());
      log.info("Content for '{}': '{}'", projectKey, formatPrettyJson(documents));
    }

  }

}
