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
package org.icgc.dcc.submission;

import static com.google.common.collect.Maps.newHashMap;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.hadoop.hdfs.MiniDFSCluster.PROP_TEST_BUILD_DATA;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;

import lombok.SneakyThrows;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.mapred.ClusterMapReduceTestCase;
import org.apache.hadoop.mapred.JobConf;
import org.icgc.dcc.hadoop.util.HadoopConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Basis for pseudo-distributed Hadoop integration tests.
 * <p>
 * TODO: move hadoop constants from this class to {@link HadoopConstants}.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public abstract class BaseClusterTest extends ClusterMapReduceTestCase {

  protected Map<String, String> hadoop = newHashMap();

  static {
    // See HADOOP-7489
    System.setProperty("java.security.krb5.realm", "OX.AC.UK");
    System.setProperty("java.security.krb5.kdc", "kdc0.ox.ac.uk:kdc1.ox.ac.uk");

    // See DCC-572
    System.setProperty(PROP_TEST_BUILD_DATA, "target/test/hadoop");

    // See MAPREDUCE-2785
    System.setProperty("hadoop.log.dir", "target/test/hadoop/logs");

    // Configure logging
    if (System.getProperty("hadoop.log.file") == null) {
      System.setProperty("hadoop.log.file", "hadoop.log");
    }
    if (System.getProperty("hadoop.root.logger") == null) {
      System.setProperty("hadoop.root.logger", "DEBUG,console");
    }

    ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).setLevel(Level.INFO);
    ((Logger) LoggerFactory.getLogger("org.apache.hadoop")).setLevel(Level.WARN);
    ((Logger) LoggerFactory.getLogger("org.apache.hadoop.conf")).setLevel(Level.ERROR);
    ((Logger) LoggerFactory.getLogger("org.apache.hadoop.ipc")).setLevel(Level.ERROR);
    ((Logger) LoggerFactory.getLogger("org.apache.hadoop.hdfs")).setLevel(Level.ERROR);
    ((Logger) LoggerFactory.getLogger("org.apache.hadoop.mapred.TaskTracker")).setLevel(Level.OFF);
    ((Logger) LoggerFactory.getLogger("org.mortbay")).setLevel(Level.WARN);
    ((Logger) LoggerFactory.getLogger("mapreduce")).setLevel(Level.ERROR);
  }

  @Before
  @SneakyThrows
  public void before() {
    File test = new File("target/test");
    if (test.exists()) {
      deleteDirectory(test);
    }

    super.setUp();

    // Relay the dynamic values required for configuration
    JobConf jobConf = createJobConf();
    hadoop.put("fs.defaultFS", jobConf.get("fs.defaultFS"));
    hadoop.put("mapred.job.tracker", jobConf.get("mapred.job.tracker"));
  }

  @After
  public void after() {
    moveTestFiles();
  }

  /**
   * Accounts for MAPREDUCE-2817 to keep the working tree clean.
   * 
   * @see https://issues.apache.org/jira/browse/MAPREDUCE-2817
   */
  @SneakyThrows
  private void moveTestFiles() {
    File target = new File("target/test/hadoop/build");
    File build = new File("build");
    build.renameTo(target);
  }

  protected final Client client = ClientFactory.newClient();

  protected static String _(String format, Object... args) {
    return String.format(format, args);
  }

  protected static void assertEmptyFile(String dir, String path) throws IOException {
    File errorFile = new File(dir, path);
    assertTrue("Expected file does not exist: " + path, errorFile.exists());
    assertTrue("Expected empty file: " + path, FileUtils.readFileToString(errorFile).isEmpty());
  }

}
