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
package org.icgc.dcc.submission.core.report;

import static com.google.common.base.Preconditions.checkArgument;
import static org.icgc.dcc.submission.core.report.ErrorLevel.CELL_LEVEL;
import static org.icgc.dcc.submission.core.report.ErrorLevel.FILE_LEVEL;
import static org.icgc.dcc.submission.core.report.ErrorLevel.ROW_LEVEL;
import static org.icgc.dcc.submission.core.report.ErrorParameterKey.EXPECTED;
import static org.icgc.dcc.submission.core.report.ErrorParameterKey.FIELDS;
import static org.icgc.dcc.submission.core.report.ErrorParameterKey.FILES;
import static org.icgc.dcc.submission.core.report.ErrorParameterKey.MAX;
import static org.icgc.dcc.submission.core.report.ErrorParameterKey.MIN;
import static org.icgc.dcc.submission.core.report.ErrorParameterKey.OTHER_FIELDS;
import static org.icgc.dcc.submission.core.report.ErrorParameterKey.OTHER_SCHEMA;
import static org.icgc.dcc.submission.core.report.ErrorParameterKey.SCHEMA;
import static org.icgc.dcc.submission.core.report.ErrorParameterKey.VALUE;
import static org.icgc.dcc.submission.core.report.ErrorParameterKey.VALUE2;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.icgc.dcc.common.core.model.ValueType;

import com.google.common.collect.ImmutableMap;

/**
 * Represents a validation {@link Error} classification.
 */
@RequiredArgsConstructor
public enum ErrorType {

