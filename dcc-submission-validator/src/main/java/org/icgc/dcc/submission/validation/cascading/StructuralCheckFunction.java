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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.common.cascading.RemoveHollowTupleFilter;
import org.icgc.dcc.common.core.model.SpecialValue;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

/**
 * Checks structural aspects of an input data file (header, format, ...)
 * <p>
 * loader's counterpart is {@link PreProcessFunction}
 * <p>
 * Empty lines have already been filtered out in {@link RemoveHollowTupleFilter}.
 * <p>
 * TODO: this should be split in multiple operations.
 */
@SuppressWarnings("rawtypes")
public class StructuralCheckFunction extends BaseOperation implements Function {

  public static final String LINE_FIELD_NAME = "line";

  public static final char FIELD_SEPARATOR = '\t';

  private final Integer headerSize;

  private final Fields dictionaryFields;

  @SuppressWarnings("unchecked")
  public StructuralCheckFunction(Iterable<String> fieldNames) {
    super(1);
    dictionaryFields = new Fields(Iterables.toArray(fieldNames, String.class));
    headerSize = dictionaryFields.size();
    fieldDeclaration = dictionaryFields.append(ValidationFields.STATE_FIELD);
  }

  @Override
  public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
    checkState(fieldDeclaration != null);
    checkState(headerSize != null);

    TupleEntry arguments = functionCall.getArguments();

    long offset = functionCall.getArguments().getLong(ValidationFields.OFFSET_FIELD_NAME);
    TupleState tupleState = new TupleState(offset);

    String line = arguments.getString(LINE_FIELD_NAME);
    List<String> values = parseValues(line);
    List<String> adjustedValues = adjustValues(values, tupleState);

    List<Object> tupleValues = Lists.<Object> newArrayList(adjustedValues);
    tupleValues.add(tupleState); // lastly state
    checkState(fieldDeclaration.size() == tupleValues.size());

    functionCall.getOutputCollector().add(new Tuple(tupleValues.toArray()));
  }

  public static List<String> parseValues(String line) {
    return newArrayList(Splitter.on(FIELD_SEPARATOR).split(line));
  }

  private List<String> adjustValues(List<String> values, TupleState tupleState) {
    checkState(headerSize == values.size(),
        "'%s' != '%s'", headerSize, values.size());
    return replaceEmptyStrings(convertMissingCodes(
        values,
        tupleState));
  }

  private List<String> convertMissingCodes(List<String> values, TupleState tupleState) {
    List<String> adjustedValues = new ArrayList<String>(values.size());
    for (int i = 0; i < values.size(); i++) {
      String value = values.get(i);
      if (SpecialValue.FULL_MISSING_CODES.contains(value)) {
        adjustedValues.add((String) SpecialValue.NO_VALUE);

        // Mark field as originally using a missing code
        tupleState.addMissingField((String) this.getFieldDeclaration().get(i));
      } else {
        adjustedValues.add(value);
      }
    }
    return adjustedValues;
  }

  private List<String> replaceEmptyStrings(List<String> adjustedValues) {
    for (int i = 0; i < adjustedValues.size(); i++) {
      String value = adjustedValues.get(i);
      if (value != null && isBlank(value)) {
        adjustedValues.set(i, null);
      }
    }
    return adjustedValues;
  }

}
