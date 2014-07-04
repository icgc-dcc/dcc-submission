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
package org.icgc.dcc.submission.validation.norm.cli;

import static com.google.common.base.Preconditions.checkState;
import static com.typesafe.config.ConfigFactory.parseMap;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.SSM_TYPE;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.hadoop.fs.DccFileSystem2;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.norm.NormalizationValidator;
import org.icgc.dcc.submission.validation.norm.PseudoNormalizer;

import com.google.common.collect.ImmutableMap;
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

  public static void main(String... args) throws InterruptedException {
    log.info("Starting normalization...");

    // Resolve configuration @formatter:off
    int i = 0;
    val parentInputDirName = args.length >= ++i ? args[i - 1] : "1_concatenator";
    val projectKey   = args.length >= ++i ? args[i - 1] : "project.1";
    val overarchDirName       = args.length >= ++i ? args[i - 1] : "/.../intermediate"; // TODO: make first
    val fsUrl        = args.length >= ++i ? args[i - 1] : "file:///"; // or like "hdfs://hname-dev.res:8020"
    val jobTracker   = args.length >= ++i ? args[i - 1] : "localhost"; // or like "hcn51.res:8021"
    val fileTypeName = args.length >= ++i ? args[i - 1] : SSM_TYPE.getTypeName(); // or "sgv"
    // @formatter:on

    val fileType = FeatureType.from(fileTypeName);
    checkState(fileType.isSimple());

    // Execute
    val context = getValidationContext(overarchDirName, parentInputDirName, projectKey, fsUrl, jobTracker);
    if (fileType.isSsm()) {
      val validator = getValidator(context);
      validator.validate(context);
    } else {
      new PseudoNormalizer(context.getFileSystem(), "", "").process();
    }

    log.info("Finished normalization.");
  }

  private static ValidationContext getValidationContext(String overarchDirName, String parentInputDirName,
      String projectKey, String fsUrl, String jobTracker) {
    // @formatter:off
    log.info("overarchDirName:    {}", overarchDirName);
    log.info("parentInputDirName: {}", parentInputDirName);
    log.info("projectKey:  {}", projectKey);
    log.info("fsUrl:       {}", fsUrl);
    log.info("jobTracker:  {}", jobTracker);
    log.info("input:       {}", overarchDirName + "/" + parentInputDirName + "/" + projectKey + "/" + "{ssmP}");
    log.info("output:      {}", overarchDirName + "/" + parentInputDirName + "/" + projectKey + "/" + ".validation/normalization");
    // @formatter:on

    return new NomalizationValidationContext(overarchDirName, parentInputDirName, projectKey, fsUrl, jobTracker);
  }

  private static NormalizationValidator getValidator(ValidationContext context) {
    return NormalizationValidator.getDefaultInstance(
        getDccFileSystem2(context),
        getNormalizationConfig());
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

}
