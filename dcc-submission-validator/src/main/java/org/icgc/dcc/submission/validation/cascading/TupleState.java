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
package org.icgc.dcc.submission.validation.cascading;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.ToString;

import org.icgc.dcc.submission.core.report.ErrorParameterKey;
import org.icgc.dcc.submission.core.report.ErrorType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * Each {@code Tuple} should have one field that holds asn instance of this class. It is used to track the state
 * (valid/invalid and corresponding reasons) of the whole {@code Tuple}.
 */

public class TupleState implements Serializable {

  private final long offset;

  private List<TupleError> errors;

  private boolean structurallyValid; // to save time on filtering

  private final Set<String> missingFieldNames = new HashSet<String>();

  public TupleState() {
    this(-1L);
  }

  public TupleState(long offset) {
    this.structurallyValid = true;
    this.offset = offset;
  }

  public void reportError(ErrorType type, List<String> columnNames, Object values, Object... params) {
    checkArgument(type != null);
    ensureErrors().add(new TupleError(type, columnNames, values, this.getOffset(), type.build(params)));
    structurallyValid = type.isStructural() == false;
  }

  public void reportError(ErrorType type, String columnName, Object value, Object... params) {
    checkArgument(type != null);
    List<String> columnNames = Lists.newArrayList(columnName);
    ensureErrors().add(new TupleError(type, columnNames, value, this.getOffset(), type.build(params)));
    structurallyValid = type.isStructural() == false;
  }

  public void reportError(int number, ErrorType type, String columnName, Object value, Object... params) {
    checkArgument(type != null);
    List<String> columnNames = Lists.newArrayList(columnName);
    ensureErrors().add(new TupleError(type, columnNames, number, value, this.getOffset(), type.build(params)));
    structurallyValid = type.isStructural() == false;
  }

  // TODO: this is just temporary until a nicer error reporting is in place
  public static TupleError createTupleError(ErrorType type, int number, String columnName, Object value,
      long lineNumber,
      Object... params) {
    return new TupleError(type, Lists.newArrayList(columnName), number, value, lineNumber, type.build(params));
  }

  public Iterable<TupleError> getErrors() {
    return ensureErrors();
  }

  @JsonIgnore
  public boolean isValid() {
    return errors == null || errors.size() == 0;
  }

  @JsonIgnore
  public boolean isInvalid() {
    return isValid() == false;
  }

  @JsonIgnore
  public boolean isStructurallyValid() {
    return structurallyValid;
  }

  public long getOffset() {
    return offset;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(TupleState.class).add(ValidationFields.OFFSET_FIELD_NAME, offset)
        .add("valid", isValid()).add("errors", errors).toString();
  }

  public void addMissingField(String fieldName) {
    this.missingFieldNames.add(fieldName);
  }

  @JsonIgnore
  public boolean isFieldMissing(String fieldName) {
    return this.missingFieldNames.contains(fieldName);
  }

  /**
   * Used to lazily instantiate the errors list. This method never returns {@code null}.
   */
  private List<TupleError> ensureErrors() {
    return errors == null ? (errors = Lists.newArrayListWithExpectedSize(3)) : errors;
  }

  /**
   * Holds an error. The {@code type} uniquely identifies the error (e.g.: range error) and the {@code parameters}
   * capture the error details (e.g.: the expected range; min and max).
   */
  @Getter
  @ToString
  public static final class TupleError implements Serializable {

    private final ErrorType type;

    private final List<String> columnNames;

    private final int number;

    private final Object value;

    private final Long line;

    private final Map<ErrorParameterKey, Object> parameters;

    public TupleError() {
      this(null, Lists.<String> newArrayList(), null, null, new LinkedHashMap<ErrorParameterKey, Object>());
    }

    private TupleError(ErrorType type, List<String> columnNames, Object value, Long line,
        Map<ErrorParameterKey, Object> parameters) {
      this(type, columnNames, 0, value, line, parameters);
    }

    private TupleError(ErrorType type, List<String> columnNames, int number, Object value, Long line,
        Map<ErrorParameterKey, Object> parameters) {
      this.type = type;
      this.number = number;
      this.columnNames = columnNames;
      this.value = firstNonNull(value, "");
      this.line = line;
      this.parameters = parameters;
    }

    @JsonIgnore
    public String getMessage() {
      return type.format(getParameters());
    }

  }

}
