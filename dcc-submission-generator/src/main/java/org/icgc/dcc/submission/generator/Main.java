/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.generator;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Strings.padEnd;
import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.readLines;
import static java.lang.System.err;
import static java.lang.System.out;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.icgc.dcc.common.core.dcc.Versions.getScmInfo;
import static org.icgc.dcc.submission.generator.service.GeneratorService.generatorService;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Map.Entry;

import org.icgc.dcc.submission.generator.cli.Options;
import org.icgc.dcc.submission.generator.config.GeneratorConfig;
import org.icgc.dcc.submission.generator.config.GeneratorConfigFile;
import org.icgc.dcc.submission.generator.utils.CodeLists;
import org.icgc.dcc.submission.generator.utils.FileSchemas;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Command line utility used to generate ICGC data sets.
 */
@Slf4j
public class Main {

  private final Options options = new Options();

  public static void main(String... args) {
    new Main().run(args);
  }

  private void run(String... args) {
    val cli = new JCommander(options);
    cli.setProgramName(getProgramName());

    try {
      cli.parse(args);

      if (options.help) {
        cli.usage();

        return;
      } else if (options.version) {
        out.printf("ICGC DCC Data Generator%nVersion %s%n", getVersion());

        return;
      }

      val config = read(options.config);
      logBanner(args, config);
      generate(config, options.dictionary, options.codeList);
    } catch (ParameterException pe) {
      err.printf("dcc-generator: %s%n", pe.getMessage());
      err.printf("Try '%s --help' for more information.%n", getProgramName());
    }
  }

  @SneakyThrows
  private void logBanner(String[] args, GeneratorConfig config) {
    log.info("{}", repeat("-", 100));
    for (String line : readLines(getResource("banner.txt"), UTF_8)) {
      log.info(line);
    }
    log.info("{}", repeat("-", 100));
    log.info("Version: {}", getVersion());
    log.info("Built:   {}", getBuildTimestamp());
    log.info("SCM:");
    for (Entry<String, String> entry : getScmInfo().entrySet()) {
      String key = entry.getKey();
      String value = firstNonNull(entry.getValue(), "").replaceAll("\n", " ");

      log.info("         {}: {}", padEnd(key, 24, ' '), value);
    }

    log.info("Command: {}", formatArguments(args));
    log.info("Options: dictionary  - {}", options.dictionary);
    log.info("         codeList    - {}", options.codeList);
    log.info("         config file - {}", options.config.getAbsolutePath());
    log.info("Config:  {}", config);
  }

  private GeneratorConfig read(File file) {
    return new GeneratorConfigFile(file).read();
  }

  private void generate(GeneratorConfig config, File dictionary, File codeList) {
    log.info("Generating using: {}", options);
    val builder = generatorService()
        .outputDirectory(config.outputDirectory())
        .leadJurisdiction(config.leadJurisdiction())
        .numberOfDonors(config.numberOfDonors())
        .numberOfSpecimensPerDonor(config.numberOfSpecimensPerDonor())
        .numberOfSamplesPerSpecimen(config.numberOfSamplesPerSpecimen())
        .tumourType(config.tumourType())
        .institution(config.institution())
        .platform(config.platform())
        .seed(config.seed())
        .experimentalFiles(config.experimentalFiles())
        .optionalFiles(config.optionalFiles());

    if (codeList != null) {
      builder.codeLists(new CodeLists(codeList));
    }
    if (dictionary != null) {
      builder.fileSchemas(new FileSchemas(dictionary));
    }

    val service = builder.build();

    log.info("Generating: config = {}", config);
    service.generateFiles();
    log.info("Finished generating");
  }

  private String getProgramName() {
    return "java -jar " + getJarName();
  }

  private String getVersion() {
    String version = getClass().getPackage().getImplementationVersion();
    return version == null ? "[unknown version]" : version;
  }

  private String getBuildTimestamp() {
    String buildTimestamp = getClass().getPackage().getSpecificationVersion();
    return buildTimestamp == null ? "[unknown build timestamp]" : buildTimestamp;
  }

  private String getJarName() {
    String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    File jarFile = new File(jarPath);

    return jarFile.getName();
  }

  private String formatArguments(String[] args) {
    RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
    List<String> inputArguments = runtime.getInputArguments();

    return "java " + join(inputArguments, ' ') + " -jar " + getJarName() + " " + join(args, ' ');
  }

}
