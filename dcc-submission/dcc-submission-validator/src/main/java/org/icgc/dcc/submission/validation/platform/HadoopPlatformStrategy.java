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
import static com.google.common.collect.Maps.newHashMap;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.MR_PART_FILE_NAME_BASE;
import static org.icgc.dcc.hadoop.util.HadoopConstants.GZIP_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.hadoop.util.HadoopConstants.SNAPPY_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.hadoop.util.HadoopProperties.enableIntermediateMapOutputCompression;
import static org.icgc.dcc.hadoop.util.HadoopProperties.enableJobOutputCompression;
import static org.icgc.dcc.hadoop.util.HadoopProperties.setAvailableCodecs;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

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

@Slf4j
public class HadoopPlatformStrategy extends BasePlatformStrategy {

  private final Map<String, String> hadoopProperties;

  public HadoopPlatformStrategy(
      @NonNull final Map<String, String> hadoopProperties,
      @NonNull final FileSystem fileSystem,
      @NonNull final Path source,
      @NonNull final Path output,
      @NonNull final Path system) {
    super(fileSystem, source, output, system);
    this.hadoopProperties = hadoopProperties;
  }

  @Override
  public FlowConnector getFlowConnector(Map<Object, Object> properties) {
    Map<Object, Object> flowProperties = newHashMap();

    // Custom serialization
    TupleSerializationProps.addSerialization(flowProperties, TupleStateSerialization.class.getName());

    // From external application configuration file
    flowProperties.putAll(hadoopProperties);

    // M/R job entry point
    AppProps.setApplicationJarClass(flowProperties, this.getClass());

    flowProperties =
        enableJobOutputCompression(
            enableIntermediateMapOutputCompression(
                setAvailableCodecs(flowProperties),
                SNAPPY_CODEC_PROPERTY_VALUE),
            GZIP_CODEC_PROPERTY_VALUE);

    flowProperties.putAll(properties);
    return new HadoopFlowConnector(flowProperties);
  }

  @Override
  public Tap<?, ?, ?> getReportTap(String fileName, FlowType type, String reportName) {
    val reportPath = getReportPath(fileName, type, reportName);
    log.info("Streaming through report: '{}'", reportPath);
    val scheme = new HadoopJsonScheme();
    scheme.setSinkCompression(ENABLE);
    return new Hfs(scheme, reportPath.toUri().getPath());
  }

  @Override
  @SneakyThrows
  public InputStream readReportTap(String fileName, FlowType type, String reportName) {
    val reportPath = getReportPath(fileName, type, reportName);
    if (fileSystem.isFile(reportPath)) {
      return getInputStream(reportPath);
    }

    val inputSuppliers = new ArrayList<InputSupplier<InputStream>>();
    for (val fileStatus : fileSystem.listStatus(reportPath)) {
      val filePath = fileStatus.getPath();

      if (fileStatus.isFile() && filePath.getName().startsWith(MR_PART_FILE_NAME_BASE)) {
        InputSupplier<InputStream> inputSupplier = new InputSupplier<InputStream>() {

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
  public Tap<?, ?, ?> getSourceTap(String fileName) {
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

}
