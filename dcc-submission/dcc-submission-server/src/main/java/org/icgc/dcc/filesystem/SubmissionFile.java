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

import java.util.Date;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;

/**
 * For serializing file data through the REST interface
 */
public class SubmissionFile {
  private final String name;

  private final Date lastUpdate;

  private final long size;

  private String matchedSchemaName;

  @JsonCreator
  public SubmissionFile(@JsonProperty("name") String name, @JsonProperty("lastUpdate") Date lastUpdate,
      @JsonProperty("size") long size, @JsonProperty("schema") String schema) {
    this.name = name;
    this.lastUpdate = lastUpdate;
    this.size = size;
    this.matchedSchemaName = schema;
  }

  public SubmissionFile(Path path, FileSystem fs, Dictionary dict) {
    this.name = path.getName();

    FileStatus fileStatus = HadoopUtils.getFileStatus(fs, path);
    this.lastUpdate = new Date(fileStatus.getModificationTime());
    this.size = fileStatus.getLen();

    this.matchedSchemaName = null;
    for(FileSchema schema : dict.getFiles()) {
      if(Pattern.matches(schema.getPattern(), this.name)) {
        this.matchedSchemaName = schema.getName();
        break;
      }
    }
  }

  public String getName() {
    return name;
  }

  public Date getLastUpdate() {
    return lastUpdate;
  }

  public long getSize() {
    return size;
  }

  public String getMatchedSchemaName() {
    return matchedSchemaName;
  }
}
