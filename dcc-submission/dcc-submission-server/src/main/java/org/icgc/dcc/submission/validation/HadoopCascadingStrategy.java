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
package org.icgc.dcc.submission.validation;

import static cascading.scheme.hadoop.TextLine.Compress.ENABLE;
import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Maps.newHashMap;
import static org.icgc.dcc.core.util.HadoopConstants.BZIP2_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.core.util.HadoopConstants.COMPRESSION_MAP_OUTPUT_PROPERTY_TRUE_VALUE;
import static org.icgc.dcc.core.util.HadoopConstants.DEFAULT_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.core.util.HadoopConstants.GZIP_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.core.util.HadoopConstants.IO_COMPRESSION_CODECS_PROPERTY_NAME;
import static org.icgc.dcc.core.util.HadoopConstants.MAPRED_COMPRESSION_MAP_OUTPUT_PROPERTY_NAME;
import static org.icgc.dcc.core.util.HadoopConstants.MAPRED_MAP_OUTPUT_COMPRESSION_CODEC_PROPERTY_NAME;
import static org.icgc.dcc.core.util.HadoopConstants.MAPRED_OUTPUT_COMPRESSION_CODE_PROPERTY_NAME;
import static org.icgc.dcc.core.util.HadoopConstants.MAPRED_OUTPUT_COMPRESSION_TYPE_PROPERTY_BLOCK_VALUE;
import static org.icgc.dcc.core.util.HadoopConstants.MAPRED_OUTPUT_COMPRESSION_TYPE_PROPERTY_NAME;
import static org.icgc.dcc.core.util.HadoopConstants.SNAPPY_CODEC_PROPERTY_VALUE;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.cascading.HadoopJsonScheme;
import org.icgc.dcc.submission.validation.cascading.TupleStateSerialization;
import org.icgc.dcc.submission.validation.cascading.ValidationFields;

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

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.InputSupplier;
import com.google.common.io.LineReader;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

public class HadoopCascadingStrategy extends BaseCascadingStrategy {

  private final Config hadoopConfig;

  public HadoopCascadingStrategy(Config hadoopConfig, FileSystem fileSystem, Path source, Path output, Path system) {
    super(fileSystem, source, output, system);
    this.hadoopConfig = hadoopConfig;
  }

  /**
   * TODO: bring together with the loader's counterpart (create a dcc-hadoop module).
   */
  @Override
  public FlowConnector getFlowConnector() {
    Map<Object, Object> flowProperties = newHashMap();

    // Custom serialization
    TupleSerializationProps.addSerialization(flowProperties, TupleStateSerialization.class.getName());

    // From external application configuration file
    for (Map.Entry<String, ConfigValue> configEntry : hadoopConfig.entrySet()) {
      flowProperties.put(configEntry.getKey(), configEntry.getValue().unwrapped());
    }

    // Specify available compression codecs
    flowProperties.put(IO_COMPRESSION_CODECS_PROPERTY_NAME, on(',').join(
        DEFAULT_CODEC_PROPERTY_VALUE,
        GZIP_CODEC_PROPERTY_VALUE,
        BZIP2_CODEC_PROPERTY_VALUE));

    // Enable compression on intermediate map outputs
    flowProperties.put(
        MAPRED_COMPRESSION_MAP_OUTPUT_PROPERTY_NAME,
        COMPRESSION_MAP_OUTPUT_PROPERTY_TRUE_VALUE);
    flowProperties.put(
        MAPRED_OUTPUT_COMPRESSION_TYPE_PROPERTY_NAME,
        MAPRED_OUTPUT_COMPRESSION_TYPE_PROPERTY_BLOCK_VALUE);
    flowProperties.put(
        MAPRED_MAP_OUTPUT_COMPRESSION_CODEC_PROPERTY_NAME,
        SNAPPY_CODEC_PROPERTY_VALUE);

    // Enable compression on job outputs
    // TODO: add mapred.output.compress=true as well?
    flowProperties.put(
        MAPRED_OUTPUT_COMPRESSION_CODE_PROPERTY_NAME,
        GZIP_CODEC_PROPERTY_VALUE);

    // M/R job entry point
    AppProps.setApplicationJarClass(flowProperties, org.icgc.dcc.submission.Main.class);

    return new HadoopFlowConnector(flowProperties);
  }

