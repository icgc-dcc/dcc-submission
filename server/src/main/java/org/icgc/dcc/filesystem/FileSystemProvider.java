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
package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.typesafe.config.Config;

class FileSystemProvider implements Provider<FileSystem> {

  private static final Logger log = LoggerFactory.getLogger(FileSystemProvider.class);

  private final Configuration configuration; // hadoop's

  private final Config config; // typesafe's

  @Inject
  FileSystemProvider(Config config, Configuration hadoopConfig) {
    checkArgument(config != null);
    checkArgument(hadoopConfig != null);
    this.config = config;
    this.configuration = hadoopConfig;
  }

  @Override
  public FileSystem get() {
    String fsUrl = this.config.getString(FsConfig.FS_URL);
    this.configuration.set(CommonConfigurationKeys.FS_DEFAULT_NAME_KEY, fsUrl);
    try {
      log.info("configuration = " + HadoopUtils.getConfigurationDescription(this.configuration)); // TODO formatting?
      return FileSystem.get(this.configuration);
    } catch(IOException e) {
      throw new RuntimeException(e);// TODO: better
    }
  }
}
