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
package org.icgc.dcc.reporter;

import static org.icgc.dcc.core.util.Jackson.formatPrettyJson;
import static org.icgc.dcc.hadoop.fs.FileSystems.getLocalFileSystem;

import java.util.Set;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.CommonConfigurationKeysPublic;
import org.icgc.dcc.core.util.Protocol;
import org.icgc.dcc.core.util.Separators;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * TODO: add checks
 */
@Slf4j
public class ReporterTest {

  private static final String TEST_RELEASE_NAME = "reporter-release-name";
  private static final String DEFAULT_PARENT_TEST_DIR = "src/test/resources/data";
  private static final String TEST_CONF_DIR = "src/test/resources/conf";

  @Test
  public void test_reporter() {
    val projectKeys = ImmutableSet.of("p1", "p2");
    val outputDirPath = Reporter.report(
        TEST_RELEASE_NAME,
        Optional.<Set<String>> of(projectKeys),
        DEFAULT_PARENT_TEST_DIR,
        TEST_CONF_DIR + "/projects.json",
        TEST_CONF_DIR + "/Dictionary.json",
        TEST_CONF_DIR + "/CodeList.json",
        ImmutableMap.<String, String> of(
            CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY,
            Protocol.FILE.getId() + Separators.PATH));

    val fileSystem = getLocalFileSystem();
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
