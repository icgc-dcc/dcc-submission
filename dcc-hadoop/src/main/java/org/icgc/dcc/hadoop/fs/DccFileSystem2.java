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
package org.icgc.dcc.hadoop.fs;

import static java.lang.String.format;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.checkExistence;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.mkdirs;
import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import cascading.flow.FlowConnector;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.flow.local.LocalFlowConnector;
import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tap.local.FileTap;

/**
 * Very basic replacement for {@link DccFileSystem}, as discussed with @Bob Tiernay around 13/11/07 (see DCC-1876). This
 * is a temporary solution until a proper re-modelling of the file operations related objects can happen.
 * <p>
 * Requirements:<br/>
 * - Junjun's tool to re-write specimen file<br/>
 * 
 */
@RequiredArgsConstructor
@Slf4j
public class DccFileSystem2 {

  private final FileSystem fileSystem;

  private final String rootDir;

  private final boolean hadoopMode;

  public FlowConnector getFlowConnector() {
    return hadoopMode ?
        new HadoopFlowConnector() :
        new LocalFlowConnector();
  }

  public Tap<?, ?, ?> getNormalizationDataOutputTap(String releaseName, String projectKey) {
    String path = getNormalizationDataOutputFile(releaseName, projectKey);
    return getTap(path);
  }

  private Tap<?, ?, ?> getTap(String path) {
    return hadoopMode ?
        new Hfs(new cascading.scheme.hadoop.TextDelimited(true, "\t"), path) :
        new FileTap(new cascading.scheme.local.TextDelimited(true, "\t"), path);
  }

  private String getReleasesDir() {
    return format("%s/releases", rootDir);
  }

  private String getReleaseDir(String releaseName) {
    return format("%s/%s", getReleasesDir(), releaseName);
  }

  private String getProjectsDir(String releaseName) {
    return format("%s/projects", getReleaseDir(releaseName));
  }

  private String getProjectDir(String releaseName, String projectKey) {
    return format("%s/%s", getProjectsDir(releaseName), projectKey);
  }

  private String getNormalizationDir(String releaseName, String projectKey) {
    return format("%s/normalization", getProjectDir(releaseName, projectKey));
  }

  private String getNormalizationReportsDir(String releaseName, String projectKey) {
    return format("%s/reports", getNormalizationDir(releaseName, projectKey));
  }

  private String getNormalizationDataDir(String releaseName, String projectKey) {
    return format("%s/data", getNormalizationDir(releaseName, projectKey));
  }

  private String getNormalizationReportOutputFile(String releaseName, String projectKey) {
    return format("%s/filtering.txt",
        lazyDirCreation(getNormalizationReportsDir(releaseName, projectKey)));
  }

  public String getNormalizationDataOutputFile(String releaseName, String projectKey) {
    return format("%s/ssm__p.txt",
        lazyDirCreation(getNormalizationDataDir(releaseName, projectKey)));
  }

  public void writeNormalizationReport(String releaseName, String projectKey, String content) {
    writeFile(
        getNormalizationReportOutputFile(
            releaseName,
            projectKey),
        content);
  }

  @SneakyThrows
  private void writeFile(String file, String content) {
    @Cleanup
    FSDataOutputStream create = fileSystem
        .create(
        new Path(file));
    create.writeBytes(content.toString());
  }

  private String lazyDirCreation(String dir) {
    if (!checkExistence(fileSystem, dir)) {
      log.info("Creating directory '{}'", dir);
      mkdirs(fileSystem, dir);
    }
    return dir;
  }
}
