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
package org.icgc.dcc.submission.validation.platform;

import static cascading.scheme.hadoop.TextLine.Compress.ENABLE;
import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Maps.newHashMap;
import static org.icgc.dcc.hadoop.util.HadoopConstants.BZIP2_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.hadoop.util.HadoopConstants.DEFAULT_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.hadoop.util.HadoopConstants.ENABLED_COMPRESSION;
import static org.icgc.dcc.hadoop.util.HadoopConstants.GZIP_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.hadoop.util.HadoopConstants.IO_COMPRESSION_CODECS_PROPERTY_NAME;
import static org.icgc.dcc.hadoop.util.HadoopConstants.MAPRED_COMPRESSION_MAP_OUTPUT_PROPERTY_NAME;
import static org.icgc.dcc.hadoop.util.HadoopConstants.MAPRED_MAP_OUTPUT_COMPRESSION_CODEC_PROPERTY_NAME;
import static org.icgc.dcc.hadoop.util.HadoopConstants.MAPRED_OUTPUT_COMPRESSION_CODE_PROPERTY_NAME;
import static org.icgc.dcc.hadoop.util.HadoopConstants.MAPRED_OUTPUT_COMPRESSION_TYPE_PROPERTY_BLOCK_VALUE;
import static org.icgc.dcc.hadoop.util.HadoopConstants.MAPRED_OUTPUT_COMPRESSION_TYPE_PROPERTY_NAME;
import static org.icgc.dcc.hadoop.util.HadoopConstants.MAPRED_OUTPUT_COMPRESS_PROPERTY_NAME;
import static org.icgc.dcc.hadoop.util.HadoopConstants.PROPERTY_VALUES_SEPARATOR;
import static org.icgc.dcc.hadoop.util.HadoopConstants.SNAPPY_CODEC_PROPERTY_VALUE;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.icgc.dcc.submission.validation.cascading.HadoopJsonScheme;
import org.icgc.dcc.submission.validation.cascading.TupleStateSerialization;
import org.icgc.dcc.submission.validation.cascading.ValidationFields;
import org.icgc.dcc.submission.validation.primary.core.FlowType;

import cascading.flow.FlowConnector;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.property.AppProps;
import cascading.scheme.hadoop.TextDelimited;
import cascading.scheme.hadoop.TextLine;
import cascading.scheme.hadoop.TextLine.Compress;
import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tuple.Fields;
import cascading.tuple.hadoop.TupleSerializationProps;

import com.google.common.io.ByteStreams;
import com.google.common.io.InputSupplier;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

public class HadoopPlatformStrategy extends BasePlatformStrategy {

  /**
   * Prefix used with Hadoop M/R part files.
   */
  private static final String PART_FILE_NAME_PREFIX = "part-";

  private final Config hadoopConfig;

  public HadoopPlatformStrategy(Config hadoopConfig, FileSystem fileSystem, Path source, Path output, Path system) {
    super(fileSystem, source, output, system);
    this.hadoopConfig = hadoopConfig;
  }

  @Override
  public FlowConnector getFlowConnector() {
    Map<Object, Object> flowProperties = newHashMap();

    // Custom serialization
    TupleSerializationProps.addSerialization(flowProperties, TupleStateSerialization.class.getName());

    // From external application configuration file
    for (Map.Entry<String, ConfigValue> configEntry : hadoopConfig.entrySet()) {
      flowProperties.put(configEntry.getKey(), configEntry.getValue().unwrapped());
    }

    // M/R job entry point
    AppProps.setApplicationJarClass(flowProperties, this.getClass());

    if (isProduction()) {
      // Specify available compression codecs
      flowProperties.put(IO_COMPRESSION_CODECS_PROPERTY_NAME,
          on(PROPERTY_VALUES_SEPARATOR)
              .join(
                  DEFAULT_CODEC_PROPERTY_VALUE,
                  GZIP_CODEC_PROPERTY_VALUE,
                  BZIP2_CODEC_PROPERTY_VALUE));

      // Enable compression on intermediate map outputs
      flowProperties.put(
          MAPRED_COMPRESSION_MAP_OUTPUT_PROPERTY_NAME,
          ENABLED_COMPRESSION);
      flowProperties.put(
          MAPRED_OUTPUT_COMPRESSION_TYPE_PROPERTY_NAME,
          MAPRED_OUTPUT_COMPRESSION_TYPE_PROPERTY_BLOCK_VALUE);
      flowProperties.put(
          MAPRED_MAP_OUTPUT_COMPRESSION_CODEC_PROPERTY_NAME,
          SNAPPY_CODEC_PROPERTY_VALUE);

      // Enable compression on job outputs
      flowProperties.put(
          MAPRED_OUTPUT_COMPRESS_PROPERTY_NAME,
          ENABLED_COMPRESSION);
      flowProperties.put(
          MAPRED_OUTPUT_COMPRESSION_CODE_PROPERTY_NAME,
          GZIP_CODEC_PROPERTY_VALUE);
    }

    return new HadoopFlowConnector(flowProperties);
  }

