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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

/**
 * Utility class for working with cascading {@code Function} objects.
 */
public abstract class FunctionUtils {

  private FunctionUtils() {
    // Prevent construction
  }

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
   * TODO: rename to account for other types of Transformable
   */
  @SuppressWarnings("rawtypes")
  public static class CloneField extends BaseOperation implements Function {

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
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
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
      TupleEntry entry = functionCall.getArguments();

      Tuple copy = entry.getTupleCopy();
      if (copy.getObject(NEST_FIELD_INDEX) == null) { // If null, then replace it with an empty tuple
        copy.set(NEST_FIELD_INDEX, new Tuple());
      }

      functionCall.getOutputCollector().add(copy);
    }
  }
}
