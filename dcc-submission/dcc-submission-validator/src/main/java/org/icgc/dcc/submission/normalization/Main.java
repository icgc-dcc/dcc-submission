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
package org.icgc.dcc.submission.normalization;

import static com.google.common.base.Preconditions.checkState;
import static com.typesafe.config.ConfigFactory.parseMap;

import java.util.List;
import java.util.Map;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.hadoop.fs.DccFileSystem2;
import org.icgc.dcc.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.typesafe.config.Config;

/**
 * Command-line utility to initiate normalization on a specified release project's submission stored locally or in HDFS.
 * Will use Cascading local or Hadoop depending on the {@code fsUrl} argument's scheme.
 * <p>
 * Syntax:
 * <p>
 * <code>
 * java -jar dcc-submission-server.jar org.icgc.dcc.submission.normalization.Main [releaseName [projectKey [fsRoot [fsUrl [jobTracker]]]]]
 * </code>
 */
@Slf4j
public class Main {

  private static final Splitter ROW_SPLITTER = Splitter.on('\t');

  public static void main(String... args) throws InterruptedException {
    log.info("Starting normalization...");

    // Resolve configuration @formatter:off
    int i = 0;
    val releaseName = args.length >= ++i ? args[i - 1] : "release2";
    val projectKey  = args.length >= ++i ? args[i - 1] : "project.1";
    val fsRoot      = args.length >= ++i ? args[i - 1] : "/tmp/dcc_root_dir";
    val fsUrl       = args.length >= ++i ? args[i - 1] : "file:///";
    val jobTracker  = args.length >= ++i ? args[i - 1] : "localhost";
    // @formatter:on

    // Execute
    val context = getValidationContext(releaseName, projectKey, fsRoot, fsUrl, jobTracker);
    val validator = getValidator(context);
    validator.validate(context);

    log.info("Finished normalization.");
  }

  private static ValidationContext getValidationContext(String releaseName, String projectKey, String fsRoot,
      String fsUrl,
      String jobTracker) {
    // @formatter:off
    log.info("releaseName: {}", releaseName);
    log.info("projectKey:  {}", projectKey);
    log.info("fsRoot:      {}", fsRoot);
    log.info("fsUrl:       {}", fsUrl);
    log.info("jobTracker:  {}", jobTracker);
    log.info("input:       {}", fsRoot + "/" + releaseName + "/" + projectKey + "/" + "{ssmP}");
    log.info("output:      {}", fsRoot + "/" + releaseName + "/" + projectKey + "/" + ".validation/normalization");
    // @formatter:on

    return new NomalizationValidationContext(releaseName, projectKey, fsRoot, fsUrl, jobTracker);
  }

  private static NormalizationValidator getValidator(ValidationContext context) {
    return NormalizationValidator.getDefaultInstance(
        getDccFileSystem2(context),
        getNormalizationConfig(),
        getSampleToDonorMap(context));
  }

  private static DccFileSystem2 getDccFileSystem2(ValidationContext context) {
    val rootDir = context.getDccFileSystem().getRootStringPath();
    val hdfs = context.getFileSystem().getScheme().equals("hdfs");

    return new DccFileSystem2(context.getFileSystem(), rootDir, hdfs);
  }

  private static Config getNormalizationConfig() {
    return parseMap(ImmutableMap.<String, Object> of(
        "error_threshold", 1,
        "masks.enabled", true,
        "duplicates.enabled", true
        ));
  }

  private static Map<String, String> getSampleToDonorMap(ValidationContext context) {
    val platform = context.getPlatformStrategy();
    val dictionary = context.getDictionary();
    val fileSystem = platform.getFileSystem();

    val sampleToSpecimen = Maps.<String, String> newTreeMap();
    for (String row : getRows(platform, dictionary, fileSystem, SubmissionFileType.SAMPLE_TYPE)) {
      val fields = Lists.<String> newArrayList(ROW_SPLITTER.split(row));
      val specimenId = fields.get(1);
      val sampleId = fields.get(0); // indices are "reversed" in sample (PK is second instead of first like for donor
                                    // and specimen)
      checkState(!sampleToSpecimen.containsKey(sampleId));
      sampleToSpecimen.put(sampleId, specimenId);
    }
    log.info("sample to specimen mapping: {}", sampleToSpecimen);

    val specimenToDonor = Maps.<String, String> newTreeMap();
    for (String row : getRows(platform, dictionary, fileSystem, SubmissionFileType.SPECIMEN_TYPE)) {
      val fields = Lists.<String> newArrayList(ROW_SPLITTER.split(row));
      val donorId = fields.get(0);
      val specimenId = fields.get(1);
      checkState(!specimenToDonor.containsKey(specimenId));
      specimenToDonor.put(specimenId, donorId);
    }
    log.info("specimen to donor mapping: {}", specimenToDonor);

    val sampleToDonor = Maps.<String, String> newTreeMap();
    for (val entry : sampleToSpecimen.entrySet()) {
      sampleToDonor.put(
          entry.getKey(),
          specimenToDonor.get(entry.getValue()));
    }
    log.info("sample to donor mapping: {}", sampleToDonor);

    return sampleToDonor;
  }

  @SneakyThrows
  private static List<String> getRows(
      PlatformStrategy platform, Dictionary dictionary, FileSystem fileSystem, SubmissionFileType fileType) {
    return HadoopUtils.readSmallTextFile(
        fileSystem,
        platform.path(
            dictionary.getFileSchema(fileType)));
  }

}
