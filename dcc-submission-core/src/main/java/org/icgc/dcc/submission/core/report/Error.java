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
package org.icgc.dcc.submission.core.report;

import static java.util.Collections.emptyList;
import static org.icgc.dcc.common.json.Jackson.DEFAULT;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * Represents a validation error.
 */
@Value
public class Error {

  @JsonProperty
  ErrorType type;
  @JsonProperty
  int number;

  @JsonProperty
  String fileName;
  @JsonProperty
  List<String> fieldNames;

  @JsonProperty
  long lineNumber;
  @JsonProperty
  Object value;
  @JsonProperty
  Object[] params;

  public static Builder error() {
    return new Builder();
  }

  private Error(
      @JsonProperty("fileName") String fileName,
      @JsonProperty("fieldNames") List<String> fieldNames,
      @JsonProperty("number") int number,
      @JsonProperty("lineNumber") long lineNumber,
      @JsonProperty("value") Object value,
      @JsonProperty("type") ErrorType type,
      @JsonProperty("params") Object[] params) {
    this.fileName = fileName;
    this.fieldNames = fieldNames;
    this.number = number;
    this.lineNumber = lineNumber;
    this.value = value;
    this.type = type;
    this.params = params;
  }

  @JsonIgnore
  public String getMessage() {
    return type.format(type.build(params));
  }

  @Override
  public String toString() {
    return toJsonSummaryString();
  }

  @SneakyThrows
  public String toJsonSummaryString() {
    return "\n" + DEFAULT
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(this);
  }

  @Setter
  @Accessors(chain = true, fluent = true)
  public static class Builder {

    String fileName = null;
    List<String> fieldNames = emptyList();
    int number = 0;
    long lineNumber = -1;
    Object value = null;
    ErrorType type = null;
    Object[] params = null;

    public Builder fieldNames(@NonNull String... fieldNames) {
      this.fieldNames = ImmutableList.copyOf(fieldNames);
      return this;
    }

    public Builder fieldNames(@NonNull Iterable<String> fieldNames) {
      this.fieldNames = ImmutableList.copyOf(fieldNames);
      return this;
    }

    public Builder params(Object... params) {
      this.params = params;
      return this;
    }

    public Builder params(@NonNull Map<?, ?> params) {
      this.params = params.values().toArray();
      return this;
    }

    public Builder params(@NonNull Collection<?> params) {
      this.params = params.toArray();
      return this;
    }

    public Error build() {
      return new Error(fileName, fieldNames, number, lineNumber, value, type, params);
    }

  }

}
