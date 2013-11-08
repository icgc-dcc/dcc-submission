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

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tap.local.FileTap;

/**
 * Very low-tech replacement for {@link DccFileSystem}, as discussed with @Bob Tiernay around 13/11/07 (see DCC-1876).
 * This is a temporary solution until a proper re-modelling of the file operations related objects can happen.
 * <p>
 * Requirements:<br/>
 * - Junjun's tool to re-write specimen file<br/>
 * 
 */
@RequiredArgsConstructor
public class DccFileSystem2 {

  private final FileSystem fileSystem;

  private final boolean hadoopMode;

  public Tap<?, ?, ?> getNormalizationDataOutputTap(String releaseName, String projectKey) {
    String path = getNormalizationDataOutput(releaseName, projectKey);
    return getTap(path);
  }

  public Tap<?, ?, ?> getNormalizationReportOutputTap(String releaseName, String projectKey) {
    String path = getNormalizationReportOutput(releaseName, projectKey);
    return getTap(path);
  }

  public String getNormalizationDataOutput(String releaseName, String projectKey) {
    return String.format("/icgc/%s/projects/%s/normalization/data/ssm__p.txt", releaseName, projectKey);
  }

  private Tap<?, ?, ?> getTap(String path) {
    return hadoopMode ?
        new Hfs(new cascading.scheme.hadoop.TextDelimited(true, "\t"), path) :
        new FileTap(new cascading.scheme.local.TextDelimited(true, "\t"), path);
  }

  public void writeNormalizationReport(String releaseName, String projectKey, String content) {
    writeFile(
        getNormalizationReportOutput(
            releaseName,
            projectKey),
        content);
  }

  private String getNormalizationReportOutput(String releaseName, String projectKey) {
    return String.format("/icgc/%s/projects/%s/normalization/reports/filtering.txt", releaseName, projectKey);
  }

  @SneakyThrows
  private void writeFile(String file, String content) {
    @Cleanup
    FSDataOutputStream create = fileSystem
        .create(
        new Path(file));
    create.writeUTF(content);
  }
}
