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
package org.icgc.dcc.submission.validation.cascading;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.icgc.dcc.hadoop.cascading.Tuples2.isNullField;
import static org.icgc.dcc.hadoop.cascading.Tuples2.nestTuple;

import java.io.Serializable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

/**
 * Utility class for working with cascading {@code Function} objects.
 * <p>
 * TODO: move outside of the submitter.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CascadingFunctions {

  /**
   * Simple function that logs the incoming tuple entries (useful for debugging).
   * <p>
   * Example of call: <code>pipe = new Each(pipe, new FunctionUtils.LogFunction("my_prefix"), Fields.RESULTS);</code>
   */
  @SuppressWarnings("rawtypes")
  public static class LogFunction extends BaseOperation implements Function {

    private final String prefix;

    public LogFunction(String prefix) {
      super(Fields.ARGS);
      this.prefix = prefix;
      System.out.println(prefix);
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      TupleEntry entry = functionCall.getArguments();
      System.out.println(prefix + "\t" + TupleEntryUtils.toJson(entry));
      functionCall.getOutputCollector().add(entry);
    }
  }

  /**
   * Clone a Field (possibly transforming its value).
   * <p>
   * Usage: clinicalPipe = new Each(clinicalPipe, new FunctionUtils.CloneField(originalField, clonedField), Fields.ALL);
   * <p>
   * TODO: rename to account for other types of Transformable
   */
  public static class CloneField extends BaseOperation<Void> implements Function<Void> {

    /**
     * Very basic for now, possibly offer more overloadings for transform()
     */
    public interface Transformable extends Serializable {

      String tranform(String value);
    }

    private final Fields originalField;

    private final Transformable transformable;

    public CloneField(Fields originalField, Fields newField) {
      this(originalField, newField, new Transformable() {

        @Override
        public String tranform(String value) {
          return value;
        }
      });
    }

    public CloneField(Fields originalField, Fields newField, Transformable transformable) {
      super(0, newField);
      checkArgument(originalField != null && originalField.size() == 1);
      checkArgument(newField != null && newField.size() == 1);
      this.originalField = checkNotNull(originalField);
      this.transformable = checkNotNull(transformable);
    }

    @Override
    public void operate(@SuppressWarnings("rawtypes")
    FlowProcess flowProcess, FunctionCall<Void> functionCall) {
      TupleEntry entry = functionCall.getArguments();
      String value = entry.getString(originalField);
      String newValue = transformable.tranform(value);
      functionCall.getOutputCollector().add(new Tuple(newValue));
    }
  }

  /**
   * Replaces a null value with an empty tuple.
   */
  @SuppressWarnings("rawtypes")
  public static class AddEmptyTuple extends BaseOperation implements Function {

    private static final int NEST_FIELD_INDEX = 0;

    public AddEmptyTuple() {
      super(Fields.ARGS);
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      Tuple copy = functionCall.getArguments().getTupleCopy();

      if (copy.getObject(NEST_FIELD_INDEX) == null) { // If null, then replace it with an empty tuple
        copy.set(NEST_FIELD_INDEX, new Tuple());
      }

      functionCall.getOutputCollector().add(copy);
    }
  }

  /**
   * Replaces the nulls resulting from a left join for summary data with the appropriate value (0 or false) based on the
   * feature type.
   */
  public static class ReplaceNulls extends BaseOperation<Void> implements cascading.operation.Function<Void> {

    private static final int SUMMARY_FIELD_INDEX = 0;

    /**
     * Default value when there was no match with the left join, or when the data type was not even provided.
     */
    public static final int COUNT_DEFAULT_VALUE = 0;

    /**
     * See {@link COUNT_DEFAULT_VALUE}.
     */
    public static final boolean EXISTENCE_DEFAULT_VALUE = false;

    private final boolean isCountSummary;

    public ReplaceNulls(boolean isCountSummary) {
      super(Fields.ARGS);
      this.isCountSummary = isCountSummary;
    }

    @Override
    public void operate(
        @SuppressWarnings("rawtypes")
        FlowProcess flowProcess,
        FunctionCall<Void> functionCall) {
      TupleEntry entry = functionCall.getArguments();

      Tuple tupleCopy = entry.getTupleCopy();
      if (isNullField(tupleCopy, SUMMARY_FIELD_INDEX)) {
        tupleCopy.set(
            SUMMARY_FIELD_INDEX,
            isCountSummary ?
                COUNT_DEFAULT_VALUE :
                EXISTENCE_DEFAULT_VALUE);
      }

      functionCall
          .getOutputCollector()
          .add(tupleCopy);
    }
  }

  /**
   * Adds a {@link Tuple} containing the list of available data types for the submission.
   */
  public static class AvailableDataTypes extends BaseOperation<Void> implements Function<Void> {

    public AvailableDataTypes(Fields availableDataTypeField) {
      super(availableDataTypeField);
    }

    @Override
    public void operate(@SuppressWarnings("rawtypes")
    FlowProcess flowProcess, FunctionCall<Void> functionCall) {
      TupleEntry entry = functionCall.getArguments();

      Tuple newTuple = nestTuple(createAvailableDataTypes(entry));

      functionCall
          .getOutputCollector()
          .add(newTuple);
    }

    /**
     * Creates the {@link Tuple} described at {@link AvailableDataTypes}.
     * <p>
     * TODO: move to Summary class?
     */
    private Tuple createAvailableDataTypes(TupleEntry entry) {
      Tuple availableDataTypes = new Tuple();
      for (FeatureType type : FeatureType.values()) {
        String summaryFieldName = type.getSummaryFieldName();
        if (hasPositiveCount(entry, type, summaryFieldName) ||
            isMarkedPresent(entry, type, summaryFieldName)) {
          availableDataTypes.add(type.getTypeName());
        }
      }
      return availableDataTypes;
    }

    /**
     * Checks whether the summary field (expected to be a count) is greater than 0 or not.
     */
    private boolean hasPositiveCount(TupleEntry entry, FeatureType type, String summaryFieldName) {
      return type.isCountSummary() &&
          checkNotNull(entry.getLong(summaryFieldName), "Expecting a long for field %s", summaryFieldName) > 0;
    }

    /**
     * Checks whether the summary field (expected to be a boolean) is true (data type present) or not (data type
     * absent).
     */
    private boolean isMarkedPresent(TupleEntry entry, FeatureType type, String summaryFieldName) {
      return !type.isCountSummary()
          && checkNotNull(entry.getBoolean(summaryFieldName), "Expecting a boolean for field %s", summaryFieldName);
    }
  }

  public static class MissingFieldsAdder extends BaseOperation<Void> implements Function<Void> {

    /**
     * At the moment we just nullify it.
     */
    public static final String MISSING_VALUE = null;

    /**
     * {@link Tuple} to add to every record.
     */
    private final Tuple missingTuple;

    public MissingFieldsAdder(Fields missingFields) {
      super(missingFields);

      // Create tuple to be added for every records
      missingTuple = new Tuple();
      for (int i = 0; i < missingFields.size(); i++) {
        missingTuple.add(MISSING_VALUE);
      }
    }

    @Override
    public void operate(
        @SuppressWarnings("rawtypes")
        FlowProcess flowProcess,
        FunctionCall<Void> functionCall) {
      functionCall.getOutputCollector().add(new Tuple(missingTuple));
    }

  }
}
