/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.validation.cascading.HadoopJsonScheme;
import org.icgc.dcc.validation.cascading.TupleStateSerialization;
import org.icgc.dcc.validation.cascading.ValidationFields;

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
import com.google.common.collect.Maps;
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

  @Override
  public FlowConnector getFlowConnector() {
    Map<Object, Object> properties = Maps.newHashMap();
    TupleSerializationProps.addSerialization(properties, TupleStateSerialization.class.getName());
    for(Map.Entry<String, ConfigValue> configEntry : hadoopConfig.entrySet()) {
      properties.put(configEntry.getKey(), configEntry.getValue().unwrapped());
    }
    properties
        .put(
            "io.compression.codecs",
            "org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,com.hadoop.compression.lzo.LzoCodec,com.hadoop.compression.lzo.LzopCodec,org.apache.hadoop.io.compress.BZip2Codec");
    properties.put("io.compression.codec.lzo.class", "com.hadoop.compression.lzo.LzoCodec");
    properties.put("mapred.compress.map.output", true);
    properties.put("mapred.map.output.compression.codec", "org.apache.hadoop.io.compress.SnappyCodec");
    properties.put("mapred.output.compression.type", "BLOCK");
    AppProps.setApplicationJarClass(properties, org.icgc.dcc.Main.class);
    return new HadoopFlowConnector(properties);
  }

  @Override
  public Tap<?, ?, ?> getReportTap(FileSchema schema, FlowType type, String reportName) {
    Path path = reportPath(schema, type, reportName);
    return new Hfs(new HadoopJsonScheme(), path.toUri().getPath());
  }

  @Override
  public InputStream readReportTap(FileSchema schema, FlowType type, String reportName) throws IOException {
    Path reportPath = reportPath(schema, type, reportName);
    if(fileSystem.isFile(reportPath)) {
      return fileSystem.open(reportPath);
    }

    List<InputSupplier<InputStream>> inputSuppliers = new ArrayList<InputSupplier<InputStream>>();
    for(FileStatus fileStatus : fileSystem.listStatus(reportPath)) {
      final Path filePath = fileStatus.getPath();
      if(fileStatus.isFile() && filePath.getName().startsWith("part-")) {
        InputSupplier<InputStream> inputSupplier = new InputSupplier<InputStream>() {
          @Override
          public InputStream getInput() throws IOException {
            return fileSystem.open(filePath);
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

  @Override
  public Fields getFileHeader(FileSchema schema) throws IOException {
    Path path = this.path(schema);

    InputStreamReader isr = null;
    Configuration conf = this.fileSystem.getConf();
    CompressionCodecFactory factory = new CompressionCodecFactory(conf);
    try {
      Path resolvedPath = FileContext.getFileContext(fileSystem.getUri()).resolvePath(path);
      CompressionCodec codec = factory.getCodec(resolvedPath);

      isr =
          (codec == null) ? new InputStreamReader(fileSystem.open(resolvedPath), Charsets.UTF_8) : new InputStreamReader(
              codec.createInputStream(fileSystem.open(resolvedPath)), Charsets.UTF_8);

      LineReader lineReader = new LineReader(isr);
      String firstLine = lineReader.readLine();
      Iterable<String> header = Splitter.on('\t').split(firstLine);
      List<String> dupHeader = this.checkDuplicateHeader(header);
      if(!dupHeader.isEmpty()) {
        throw new DuplicateHeaderException(dupHeader);
      }
      return new Fields(Iterables.toArray(header, String.class));
    } finally {
      Closeables.closeQuietly(isr);
    }
  }
}