  @Override
  public Tap<?, ?, ?> getReportTap2(String fileName, FlowType type, String reportName) {
    val reportPath = reportPath2(fileName, type, reportName);
    val scheme = new HadoopJsonScheme();
    scheme.setSinkCompression(ENABLE);
    return new Hfs(scheme, reportPath.toUri().getPath());
  }

  @Override
  public InputStream readReportTap2(String fileName, FlowType type, String reportName)
      throws IOException {
    val reportPath = reportPath2(fileName, type, reportName);
    if (fileSystem.isFile(reportPath)) {
      return getInputStream(reportPath);
    }

    val inputSuppliers = new ArrayList<InputSupplier<InputStream>>();
    for (val fileStatus : fileSystem.listStatus(reportPath)) {
      val filePath = fileStatus.getPath();

      if (fileStatus.isFile() && filePath.getName().startsWith(PART_FILE_NAME_PREFIX)) {
        val inputSupplier = new InputSupplier<InputStream>() {

          @Override
          public InputStream getInput() throws IOException {
            return getInputStream(filePath);
          }
        };

        inputSuppliers.add(inputSupplier);
      }
    }

    val combinedInputStream = ByteStreams.join(inputSuppliers).getInput();

    return combinedInputStream;
  }

  @Override
  protected Tap<?, ?, ?> tap(Path path) {
    val textDelimited = new TextDelimited(true, FIELD_SEPARATOR);
    textDelimited.setSinkCompression(Compress.ENABLE);

    return new Hfs(textDelimited, path.toUri().getPath());
  }

  @Override
  protected Tap<?, ?, ?> tap(Path path, Fields fields) {
    val textDelimited = new TextDelimited(fields, true, FIELD_SEPARATOR);
    textDelimited.setSinkCompression(Compress.ENABLE);

    return new Hfs(textDelimited, path.toUri().getPath());
  }

  @Override
  public Tap<?, ?, ?> getInputTap(String fileName) {
    val scheme = new TextLine(
        new Fields(ValidationFields.OFFSET_FIELD_NAME,
            "line"));
    scheme.setSinkCompression(Compress.ENABLE);
    return new Hfs(
        scheme,
        getFilePath(fileName).toUri().getPath());
  }

  /**
   * Temporary: see DCC-1876
   */
  @Override
  protected Tap<?, ?, ?> tapSource2(Path path) {
    val scheme = new TextDelimited(
        true, // headers
        FIELD_SEPARATOR);
    scheme.setSinkCompression(Compress.ENABLE);
    return new Hfs(
        scheme,
        path.toUri().getPath());
  }

  @SneakyThrows
  private InputStream getInputStream(Path path) {
    val factory = new CompressionCodecFactory(fileSystem.getConf());
    val resolvedPath = FileContext.getFileContext(fileSystem.getUri()).resolvePath(path);
    val codec = factory.getCodec(path);
    val inputStream = fileSystem.open(resolvedPath);

    return codec == null ? inputStream : codec.createInputStream(inputStream);
  }

  /**
   * Simple flag to avoid configuring hadoop properties that will not work when running in pseudo-distributed mode due
   * to the lack of native libraries.
   * 
   * @see https://groups.google.com/a/cloudera.org/forum/#!topic/cdh-user/oBhz-XbuSNI
   */
  private static boolean isProduction() {
    // See SubmissionIntegrationTest#setUp
    return System.getProperty("dcc.hadoop.test") == null;
  }
}
