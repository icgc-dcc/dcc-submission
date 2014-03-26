/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.hadoop.util;

import static cascading.flow.stream.StreamGraph.DOT_FILE_PATH;
import static cascading.flow.stream.StreamGraph.ERROR_DOT_FILE_NAME;
import static com.google.common.base.Joiner.on;
import static java.lang.String.format;
import static org.icgc.dcc.hadoop.util.HadoopConstants.BZIP2_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.hadoop.util.HadoopConstants.CASCADING_DOT_FILE_PATH;
import static org.icgc.dcc.hadoop.util.HadoopConstants.CASCADING_ERROR_DOT_FILE_NAME;
import static org.icgc.dcc.hadoop.util.HadoopConstants.DEFAULT_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.hadoop.util.HadoopConstants.ENABLED_COMPRESSION;
import static org.icgc.dcc.hadoop.util.HadoopConstants.GZIP_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.hadoop.util.HadoopConstants.IO_COMPRESSION_CODECS_PROPERTY_NAME;
import static org.icgc.dcc.hadoop.util.HadoopConstants.LZOP_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.hadoop.util.HadoopConstants.LZO_CODEC_PROPERTY_VALUE;
import static org.icgc.dcc.hadoop.util.HadoopConstants.MAPRED_COMPRESSION_MAP_OUTPUT_PROPERTY_NAME;
import static org.icgc.dcc.hadoop.util.HadoopConstants.MAPRED_MAP_OUTPUT_COMPRESSION_CODEC_PROPERTY_NAME;
import static org.icgc.dcc.hadoop.util.HadoopConstants.MAPRED_OUTPUT_COMPRESSION_CODE_PROPERTY_NAME;
import static org.icgc.dcc.hadoop.util.HadoopConstants.MAPRED_OUTPUT_COMPRESSION_TYPE_PROPERTY_BLOCK_VALUE;
import static org.icgc.dcc.hadoop.util.HadoopConstants.MAPRED_OUTPUT_COMPRESSION_TYPE_PROPERTY_NAME;
import static org.icgc.dcc.hadoop.util.HadoopConstants.MAPRED_OUTPUT_COMPRESS_PROPERTY_NAME;
import static org.icgc.dcc.hadoop.util.HadoopConstants.PROPERTY_VALUES_SEPARATOR;
import static org.icgc.dcc.hadoop.util.HadoopConstants.SNAPPY_CODEC_PROPERTY_VALUE;

import java.util.Map;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.util.AppUtils;

/**
 * Helper class to set common hadoop properties.
 */
@Slf4j
public class HadoopProperties {

  public static Map<Object, Object> setAvailableCodecs(Map<Object, Object> properties) {
    val codecs = on(PROPERTY_VALUES_SEPARATOR)
        .join(
            DEFAULT_CODEC_PROPERTY_VALUE,
            GZIP_CODEC_PROPERTY_VALUE,
            BZIP2_CODEC_PROPERTY_VALUE);
    properties.put(
        IO_COMPRESSION_CODECS_PROPERTY_NAME,
        AppUtils.isTestEnvironment() ?
            codecs :
            on(PROPERTY_VALUES_SEPARATOR).join(
                codecs,
                LZO_CODEC_PROPERTY_VALUE,
                LZOP_CODEC_PROPERTY_VALUE));
    log.info(getLogMessage(properties, IO_COMPRESSION_CODECS_PROPERTY_NAME));

    return properties;
  }

  public static Map<Object, Object> enableIntermediateMapOutputCompression(Map<Object, Object> properties, String codec) {
    properties.put(
        MAPRED_COMPRESSION_MAP_OUTPUT_PROPERTY_NAME,
        ENABLED_COMPRESSION);
    log.info(getLogMessage(properties, MAPRED_COMPRESSION_MAP_OUTPUT_PROPERTY_NAME));

    properties.put(
        MAPRED_MAP_OUTPUT_COMPRESSION_CODEC_PROPERTY_NAME,
        AppUtils.isTestEnvironment() ?
            SNAPPY_CODEC_PROPERTY_VALUE :
            codec);
    log.info(getLogMessage(properties, MAPRED_MAP_OUTPUT_COMPRESSION_CODEC_PROPERTY_NAME));

    return properties;
  }

  public static Map<Object, Object> enableJobOutputCompression(Map<Object, Object> properties, String codec) {
    properties.put(
        MAPRED_OUTPUT_COMPRESS_PROPERTY_NAME,
        ENABLED_COMPRESSION);
    log.info(getLogMessage(properties, MAPRED_OUTPUT_COMPRESS_PROPERTY_NAME));

    properties.put(
        MAPRED_OUTPUT_COMPRESSION_TYPE_PROPERTY_NAME,
        MAPRED_OUTPUT_COMPRESSION_TYPE_PROPERTY_BLOCK_VALUE);
    log.info(getLogMessage(properties, MAPRED_OUTPUT_COMPRESSION_TYPE_PROPERTY_NAME));

    properties.put(
        MAPRED_OUTPUT_COMPRESSION_CODE_PROPERTY_NAME,
        AppUtils.isTestEnvironment() ?
            SNAPPY_CODEC_PROPERTY_VALUE :
            codec);
    log.info(getLogMessage(properties, MAPRED_OUTPUT_COMPRESSION_CODE_PROPERTY_NAME));

    return properties;
  }

  /**
   * DCC-1526: Enable DOT export.
   */
  public static Map<Object, Object> enableDotExports(Map<Object, Object> properties) {
    properties.put(
        DOT_FILE_PATH,
        CASCADING_DOT_FILE_PATH);
    log.info(getLogMessage(properties, DOT_FILE_PATH));

    properties.put(
        ERROR_DOT_FILE_NAME,
        CASCADING_ERROR_DOT_FILE_NAME);
    log.info(getLogMessage(properties, ERROR_DOT_FILE_NAME));

    return properties;
  }

  private static String getLogMessage(Map<Object, Object> properties, String property) {
    return format("Setting '%s' to '%s'", property, properties.get(property));
  }

}
