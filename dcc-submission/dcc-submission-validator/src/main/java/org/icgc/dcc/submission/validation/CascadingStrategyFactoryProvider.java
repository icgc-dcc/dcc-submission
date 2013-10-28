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

import static com.google.common.base.Preconditions.checkArgument;
import static org.icgc.dcc.core.model.Configurations.HADOOP_KEY;

import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.submission.validation.factory.CascadingStrategyFactory;
import org.icgc.dcc.submission.validation.factory.HadoopCascadingStrategyFactory;
import org.icgc.dcc.submission.validation.factory.LocalCascadingStrategyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;

public class CascadingStrategyFactoryProvider implements Provider<CascadingStrategyFactory> {

  private static final Logger log = LoggerFactory.getLogger(CascadingStrategyFactoryProvider.class);

  private final FileSystem fs;

  private final Config config;

  @Inject
  CascadingStrategyFactoryProvider(Config config, FileSystem fs) {
    checkArgument(fs != null);
    checkArgument(config != null);
    this.fs = fs;
    this.config = config;
  }

  @Override
  public CascadingStrategyFactory get() {
    String fsUrl = fs.getScheme();

    if(fsUrl.equals("file")) {
      log.info("System configured for local filesystem");
      return new LocalCascadingStrategyFactory();
    } else if(fsUrl.equals("hdfs")) {
      log.info("System configured for Hadoop filesystem");
      return new HadoopCascadingStrategyFactory(config.getConfig(HADOOP_KEY), fs);
    } else {
      throw new RuntimeException("Unknown file system type: " + fsUrl + ". Expected file or hdfs");
    }
  }

}
