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

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.ValueType;
import org.icgc.dcc.validation.InternalFlowPlanningVisitor;
import org.icgc.dcc.validation.InternalPlanElement;
import org.icgc.dcc.validation.PlanningException;
import org.icgc.dcc.validation.ValidationErrorCode;
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

  public static final String NAME = "valueType";

  public static final String DISPLAY_NAME = "value type";

  @Override
  public void visit(Field field) {
    // No need to verify ValueType.TEXT since everything can be a String...
    if(field.getValueType() != ValueType.TEXT) {
      collect(new ValueTypePlanElement(field.getName(), field.getValueType()));
    }
  }

  public static class ValueTypePlanElement implements InternalPlanElement {

    private final String field;

    protected final ValueType type;

    public ValueTypePlanElement(String field, ValueType type) {
      this.field = field;
      this.type = type;
    }

    @Override
    public String describe() {
      return String.format("%s[%s:%s]", NAME, field, type);
    }

    @Override
    public Pipe extend(Pipe pipe) {
      return new Each(pipe, new ValidationFields(field), new ValueTypeFunction(type), Fields.REPLACE);
    }

    @SuppressWarnings("rawtypes")
    public static final class ValueTypeFunction extends BaseOperation implements Function {

      protected final ValueType type;

      public ValueTypeFunction(ValueType type) {
        super(2, Fields.ARGS);
        this.type = type;
      }

      @Override
      public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
        TupleEntry arguments = functionCall.getArguments();
        String value = arguments.getString(0);
        Object parsedValue = null;
        try {
          parsedValue = parse(value);
        } catch(IllegalArgumentException e) {
          Object fieldName = arguments.getFields().get(0);
          ValidationFields.state(arguments).reportError(ValidationErrorCode.VALUE_TYPE_ERROR, value, fieldName, type);
        }
        functionCall.getOutputCollector().add(new Tuple(parsedValue, ValidationFields.state(arguments)));
      }

      private Object parse(String value) {
        if(value == null) {
          return null;
        }
        switch(type) {
        case DATETIME:
          throw new PlanningException(DISPLAY_NAME + " " + ValueType.DATETIME + " is not supported at the moment");
        case DECIMAL:
          Double doubleValue = Double.valueOf(value);
          if(doubleValue == Double.POSITIVE_INFINITY || doubleValue == Double.NEGATIVE_INFINITY) {
            throw new NumberFormatException();
          }
          return doubleValue;
        case INTEGER:
          return Long.valueOf(value);
        case TEXT:
          throw new PlanningException(DISPLAY_NAME + " " + ValueType.TEXT + " should not be validated");
        default:
          throw new PlanningException("unknown " + type + " " + DISPLAY_NAME);
        }
      }
    }
  }
}
