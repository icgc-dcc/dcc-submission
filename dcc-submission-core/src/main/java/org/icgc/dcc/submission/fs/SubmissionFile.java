/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.fs;

import static com.google.common.base.Optional.fromNullable;

import java.util.Date;

import lombok.NonNull;
import lombok.Value;

import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.util.Serdes.FileTypeDeserializer;
import org.icgc.dcc.submission.core.util.Serdes.FileTypeSerializer;
import org.icgc.dcc.submission.core.util.TypeConverters.FileTypeConverter;
import org.mongodb.morphia.annotations.Converters;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Optional;

/**
 * For serializing file data through the REST interface
 */
@Value
@Converters(FileTypeConverter.class)
public class SubmissionFile {

  private final String name;
  private final Date lastUpdate;
  private final long size;
  private final FileType fileType;

  @JsonCreator
  public SubmissionFile(
      @NonNull//
      @JsonProperty("name")//
      String name,

      @NonNull//
      @JsonProperty("lastUpdate")//
      Date lastUpdate,

      @JsonProperty("size")//
      long size,

      @JsonProperty("fileType")//
      @JsonSerialize(using = FileTypeSerializer.class)//
      @JsonDeserialize(using = FileTypeDeserializer.class)//
      FileType fileType)
  {
    this.name = name;
    this.lastUpdate = lastUpdate;
    this.size = size;
    this.fileType = fileType;
  }

  @JsonIgnore
  public Optional<DataType> getDataType() {
    return fromNullable(fileType != null ? fileType.getDataType() : null);
  }

}
