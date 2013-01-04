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
package org.icgc.dcc.integration;

import static org.apache.hadoop.hdfs.MiniDFSCluster.PROP_TEST_BUILD_DATA;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.mapred.ClusterMapReduceTestCase;
import org.apache.hadoop.mapred.JobConf;

/**
 * Basis for pseudo-distributed Hadoop integration tests.
 */
public abstract class BaseClusterTest extends ClusterMapReduceTestCase {

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

    System.setProperty("HADOOP_USER_NAME", System.getProperty("user.name"));
    System.setProperty("hadoop.log.dir", "target/test/hadoop/logs");
    System.setProperty(PROP_TEST_BUILD_DATA, "target/test/hadoop");
  }

  @Override
  protected void setUp() throws Exception {
    File test = new File("target/test");
    if(test.exists()) {
      FileUtils.deleteDirectory(test);
    }

    Properties props = new Properties();
    super.startCluster(true, props);

    // Get the dynamic values of the namenode and job tracker
    JobConf jobConf = createJobConf();
    String jobTracker = jobConf.get("mapred.job.tracker");
    String fs = jobConf.get("fs.defaultFS");

    // Set for typesafe config overrides
    System.setProperty("hadoop.mapred.job.tracker", jobTracker);
    System.setProperty("fs.url", fs);
    System.setProperty("hadoop.mapred.compress.map.output", "false"); // Snappy compression not available in test
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    moveTestFiles();
  }

  /**
   * Accounts for MAPREDUCE-2817 to keep the working tree clean.
   * 
   * @throws IOException
   * @see https://issues.apache.org/jira/browse/MAPREDUCE-2817
   */
  private void moveTestFiles() throws IOException {
    File target = new File("target/test/hadoop/build");
    File build = new File("build");
    build.renameTo(target);
  }

}
