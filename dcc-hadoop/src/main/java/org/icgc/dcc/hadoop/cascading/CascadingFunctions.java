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
package org.icgc.dcc.hadoop.cascading;

import static cascading.tuple.Fields.ARGS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.operation.NoOp;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

/**
 * Utility class for working with cascading {@code Function} objects.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CascadingFunctions {

  /**
   * {@link Function} that emits no {@link Tuple}s. It is different than {@link NoOp} because it still preserves the
   * schema (TODO: unusure why NoOp doesn't, figure it out..).
   */
  public static final class EmitNothing extends BaseOperation<Void> implements Function<Void> {

    public EmitNothing() {
      super(ARGS);
    }

    @Override
    public void operate(
        @SuppressWarnings("rawtypes") FlowProcess flowProcess,
        FunctionCall<Void> functionCall) {
    }
  }

  /**
   * TODO
   */
  public static final class Counter extends BaseOperation<Void> implements Function<Void> {

    private final Enum<?> counter;
    private final long increment;

    public Counter(Enum<?> counter, long increment) {
      super(ARGS);
      this.counter = counter;
      this.increment = increment;
    }

    @Override
    public void operate(
        @SuppressWarnings("rawtypes") FlowProcess flowProcess,
        FunctionCall<Void> functionCall) {
      flowProcess.increment(counter, increment);
      functionCall
          .getOutputCollector()
          .add(functionCall.getArguments());
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
    public void operate(@SuppressWarnings("rawtypes") FlowProcess flowProcess, FunctionCall<Void> functionCall) {
      TupleEntry entry = functionCall.getArguments();
      String value = entry.getString(originalField);
      String newValue = transformable.tranform(value);
      functionCall.getOutputCollector().add(new Tuple(newValue));
    }
  }

}
