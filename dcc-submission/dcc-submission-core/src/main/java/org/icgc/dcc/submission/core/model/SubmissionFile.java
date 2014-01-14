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
package org.icgc.dcc.submission.core.model;

import static org.icgc.dcc.submission.dictionary.util.Dictionaries.getFeatureType;
import static org.icgc.dcc.submission.dictionary.util.Dictionaries.getFileSchema;

import java.util.Date;

import lombok.Getter;
import lombok.val;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.icgc.dcc.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.dictionary.model.Dictionary;

/**
 * For serializing file data through the REST interface
 */
@Getter
public class SubmissionFile {

  private final String name;
  private final Date lastUpdate;
  private final long size;
  private final String schemaName;
  private final String featureTypeName;

  @JsonCreator
  public SubmissionFile(
      @JsonProperty("name") String name,
      @JsonProperty("lastUpdate") Date lastUpdate,
      @JsonProperty("size") long size,
      @JsonProperty("schema") String schemaName,
      @JsonProperty("featureTypeName") String featureTypeName) {
    this.name = name;
    this.lastUpdate = lastUpdate;
    this.size = size;
    this.schemaName = schemaName;
    this.featureTypeName = featureTypeName;
  }

  public SubmissionFile(Path path, FileSystem fs, Dictionary dictionary) {
    this.name = path.getName();

    FileStatus fileStatus = HadoopUtils.getFileStatus(fs, path);
    this.lastUpdate = new Date(fileStatus.getModificationTime());
    this.size = fileStatus.getLen();

    val fileSchema = getFileSchema(dictionary, this.name);
    if (fileSchema.isPresent()) {
      val featureType = getFeatureType(fileSchema.get());

      this.schemaName = fileSchema.get().getName();
      this.featureTypeName = featureType.isPresent() ? featureType.get().name() : null;
    } else {
      this.schemaName = null;
      this.featureTypeName = null;
    }
  }

}
