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

import java.util.Map;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.common.core.util.InjectionNames;
import org.icgc.dcc.common.core.util.Scheme;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

@Slf4j
public class SubmissionPlatformStrategyFactoryProvider implements Provider<SubmissionPlatformStrategyFactory> {

  private final FileSystem fs;
  private final Map<String, String> hadoopProperties;

  @Inject
  public SubmissionPlatformStrategyFactoryProvider(
      @Named(InjectionNames.HADOOP_PROPERTIES) @NonNull final Map<String, String> hadoopProperties,
      @NonNull final FileSystem fs) {
    this.fs = fs;
    this.hadoopProperties = hadoopProperties;
  }

  @Override
  public SubmissionPlatformStrategyFactory get() {
    String fsUrl = fs.getScheme();

    if (Scheme.isFile(fsUrl)) {
      log.info("System configured for local filesystem");
      return new LocalSubmissionPlatformStrategyFactory(hadoopProperties);
    } else if (Scheme.isHdfs(fsUrl)) {
      log.info("System configured for Hadoop filesystem");
      return new HadoopSubmissionPlatformStrategyFactory(hadoopProperties, fs);
    } else {
      throw new RuntimeException("Unknown file system type: " + fsUrl + ". Expected file or hdfs");
    }
  }
}
