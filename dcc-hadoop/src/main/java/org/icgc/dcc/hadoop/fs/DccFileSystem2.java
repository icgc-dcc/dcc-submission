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

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static org.icgc.dcc.core.model.FileTypes.FileType.SGV_P_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SSM_P_TYPE;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.checkExistence;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.lsFile;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.mkdirs;

import java.io.File;
import java.util.List;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.FileTypes.FileType;

import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tap.local.FileTap;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * Very basic replacement for {@link DccFileSystem}, as discussed with @Bob Tiernay around 13/11/07 (see DCC-1876). This
 * is a temporary solution until a proper re-modelling of the file operations related objects can happen.
 * <p>
 * Requirements:<br/>
 * - Junjun's tool to re-write specimen file<br/>
 */
@RequiredArgsConstructor
@Slf4j
public class DccFileSystem2 {

  private static final String SSM_P_FILE_NAME = "ssm_p.txt";
  private static final String SSM_S_FILE_NAME = "ssm_s.txt";

  private final FileSystem fileSystem;

  private final String rootDir;

  private final boolean hadoopMode;

  public Tap<?, ?, ?> getNormalizationDataOutputTap(String releaseName, String projectKey) {
    String path = getNormalizationSsmDataOutputFile(releaseName, projectKey);
    return getTap(path);
  }

  private Tap<?, ?, ?> getTap(String path) {
    return hadoopMode ?
        new Hfs(new cascading.scheme.hadoop.TextDelimited(true, "\t"), path) :
        new FileTap(new cascading.scheme.local.TextDelimited(true, "\t"), path);
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

  public String getAnnotationDataOutputFile(String releaseName, String projectKey) {
    return format(
        "%s/%s",
        lazyDirCreation(getAnnotationDataDir(releaseName, projectKey)),
        SSM_S_FILE_NAME);
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
   * List relevant files for the loader component, not necessarily all under the same directory.
   */
  public List<String> listLoaderFiles(
      String projectKey, String projectDataPath, String ssmPPattern, String ssmSPattern) {
    val files = Lists.<String> newArrayList();

    // Handle all but ssm_p and ssm_s files (directly from the submission system)
    for (val filePath : lsFile(fileSystem, new Path(projectDataPath))) {
      val file = filePath.toUri().toString();
      if (!matches(file, ssmPPattern) && !matches(file, ssmSPattern)) { // ssm_p and ssm_s are handled separately
        files.add(file);
      }
    }

    // Handle ssm_p (from the normalizer)
    {
      // Temporarily using the same directory until
      // https://wiki.oicr.on.ca/display/DCCSOFT/Incremental+Submission+-+Discussion and
      // https://wiki.oicr.on.ca/display/DCCSOFT/Overarching+Component are fleshed out
      // val normalizationDataOutputFile = getNormalizationDataOutputFile(releaseName, projectKey);
      val normalizationDataOutputFile = Joiner.on('/').join(projectDataPath, SSM_P_FILE_NAME);

      if (checkExistence(fileSystem, normalizationDataOutputFile)) {
        val fileName = new File(normalizationDataOutputFile).getName();
        val matchesSsmPPattern = compile(ssmPPattern).matcher(fileName).matches();
        checkState(matchesSsmPPattern, // By design
            "File '%s' does not match expected pattern: '%s'", ssmPPattern, fileName);
        files.add(normalizationDataOutputFile);
      } else {
        log.info("No ssm_p normalization file found at '{}'", normalizationDataOutputFile);
      }
    }

    // Handle ssm_s (from the annotator)
    {
      // Temporarily using the same directory until
      // https://wiki.oicr.on.ca/display/DCCSOFT/Incremental+Submission+-+Discussion and
      // https://wiki.oicr.on.ca/display/DCCSOFT/Overarching+Component are fleshed out
      // val annotationDataOutputFile = getAnnotationDataOutputFile(releaseName, projectKey);
      val annotationDataOutputFile = Joiner.on('/').join(projectDataPath, SSM_S_FILE_NAME);

      if (checkExistence(fileSystem, annotationDataOutputFile)) {
        val fileName = new File(annotationDataOutputFile).getName();
        val matchesSsmSPattern = compile(ssmSPattern).matcher(fileName).matches();
        checkState(matchesSsmSPattern, // By design
            "File '%s' does not match expected pattern: '%s'", fileName, ssmSPattern);
        files.add(annotationDataOutputFile);
      } else {
        log.info("No ssm_s annotation file found at '{}'", annotationDataOutputFile);
      }
    }

    return files;
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
