/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.validation.visitor;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.ValueType;
import org.icgc.dcc.validation.ErrorCodeRegistry;
import org.icgc.dcc.validation.InternalFlowPlanningVisitor;
import org.icgc.dcc.validation.InternalPlanElement;
import org.icgc.dcc.validation.PlannerException;
import org.icgc.dcc.validation.cascading.ValidationFields;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

/**
 * Creates {@code PlanElement}s for validating the {@code ValueType} of a {@code Field}.
 */
public class ValueTypePlanningVisitor extends InternalFlowPlanningVisitor {

  private static final String NAME = "valueType";

  private static final String DISPLAY_NAME = "value type";

  private static final int CODE = 499;

  private static final String MESSAGE = "invalid value type for value %s for field %s. Expected: %s";

  static {
    ErrorCodeRegistry.get().register(CODE, MESSAGE);
  }

  @Override
  public void visit(Field field) {
    // No need to verify ValueType.TEXT since everything can be a String...
    if(field.getValueType() != ValueType.TEXT) {
      collect(new ValueTypePlanElement(field));
    }
  }

  private class ValueTypePlanElement implements InternalPlanElement {

    private final Field field;

    private final ValueType type;

    private ValueTypePlanElement(Field field) {
      this.field = field;
      this.type = field.getValueType();
    }

    @Override
    public String describe() {
      return String.format("%s[%s:%s]", NAME, field.getName(), type);
    }

    @Override
    public Pipe extend(Pipe pipe) {
      return new Each(pipe, new ValidationFields(field.getName()), new ValueTypeFunction(), Fields.REPLACE);
    }

    @SuppressWarnings("rawtypes")
    private final class ValueTypeFunction extends BaseOperation implements Function {

      public ValueTypeFunction() {
        super(2, Fields.ARGS);
      }

      @Override
      public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
        TupleEntry arguments = functionCall.getArguments();
        String value = arguments.getString(0);
        try {
          Object parsedValue = parse(value);
          functionCall.getOutputCollector().add(new Tuple(parsedValue, ValidationFields.state(arguments)));
        } catch(IllegalArgumentException e) {
          Object fieldName = arguments.getFields().get(0);
          ValidationFields.state(arguments).reportError(CODE, value, fieldName, type);
          functionCall.getOutputCollector().add(arguments);
        }
      }

      private Object parse(String value) {
        switch(type) {
        case DATETIME:
          throw new PlannerException(DISPLAY_NAME + " " + ValueType.DATETIME + " is not supported at the moment");
        case DECIMAL:
          return Double.valueOf(value);
        case INTEGER:
          return Long.valueOf(value);
        case TEXT:
          throw new PlannerException(DISPLAY_NAME + " " + ValueType.TEXT + " should not be validated");
        default:
          throw new PlannerException("unknown " + type + " " + DISPLAY_NAME);
        }
      }
    }
  }
}
