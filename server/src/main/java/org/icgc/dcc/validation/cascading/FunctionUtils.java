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

import static com.google.common.base.Preconditions.checkNotNull;

import org.icgc.dcc.data.schema.Schema;
import org.icgc.dcc.data.schema.ValueSchema;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.FunctionCall;
import cascading.tuple.Fields;
import cascading.tuple.TupleEntry;

import com.google.common.base.Function;

/**
 * Utility class for working with cascading {@code Function} objects.
 */
public class FunctionUtils {
  private FunctionUtils() {
    // Prevent construction
  }

  public final static class AlternativePrefixFunction implements Function<Schema, String> {

    private final String prefix;

    private final String sep;

    public AlternativePrefixFunction(String prefix, String sep) {
      this.prefix = checkNotNull(prefix);
      this.sep = checkNotNull(sep);
    }

    @Override
    public String apply(Schema schema) {
      final boolean value = ValueSchema.isValueType(schema.getType());
      if(value) {
        ValueSchema valueSchema = schema.asValue();

        String alternativePrefix = valueSchema.getFileSchema();
        String prefix = (alternativePrefix != null ? alternativePrefix : this.prefix);
        String fieldName = valueSchema.getFileSchemaField();

        // Compose fully qualified field name
        return prefix + sep + fieldName;
      }

      return FieldsUtils.prefix(prefix, sep, schema.getName());
    }

  };

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
}