  @Override
  public Tap<?, ?, ?> getReportTap(FileSchema schema, FlowType type, String reportName) {
    Path path = reportPath(schema, type, reportName);
    HadoopJsonScheme scheme = new HadoopJsonScheme();
    scheme.setSinkCompression(ENABLE);
    return new Hfs(scheme, path.toUri().getPath());
  }

  @Override
  public InputStream readReportTap(FileSchema schema, FlowType type, String reportName) throws IOException {
    Path reportPath = reportPath(schema, type, reportName);
    if (fileSystem.isFile(reportPath)) {
      return new GZIPInputStream(fileSystem.open(reportPath));
    }

    List<InputSupplier<InputStream>> inputSuppliers = new ArrayList<InputSupplier<InputStream>>();
    for (FileStatus fileStatus : fileSystem.listStatus(reportPath)) {
      final Path filePath = fileStatus.getPath();

      if (fileStatus.isFile() && filePath.getName().startsWith("part-")) {
        InputSupplier<InputStream> inputSupplier = new InputSupplier<InputStream>() {

          @Override
          public InputStream getInput() throws IOException {
            return new GZIPInputStream(fileSystem.open(filePath));
          }
        };

        inputSuppliers.add(inputSupplier);
      }
    }

    return ByteStreams.join(inputSuppliers).getInput();
  }

  @Override
  protected Tap<?, ?, ?> tap(Path path) {
    TextDelimited textDelimited = new TextDelimited(true, "\t");
    textDelimited.setSinkCompression(Compress.ENABLE);
    return new Hfs(textDelimited, path.toUri().getPath());
  }

  @Override
  protected Tap<?, ?, ?> tap(Path path, Fields fields) {
    TextDelimited textDelimited = new TextDelimited(fields, true, "\t");
    textDelimited.setSinkCompression(Compress.ENABLE);
    return new Hfs(textDelimited, path.toUri().getPath());
  }

  @Override
  protected Tap<?, ?, ?> tapSource(Path path) {
    TextLine textLine = new TextLine(new Fields(ValidationFields.OFFSET_FIELD_NAME, "line"));
    textLine.setSinkCompression(Compress.ENABLE);
    return new Hfs(textLine, path.toUri().getPath());
  }

  /**
   * TODO: try and combine with validator's equivalent. (DCC-996)
   */
  @Override
  public Fields getFileHeader(FileSchema fileSchema) throws IOException {
    Path path = this.path(fileSchema);

    InputStreamReader isr = null;
    Configuration conf = this.fileSystem.getConf();
    CompressionCodecFactory factory = new CompressionCodecFactory(conf);
    try {
      Path resolvedPath = FileContext.getFileContext(fileSystem.getUri()).resolvePath(path);
      CompressionCodec codec = factory.getCodec(resolvedPath);

      isr = (codec == null) ? //
      new InputStreamReader(fileSystem.open(resolvedPath), Charsets.UTF_8) : //
      new InputStreamReader(codec.createInputStream(fileSystem.open(resolvedPath)), Charsets.UTF_8);

      LineReader lineReader = new LineReader(isr);
      String firstLine = lineReader.readLine();
      Iterable<String> header = Splitter.on('\t').split(firstLine);
      List<String> dupHeader = this.checkDuplicateHeader(header);
      if (!dupHeader.isEmpty()) {
        throw new DuplicateHeaderException(dupHeader);
      }
      return new Fields(Iterables.toArray(header, String.class));
    } finally {
      Closeables.closeQuietly(isr);
    }
  }

}
