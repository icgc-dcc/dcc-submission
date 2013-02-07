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
package org.icgc.dcc.validation.cascading;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.icgc.dcc.validation.ValidationErrorCode;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Checks structural aspects of an input data file (header, format, ...)
 */
@SuppressWarnings("rawtypes")
public class StructuralCheckFunction extends BaseOperation implements Function {

  private static final String ROW_LEVEL_ERROR_COLUMN_NAME = "RowLevelError";

  public static final String LINE_FIELD_NAME = "line";

  public static final char FIELD_SEPARATOR = '\t';

  public static final List<String> MISSING_CODES = Arrays.asList("-777", "-888", "-999"); // TODO: move elsewhere?

  private static boolean REPORT_WARNINGS = false; // see DCC-270 & DCC-411

  private Integer headerSize;

  private final Fields dictionaryFields;

  private List<Integer> unknownHeaderIndices;

  public StructuralCheckFunction(Iterable<String> fieldNames) {
    super(1);
    dictionaryFields = new Fields(Iterables.toArray(fieldNames, String.class));
  }

  @SuppressWarnings("unchecked")
  public void processFileHeader(Fields headerFields) {
    headerSize = headerFields.size();

    Fields mergedFields = Fields.merge(headerFields, dictionaryFields);
    Fields extraFields = mergedFields.subtract(dictionaryFields);
    Fields adjustedFields = headerFields.subtract(extraFields); // existing valid fields first
    Fields missingFields = dictionaryFields.subtract(adjustedFields);
    adjustedFields = adjustedFields.append(missingFields); // then missing fields to be emulated
    checkState(FieldsUtils.buildSortedList(dictionaryFields)//
        .equals(FieldsUtils.buildSortedList(adjustedFields))); // worth checking; order may differ but nothing else
    fieldDeclaration = adjustedFields.append(ValidationFields.STATE_FIELD); // lastly state

    unknownHeaderIndices = FieldsUtils.indicesOf(headerFields, extraFields);
  }

  @Override
  public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
    checkState(fieldDeclaration != null);
    checkState(headerSize != null);

    TupleEntry arguments = functionCall.getArguments();

    long offset = functionCall.getArguments().getLong(ValidationFields.OFFSET_FIELD_NAME);
    TupleState tupleState = new TupleState(offset);

    String line = arguments.getString(StructuralCheckFunction.LINE_FIELD_NAME);
    List<String> values = Lists.newArrayList(Splitter.on(FIELD_SEPARATOR).split(line));
    List<String> adjustedValues = adjustValues(values, tupleState);
    List<Object> tupleValues = Lists.<Object> newArrayList(adjustedValues);
    tupleValues.add(tupleState); // lastly state
    checkState(fieldDeclaration.size() == tupleValues.size());

    functionCall.getOutputCollector().add(new Tuple(tupleValues.toArray()));
  }

  private List<String> adjustValues(List<String> values, TupleState tupleState) {
    List<String> adjustedValues = null;
    int dataSize = values.size();
    if(headerSize == dataSize) {
      adjustedValues = filterUnknownColumns(values); // existing valid fields first
      adjustedValues = padMissingColumns(adjustedValues); // then missing fields to be emulated
      adjustedValues = convertMissingCodes(adjustedValues, tupleState);
      adjustedValues = replaceEmptyStrings(adjustedValues);
      if(REPORT_WARNINGS && unknownHeaderIndices.isEmpty() == false) {
        // FIXME: not working as expected anyway: this would report the error on all tuples
        // tupleState.reportError(ValidationErrorCode.UNKNOWN_COLUMNS_WARNING,
        // ValidationErrorCode.FILE_LEVEL_ERROR_COLUMN_NAME, unknownHeaderIndices);
      }
    } else {
      adjustedValues = Arrays.asList(new String[dictionaryFields.size()]); // can discard values but must match number
                                                                           // of fields in headers for later merge in
                                                                           // error reporting
      tupleState.reportError(ValidationErrorCode.STRUCTURALLY_INVALID_ROW_ERROR,//
          ROW_LEVEL_ERROR_COLUMN_NAME, // because we don't have a specific column to report this on (TODO: revisit not
                                       // elegant)
          dataSize, headerSize); // we use dataSize as the value, and headerSize as an expected value parameter
    }
    return adjustedValues;
  }

  private List<String> convertMissingCodes(List<String> values, TupleState tupleState) {
    List<String> adjustedValues = new ArrayList<String>(values.size());
    for(int i = 0; i < values.size(); i++) {
      String value = values.get(i);
      if(MISSING_CODES.contains(value)) {
        adjustedValues.add(null);
        tupleState.addMissingField((String) this.getFieldDeclaration().get(i));
      } else {
        adjustedValues.add(value);
      }
    }
    return adjustedValues;
  }

  /*
   * ignore columns corresponding to unknown headers (TODO: see DCC-270 to improve on this)
   */
  private List<String> filterUnknownColumns(List<String> values) {
    List<String> adjustedValues = new ArrayList<String>();
    for(int i = 0; i < values.size(); i++) {
      if(unknownHeaderIndices.contains(i) == false) {
        adjustedValues.add(values.get(i));
      }
    }
    return adjustedValues;
  }

  private List<String> padMissingColumns(List<String> adjustedValues) {
    int adjustedDataSize = adjustedValues.size();
    int size = dictionaryFields.size();
    checkState(adjustedDataSize <= size); // by design (since we discarded unknown columns)
    if(adjustedDataSize < size) { // padding with nulls
      adjustedValues.addAll(Arrays.asList(new String[size - adjustedDataSize]));
    }
    return adjustedValues;
  }

  private List<String> replaceEmptyStrings(List<String> adjustedValues) {
    for(int i = 0; i < adjustedValues.size(); i++) {
      if("".equals(adjustedValues.get(i))) {
        adjustedValues.set(i, null);
      }
    }
    return adjustedValues;
  }
}
