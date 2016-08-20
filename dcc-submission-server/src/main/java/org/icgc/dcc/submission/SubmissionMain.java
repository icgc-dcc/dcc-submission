/*


 * 
 * 
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
package org.icgc.dcc.submission;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.padEnd;
import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.readLines;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.icgc.dcc.common.core.dcc.Versions.getScmInfo;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Map.Entry;

import org.icgc.dcc.common.hadoop.util.HadoopProperties;
import org.icgc.dcc.submission.core.DccRuntime;
import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * SubmissionMain class for the submission system. Starts the DCC Submission System daemon.
 */
@Slf4j
@AllArgsConstructor
public class SubmissionMain {

  static {
    HadoopProperties.setHadoopUserNameProperty();
  }

  /**
   * Application instance.
   */
  private static SubmissionMain instance;

  /**
   * Services handle.
   */
  @NonNull
  private AnnotationConfigApplicationContext context;
  @NonNull
  private DccRuntime dccRuntime;

  /**
   * SubmissionMain method for the submission system.
   */
  public static void main(String... args) {
    val properties = loadProperties(args);
    logBanner(args, properties);

    val context = new AnnotationConfigApplicationContext();
    context.getBeanFactory().registerSingleton("submissionProperties", properties);
    context.scan(getType().getPackage().getName());
    context.refresh();

    val dccRuntime = context.getBean(DccRuntime.class);

    instance = new SubmissionMain(context, dccRuntime);
    instance.registerShutdownHook();
    instance.start();

    log.info("Exiting main method.");
  }

  private void registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

      @Override
      public void run() {
        if (isRunning()) {
          stop();
        }
      }

    }, "Shutdown-thread"));
  }

  @SneakyThrows
  private void start() {
    try {
      dccRuntime.start();
    } catch (Throwable t) {
      log.error("An unknown error was caught starting:", t);
      throw t;
    }
  }

  @SneakyThrows
  private void stop() {
    try {
      dccRuntime.stop();

      // Allow GC to cleanup
      dccRuntime = null;
      instance = null;
    } catch (Throwable t) {
      log.error("An unknown error was caught stopping:", t);
      throw t;
    }
  }

  private boolean isRunning() {
    return dccRuntime != null;
  }

  @SneakyThrows
  private static SubmissionProperties loadProperties(String[] args) {
    checkArgument(args.length >= 2, "The argument 'external' requires a filename as an additional parameter");

    val configFile = new File(args[1]);
    checkArgument(configFile.exists(), "Configuration file '%s' not found", configFile.getAbsolutePath());

    // Overlay overrides to base to get affective configuration
    log.info("Using config file {}", configFile.getAbsoluteFile());
    return readConfig(configFile);
  }

  @SneakyThrows
  private static void logBanner(String[] args, SubmissionProperties properties) {
    log.info("{}", repeat("-", 100));
    for (String line : readLines(getResource("banner.txt"), UTF_8)) {
      log.info(line);
    }
    log.info("{}", repeat("-", 100));
    log.info("Version: {}", getVersion());
    log.info("Built:   {}", getBuildTimestamp());
    log.info("SCM:");
    for (Entry<String, String> entry : getScmInfo().entrySet()) {
      val key = entry.getKey();
      val value = firstNonNull(entry.getValue(), "").replaceAll("\n", " ");

      log.info("         {}: {}", padEnd(key, 24, ' '), value);
    }

    log.info("Command: {}", formatArguments(args));
    log.info("Properties:  {}", properties);
  }

  private static String getVersion() {
    val version = getType().getPackage().getImplementationVersion();
    return version == null ? "[unknown version]" : version;
  }

  private static String getBuildTimestamp() {
    val buildTimestamp = getType().getPackage().getSpecificationVersion();
    return buildTimestamp == null ? "[unknown build timestamp]" : buildTimestamp;
  }

  private static String getJarName() {
    val jarPath = getType().getProtectionDomain().getCodeSource().getLocation().getPath();
    val jarFile = new File(jarPath);

    return jarFile.getName();
  }

  private static Class<SubmissionMain> getType() {
    return SubmissionMain.class;
  }

  private static String formatArguments(String[] args) {
    val runtime = ManagementFactory.getRuntimeMXBean();
    val inputArguments = runtime.getInputArguments();

    return "java " + join(inputArguments, ' ') + " -jar " + getJarName() + " " + join(args, ' ');
  }

  @SneakyThrows
  private static SubmissionProperties readConfig(File configFile) {
    return new ObjectMapper(new YAMLFactory()).readValue(configFile, SubmissionProperties.class);
  }

  /**
   * This method is required for testing since shutdown hooks are not invoked between tests.
   */
  @VisibleForTesting
  public static void shutdown() {
    if (instance != null) instance.stop();
  }

}
