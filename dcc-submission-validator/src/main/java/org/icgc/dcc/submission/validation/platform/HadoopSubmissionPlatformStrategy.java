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

import static com.google.common.collect.Maps.newHashMap;
import static org.icgc.dcc.common.core.util.Maps2.toObjectsMap;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.getInputStream;
import static org.icgc.dcc.common.hadoop.util.HadoopConstants.GZIP_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.common.hadoop.util.HadoopConstants.SNAPPY_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.common.hadoop.util.HadoopProperties.enableIntermediateMapOutputCompression;
import static org.icgc.dcc.common.hadoop.util.HadoopProperties.enableJobOutputCompression;
import static org.icgc.dcc.common.hadoop.util.HadoopProperties.setAvailableCodecs;

import java.io.InputStream;
import java.util.Map;

import lombok.NonNull;
import lombok.SneakyThrows;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.hadoop.cascading.CascadingContext;
import org.icgc.dcc.submission.validation.cascading.TupleStateSerialization;
import org.icgc.dcc.submission.validation.primary.core.FlowType;

import cascading.property.AppProps;
import cascading.tuple.hadoop.TupleSerializationProps;

public class HadoopSubmissionPlatformStrategy extends BaseSubmissionPlatformStrategy {

  public HadoopSubmissionPlatformStrategy(
      @NonNull final Map<String, String> hadoopProperties,
      @NonNull final FileSystem fileSystem,
      @NonNull final Path source,
      @NonNull final Path output) {
    super(hadoopProperties, fileSystem, source, output);
  }

  @Override
  protected CascadingContext getCascadingContext() {
    return CascadingContext.getDistributed();
  }

  @Override
  protected Map<?, ?> augmentFlowProperties(@NonNull final Map<?, ?> flowProperties) {
    Map<Object, Object> additionalFlowProperties = newHashMap();

    // Custom serialization
    TupleSerializationProps.addSerialization(additionalFlowProperties, TupleStateSerialization.class.getName());

    // M/R job entry point
    AppProps.setApplicationJarClass(additionalFlowProperties, this.getClass());

    additionalFlowProperties =
        enableJobOutputCompression(
            enableIntermediateMapOutputCompression(
                setAvailableCodecs(additionalFlowProperties),
                SNAPPY_CODEC_PROPERTY_VALUE),
            GZIP_CODEC_PROPERTY_VALUE);

    toObjectsMap(flowProperties)
        .putAll(additionalFlowProperties);

    return flowProperties;
  }

  @Override
  @SneakyThrows
  public InputStream readReportTap(String fileName, FlowType type, String reportName) {
    return getInputStream(
        fileSystem,
        getReportPath(fileName, type, reportName));
  }

}