  /**
   * Number of columns does not match that of header.
   */
  STRUCTURALLY_INVALID_ROW_ERROR(ROW_LEVEL, "Structurally invalid row: %s columns against %s declared in the header (row will be ignored by the rest of validation)", true) {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, Integer.class);
      return ImmutableMap.of(
          EXPECTED, params[0]);
    }
  },

  /**
   * Last line is missing new line.
   */
  LINE_TERMINATOR_MISSING_ERROR(ROW_LEVEL, "Row is missing line terminator. Expected \\n", true) {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  },

  /**
   * TODO.
   */
  INVALID_CHARSET_ROW_ERROR(ROW_LEVEL, "Row contains invalid charset", true) {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class);
      return ImmutableMap.of(
          EXPECTED, params[0]);
    }
  },

  /**
   * A forbidden value was found (for instance deprecated "-999" value).
   */
  FORBIDDEN_VALUE_ERROR(CELL_LEVEL, "Invalid value (%s) for field %s. Cannot use forbidden value: %s") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class);
      return ImmutableMap.of(
          VALUE, params[0]);
    }
  },

  /**
   * No matching value(s) for referencED field(s).
   * <p>
   * Example: for specimen report if say specimen.donor_id holds value "my_donor_X" but donor.donor_id does not
   * <p>
   * Not to be confused with its file counterpart {@code RELATION_FILE_ERROR}
   */
  RELATION_VALUE_ERROR(CELL_LEVEL, "Invalid value(s) (%s) for field(s) %s.%s. Expected to match value(s) in: %s.%s") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class, List.class);
      return ImmutableMap.of(
          OTHER_SCHEMA, params[0],
          OTHER_FIELDS, params[1]);
    }
  },

  /**
   * No matching value(s) for referencING field(s) (only applicable if relation is set to bidirectional).
   * <p>
   * Example: e.g,. for specimen report: if specimen.donor_id does not hold value "my_donor_B" but donor.donor_id does
   * (would not be a problem with family.donor_id as the relation from family to donor is not bidirectional with donor,
   * unlike the one from specimen to donor)
   * <p>
   * Not quite the value-counterpart to {@code REVERSE_RELATION_FILE_ERROR}
   */
  RELATION_PARENT_VALUE_ERROR(CELL_LEVEL, "No corresponding values in %s.%s for value(s) %s in %s.%s") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class, List.class);
      return ImmutableMap.of(
          OTHER_SCHEMA, params[0],
          OTHER_FIELDS, params[1]);
    }
  },

  /**
   * Duplicate values in unique field(s).
   */
  UNIQUE_VALUE_ERROR(CELL_LEVEL, "Invalid set of values (%s) for fields %s. Expected to be unique") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  },

  /**
   * Invalid value type (i.e. a string where an integer is expected).
   */
  VALUE_TYPE_ERROR(CELL_LEVEL, "Invalid value (%s) for field %s. Expected type is: %s") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, ValueType.class);
      return ImmutableMap.of(
          EXPECTED, params[0]);
    }
  },

  /**
   * Value out for (inclusive) range.
   */
  OUT_OF_RANGE_ERROR(CELL_LEVEL, "Number %d is out of range for field %s. Expected value between %d and %d") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, Long.class, Long.class);
      return ImmutableMap.of(
          MIN, params[0],
          MAX, params[1]);
    }
  },

  /**
   * Missing required value.
   */
  MISSING_VALUE_ERROR(CELL_LEVEL, "Value missing for required field: %s") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  },

  /**
   * Values not in code list (as codes)
   */
  CODELIST_ERROR(CELL_LEVEL, "Invalid value %s for field %s. Expected code or value from CodeList %s") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  },

  /**
   * Values not in set of discrete values.
   */
  DISCRETE_VALUES_ERROR(CELL_LEVEL, "Invalid value %s for field %s. Expected one of the following values: %s") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, Set.class);
      return ImmutableMap.of(
          EXPECTED, params[0]);
    }
  },

  /**
   * Values do not match regex.
   */
  REGEX_ERROR(CELL_LEVEL, "Invalid value %s for field %s. Expected to match regex: %s") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class);
      return ImmutableMap.of(
          EXPECTED, params[0]);
    }
  },

  /**
   * Values do not pass script.
   */
  SCRIPT_ERROR(CELL_LEVEL, "Invalid value %s for field %s. Expected to pass script: %s") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  },

  /**
   * More than one file matches the schema pattern.
   */
  TOO_MANY_FILES_ERROR(FILE_LEVEL, "More than one file matches the schema pattern") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class, List.class);
      return ImmutableMap.of(SCHEMA, params[0], FILES, params[1]);
    }
  },

  /**
   * No matching file for referencED schema.
   * <p>
   * Example: for specimen, if donor is missing
   * <p>
   * Not to be confused with its value counterpart {@code RELATION_VALUE_ERROR}
   */
  RELATION_FILE_ERROR(FILE_LEVEL, "Relation to schema %s has no matching file") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class);
      return ImmutableMap.of(SCHEMA, params[0]);
    }
  },

  /**
   * No matching file for referencING schema (only applicable if relation is set to bidirectional).
   * <p>
   * Example: for specimen, if sample is missing (but not if surgery is missing, as the relation from surgery is not
   * bidirectional)
   * <p>
   * Not quite the file-counterpart to {@code RELATION_PARENT_VALUE_ERROR}
   */
  REVERSE_RELATION_FILE_ERROR(FILE_LEVEL, "Relation from schema %s has no matching file and this relation imposes that there be one") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class);
      return ImmutableMap.of(SCHEMA, params[0]);
    }
  },

  /**
   * concatentating bzip2 files is not supported
   */
  UNSUPPORTED_COMPRESSED_FILE(FILE_LEVEL, "The compressed file should not be concatenated or the block header is corrupted") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class);
      return ImmutableMap.of(SCHEMA, params[0]);
    }
  },

  /**
   * Compression codec doesn't match file extension
   */
  COMPRESSION_CODEC_ERROR(FILE_LEVEL, "File compression type does not match file extension") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class);
      return ImmutableMap.of(SCHEMA, params[0]);
    }
  },

  /**
   * Repeated field names found in header.
   */
  DUPLICATE_HEADER_ERROR(FILE_LEVEL, "Duplicate header found: %s") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, List.class);
      return ImmutableMap.of(FIELDS, params[0]);
    }
  },

  /**
   * File has no data rows.
   */
  MISSING_ROWS_ERROR(FILE_LEVEL, "No rows found: %s") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      return ImmutableMap.of();
    }
  },

  /**
   * Repeated field names found in header.
   */
  FILE_HEADER_ERROR(FILE_LEVEL, "File header error: %s") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, List.class, List.class);
      return ImmutableMap.of(EXPECTED, params[0], VALUE, params[1]);
    }
  },

  /**
   * Submitted reference genome does not match the starnde reference genome.
   */
  REFERENCE_GENOME_MISMATCH_ERROR(CELL_LEVEL, "Found value %s for column %s, reference genome is %s") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class);
      return ImmutableMap.of(EXPECTED, params[0]);
    }
  },

  /**
   * Submitted reference genome has type insertion. Expect "-" but found something else instead
   */
  REFERENCE_GENOME_INSERTION_ERROR(CELL_LEVEL, "Found value %s for column %s, reference genome is %s") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class);
      return ImmutableMap.of(EXPECTED, params[0]);
    }
  },

  /**
   * It is considered abnormal to report confidential observation above a certain configured threshold.
   */
  TOO_MANY_CONFIDENTIAL_OBSERVATIONS_ERROR(FILE_LEVEL, "An unreasonnably high number of sensitive observations have been detected.") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, Long.class, Long.class, Float.class);
      return ImmutableMap.of(VALUE, params[0], VALUE2, params[1], EXPECTED, params[2]);
    }
  },

  /**
   * Sample types should be consistent between clinical and experimental meta files
   */
  SAMPLE_TYPE_MISMATCH(CELL_LEVEL, "Inconsistent sample type between clinical and experimental meta files") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class, String.class, String.class);
      return ImmutableMap.of(VALUE, params[0], EXPECTED, params[1]);
    }
  },

  /**
   * Reference sample types should be consistent between clinical and experimental meta files
   */
  REFERENCE_SAMPLE_TYPE_MISMATCH(CELL_LEVEL, "Inconsistent reference sample type between clinical and experimental meta files") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class, String.class, String.class);
      return ImmutableMap.of(VALUE, params[0], EXPECTED, params[1]);
    }

  },

  /**
   * Sample study should be consistent between ICGC DCC and pancancer.info
   */
  PCAWG_SAMPLE_STUDY_MISMATCH(CELL_LEVEL, "Inconsistent sample study between ICGC DCC and pancancer.info") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      return ImmutableMap.of();
    }

  },

  /**
   * Additional clinical field constraint for PCAWG.
   */
  PCAWG_CLINICAL_FIELD_REQUIRED(CELL_LEVEL, "Clinical field is required for PanCancer") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class);
      return ImmutableMap.of();
    }

  },

  /**
   * Additional clinical row constraint for PCAWG.
   */
  PCAWG_CLINICAL_ROW_REQUIRED(ROW_LEVEL, "Clinical row is required for PanCancer") {

    @Override
    public final ImmutableMap<ErrorParameterKey, Object> build(Object... params) {
      checkParams(params, String.class, String.class, String.class);
      return ImmutableMap.of(VALUE, params[0]);
    }

  };

  /**
   * Metadata.
   */
  @Getter
  private final ErrorLevel level;
  private final String message;
  private final boolean structural;

  public abstract ImmutableMap<ErrorParameterKey, Object> build(@Nullable Object... params);

  protected void checkParams(Object[] params, Class<?>... paramTypes) {
    val check = false;
    if (check) {
      // Verify param existence
      checkArgument(
          params != null,
          "'%s' expects params when build() is invoked but '%s' was supplied instead",
          this, Arrays.toString(params));

      // Verify param lengths
      checkArgument(
          params.length == paramTypes.length,
          "'%s' expects %s params of types '%s' when build() is invoked but %s params ('%s') of types '%s' was supplied instead",
          this, paramTypes.length, Arrays.toString(paramTypes), params.length, Arrays.toString(params),
          Arrays.toString(getParamTypes(params)));

      // Verify param types
      for (int i = 0; i < paramTypes.length; i++) {
        val param = params[i];
        val paramType = paramTypes[i];

        checkArgument(
            paramType.isAssignableFrom(param.getClass()),
            "'%s' expects param '%s' to be of type '%s' when build() is invoked but '%s' was supplied instead",
            this, param, paramType, param.getClass());
      }
    }
  }

  protected Class<?>[] getParamTypes(Object[] params) {
    Class<?>[] paramTypes = new Class<?>[params.length];
    for (int i = 0; i < params.length; i++) {
      paramTypes[i] = params[i].getClass();
    }

    return paramTypes;
  }

  ErrorType(ErrorLevel level, String message) {
    this(level, message, false);
  }

  public String format(Map<ErrorParameterKey, ? extends Object> parameters) {
    // The formatted message doesn't make sense anymore since the column name was moved to TupleError
    // return String.format(message, terms);
    return this.message;
  }

  public boolean isStructural() {
    return structural;
  }

}
