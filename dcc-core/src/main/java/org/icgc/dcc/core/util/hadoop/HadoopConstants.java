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
package org.icgc.dcc.core.util.hadoop;

import static java.lang.Boolean.TRUE;
import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;

/**
 * Constants for hadoop, do not include any actual hadoop objects in here.
 */
// @formatter:off
// TODO: Move this an the other hadoop classes in this module to dcc-hadoop! That's the point!
@NoArgsConstructor(access = PRIVATE)
public final class HadoopConstants {
  
  public static final char PROPERTY_VALUES_SEPARATOR = ',';
  
  // Property names
  public static final String JAVA_LIBRARY_PATH_PROPERTY_NAME = "java.library.path";
  public static final String HADOOP_USER_NAME_PROPERTY_NAME = "HADOOP_USER_NAME";
  
  public static final String IO_COMPRESSION_CODECS_PROPERTY_NAME = "io.compression.codecs";
  public static final String MAPRED_COMPRESSION_MAP_OUTPUT_PROPERTY_NAME =       "mapred.compress.map.output";
  public static final String MAPRED_MAP_OUTPUT_COMPRESSION_CODEC_PROPERTY_NAME = "mapred.map.output.compression.codec";
  
  public static final String MAPRED_OUTPUT_COMPRESS_PROPERTY_NAME =              "mapred.output.compress";
  public static final String MAPRED_OUTPUT_COMPRESSION_CODE_PROPERTY_NAME =      "mapred.output.compression.codec";
  public static final String MAPRED_OUTPUT_COMPRESSION_TYPE_PROPERTY_NAME =      "mapred.output.compression.type";

  // Property values
  public static final String JAVA_LIBRARY_PATH_PROPERTY_NATIVE_VALUE = "/usr/lib/hadoop/lib/native";
  public static final String HDFS_USERNAME_PROPERTY_VALUE = "hdfs";
  
  public static final String MAPRED_OUTPUT_COMPRESSION_TYPE_PROPERTY_BLOCK_VALUE = "BLOCK";
  public static final String MAPRED_OUTPUT_COMPRESS_PROPERTY_TRUE_VALUE = TRUE.toString();
  public static final String COMPRESSION_MAP_OUTPUT_PROPERTY_TRUE_VALUE = TRUE.toString();
  
  public static final String DEFAULT_CODEC_PROPERTY_VALUE = "org.apache.hadoop.io.compress.DefaultCodec";
  public static final String GZIP_CODEC_PROPERTY_VALUE =    "org.apache.hadoop.io.compress.GzipCodec";  
  public static final String BZIP2_CODEC_PROPERTY_VALUE =   "org.apache.hadoop.io.compress.BZip2Codec";
  public static final String SNAPPY_CODEC_PROPERTY_VALUE =  "org.apache.hadoop.io.compress.SnappyCodec";
  public static final String LZOP_CODEC_PROPERTY_VALUE =    "com.hadoop.compression.lzo.LzopCodec";
  
  public static final String CASCADING_DOT_FILE_PATH = "";
  public static final String CASCADING_ERROR_DOT_FILE_NAME = "stream-error.dot";
}
//@formatter:on
