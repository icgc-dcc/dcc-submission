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

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with cascading {@code Function} objects.
 */
public class FunctionUtils {
  private FunctionUtils() {
    // Prevent construction
  }

  /**
   * Simple function that logs the incoming tuple entries (useful for debugging).
   * <p>
   * Example of call: <code>pipe = new Each(pipe, new FunctionUtils.LogFunction("my_prefix"), Fields.RESULTS);</code>
   */
  @SuppressWarnings("rawtypes")
  public static class LogFunction extends BaseOperation implements cascading.operation.Function {

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
   * Adds the "icgc_id" (= "project_key.donor_id") field for every incoming tuple.
   * <p>
   * "donor_id" is not known at planning time.
   */
  @SuppressWarnings("rawtypes")
  public static class InsertFunction extends BaseOperation implements Function {

    /**
     * Very basic for now, possibly offer more overloadings for transform()
     */
    public interface Transformable {
      String tranform(String value);
    }

    private final Fields originalField;

    private final Optional<Transformable> transformable;

    public InsertFunction(Fields originalField, Fields newField) {
      this(originalField, newField, null);
    }

    public InsertFunction(Fields originalField, Fields newField, Transformable transformable) {
      super(0, newField);
      checkArgument(originalField != null && originalField.size() == 1);
      checkArgument(newField != null && newField.size() == 1);
      this.originalField = checkNotNull(originalField);
      this.transformable = transformable == null ? Optional.<Transformable> absent() : Optional.of(transformable);
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      TupleEntry entry = functionCall.getArguments();
      String value = entry.getString(originalField);
      String newValue = transformable.isPresent() ? transformable.get().tranform(value) : value;
      functionCall.getOutputCollector().add(new Tuple(newValue));
    }
  }
}
