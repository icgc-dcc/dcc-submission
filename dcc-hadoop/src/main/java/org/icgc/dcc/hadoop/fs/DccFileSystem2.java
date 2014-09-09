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
import static java.util.regex.Pattern.compile;
import static org.icgc.dcc.core.model.FileTypes.FileType.SGV_P_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SSM_P_TYPE;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.checkExistence;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.mkdirs;

import java.io.File;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.hadoop.cascading.CascadingContext;

import cascading.tap.Tap;

/**
 * Very basic replacement for {@link DccFileSystem}, as discussed with @Bob Tiernay around 13/11/07 (see DCC-1876). This
 * is a temporary solution until a proper re-modelling of the file operations related objects can happen.
 */
@RequiredArgsConstructor
@Slf4j
public class DccFileSystem2 {

  private final FileSystem fileSystem;
  private final String rootDir;
  private final boolean hadoopMode;

  private CascadingContext getCascadingContext() {
    return hadoopMode ?
        CascadingContext.getDistributed() :
        CascadingContext.getLocal();
  }

  public Tap<?, ?, ?> getNormalizationDataOutputTap(String path) {
    return getCascadingContext()
        .getTaps()
        .getNoCompressionTsvWithHeader(path);
  }

  /**
   * Temporarily under .validation so that ReleaseFileSystem#resetValidationFolder() can reset it as well (TODO: address
   * in DCC-1876)
   */
  private String getNormalizationDir(String releaseName, String projectKey) {
    return format("%s/%s/%s/.validation/normalization", rootDir, releaseName, projectKey); // TODO: refactor process
                                                                                           // name
  }

  /**
   * TODO: DCC-1876 - no need to actually nest it under the .validation
   */
  private String getAnnotationDir(String releaseName, String projectKey) {
    return format("%s/%s/%s/.validation/annotation", rootDir, releaseName, projectKey); // TODO: refactor process name
  }

  public String getSubmissionDataDir(String releaseName, String projectKey) {
    return format("%s/%s/%s", rootDir, releaseName, projectKey);
  }

  public String getNormalizationDataDir(String releaseName, String projectKey) {
    return format("%s/data", getNormalizationDir(releaseName, projectKey));
  }

  public String getAnnotationDataDir(String releaseName, String projectKey) {
    return format("%s/data", getAnnotationDir(releaseName, projectKey));
  }

  private String getNormalizationReportsDir(String releaseName, String projectKey) {
    return format("%s/reports", getNormalizationDir(releaseName, projectKey));
  }

  private String getNormalizationReportOutputFile(String releaseName, String projectKey) {
    return format("%s/summary.txt",
        lazyDirCreation(getNormalizationReportsDir(releaseName, projectKey)));
  }

  public String getNormalizationSsmDataOutputFile(String releaseName, String projectKey) {
    return getNormalizationDataOutputFile(releaseName, projectKey, SSM_P_TYPE);
  }

  public String getNormalizationSgvDataOutputFile(String releaseName, String projectKey) {
    return getNormalizationDataOutputFile(releaseName, projectKey, SGV_P_TYPE);
  }

  private String getNormalizationDataOutputFile(String releaseName, String projectKey, FileType fileType) {
    return format(
        "%s/%s",
        lazyDirCreation(getNormalizationDataDir(releaseName, projectKey)),
        fileType.getHarmonizedOutputFileName());
  }

  // TODO: move to a NormalizerDccFileSystem2-like class?
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

  /**
   * Determines whether or not the name of the given file - including its parent directory(ies) - matches the given
   * pattern.
   * <p>
   * For instance: /path/to/myfile.txt is a match for ".+\.txt".
   */
  public static boolean matches(String file, String filePattern) {
    return compile(filePattern)
        .matcher(new File(file).getName())
        .matches();
  }
}
