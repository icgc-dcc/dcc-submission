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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MiniMRCluster;

/**
 * Wraps Hadoop's built-in {@link MiniMRCluster} and and {@link MiniDFSCluster} to make them work outside of Hadoop's
 * build environment.
 */
public class MiniHadoop {

  /**
   * The in-memory HDFS cluster.
   */
  private MiniDFSCluster dfsCluster = null;

  /**
   * The in-memory MapReduce cluster.
   */
  private MiniMRCluster mrCluster = null;

  /**
   * A handle to the in-memory HDFS.
   */
  private FileSystem fileSystem = null;

  /**
   * Creates a {@link MiniMRCluster} and {@link MiniDFSCluster} all working within the directory supplied in
   * {@code tmpDir}.
   * 
   * The DFS will be formatted regardless if there was one or not before in the given location.
   * 
   * @param config the Hadoop configuration
   * @param taskTrackers number of task trackers to start
   * @param dataNodes number of data nodes to start
   * @param tmpDir the temporary directory which the Hadoop cluster will use for storage
   * @throws IOException thrown if the base directory cannot be set.
   */
  public MiniHadoop(final Configuration config, final int taskTrackers, final int dataNodes,
      final File tmpDir)
      throws IOException {

    if (taskTrackers < 1) {
      throw new IllegalArgumentException(
          "Invalid taskTrackers value, must be greater than 0");
    }
    if (dataNodes < 1) {
      throw new IllegalArgumentException(
          "Invalid dataNodes value, must be greater than 0");
    }

    config.set("hadoop.tmp.dir", tmpDir.getAbsolutePath());

    if (tmpDir.exists()) {
      FileUtils.forceDelete(tmpDir);
    }
    FileUtils.forceMkdir(tmpDir);

    // used by MiniDFSCluster for DFS storage
    System.setProperty("test.build.data", new File(tmpDir, "data").getAbsolutePath());

    // required by JobHistory.initLogDir
    System.setProperty("hadoop.log.dir", new File(tmpDir, "logs").getAbsolutePath());

    JobConf jobConfig = new JobConf(config);

    dfsCluster = new MiniDFSCluster.Builder(jobConfig).numDataNodes(dataNodes).format(true).build();
    fileSystem = dfsCluster.getFileSystem();
    mrCluster = new MiniMRCluster(0, 0, taskTrackers, fileSystem.getUri().toString(), 1,
        null, null, null, jobConfig);
  }

  /**
   * Destroys the MiniDFSCluster and MiniMRCluster Hadoop instance.
   * 
   * @throws Exception if shutdown fails
   */
  public void close() throws Exception {
    mrCluster.shutdown();
    dfsCluster.shutdown();
  }

  /**
   * Returns the Filesystem.
   * 
   * @return the filesystem used by Hadoop.
   */
  public FileSystem getFileSystem() {
    return fileSystem;
  }

  /**
   * Returns a job configuration preconfigured to run against the Hadoop managed by this instance.
   * 
   * @return configuration that works on the testcase Hadoop instance
   */
  public JobConf createJobConf() {
    return mrCluster.createJobConf();
  }

  /**
   * Returns a job configuration preconfigured to run against the Hadoop managed by this instance.
   * 
   * @param config a {@link JobConf} whose configuration is folded into the returned {@link JobConf}.
   * @return configuration that works on the testcase Hadoop instance
   */
  public JobConf createJobConf(final JobConf config) {
    return mrCluster.createJobConf(config);
  }
}