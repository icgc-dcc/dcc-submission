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
package org.icgc.dcc.submission.validation.primary.restriction;

import static com.google.common.base.Preconditions.checkState;

import org.icgc.dcc.submission.core.report.ErrorType;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.validation.cascading.ValidationFields;
import org.icgc.dcc.submission.validation.primary.core.FlowType;
import org.icgc.dcc.submission.validation.primary.core.PlanElement;
import org.icgc.dcc.submission.validation.primary.core.RestrictionType;
import org.icgc.dcc.submission.validation.primary.core.RestrictionTypeSchema;
import org.icgc.dcc.submission.validation.primary.core.RestrictionTypeSchema.FieldRestrictionParameter;
import org.icgc.dcc.submission.validation.primary.core.RestrictionTypeSchema.ParameterType;
import org.icgc.dcc.submission.validation.primary.core.RowBasedPlanElement;
import org.icgc.dcc.submission.validation.primary.visitor.ValueTypePlanningVisitor;

import com.mongodb.DBObject;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.TupleEntry;

/**
 * Must happen after {@link ValueTypePlanningVisitor} to ensure data types are correct to begin with.
 */
public class RangeFieldRestriction implements RowBasedPlanElement {

  public static final String NAME = "range";

  public static final String MIN = "min";

  public static final String MAX = "max";

  private final String field;

  private final Number min;

  private final Number max;

  protected RangeFieldRestriction(String field, Number min, Number max) {
    this.field = field;
    this.min = min;
    this.max = max;
  }

  @Override
  public String describe() {
    return String.format("%s[%d-%d]", NAME, min, max);
  }

  @Override
  public Pipe extend(Pipe pipe) {
    return new Each(pipe, new ValidationFields(field), new RangeFunction(min, max), Fields.REPLACE);
  }

  public static class Type implements RestrictionType {

    private final RestrictionTypeSchema schema = new RestrictionTypeSchema(//
        new FieldRestrictionParameter(MIN, ParameterType.NUMBER, "minimum value (inclusive)"), //
        new FieldRestrictionParameter(MAX, ParameterType.NUMBER, "maximum value (inclusive)"));

    @Override
    public String getType() {
      return NAME;
    }

    @Override
    public boolean builds(String name) {
      return getType().equals(name);
    }

    @Override
    public FlowType flowType() {
      return FlowType.ROW_BASED;
    }

    @Override
    public RestrictionTypeSchema getSchema() {
      return schema;
    }

    @Override
    public PlanElement build(String projectKey, Field field, Restriction restriction) {
      DBObject configuration = restriction.getConfig();
      Number min = (Number) configuration.get(MIN);
      Number max = (Number) configuration.get(MAX);
      return new RangeFieldRestriction(field.getName(), min, max);
    }

  }

  @SuppressWarnings("rawtypes")
  public static class RangeFunction extends BaseOperation implements Function {

    private final Number min;

    private final Number max;

    protected RangeFunction(Number min, Number max) {
      super(2, Fields.ARGS);
      this.min = min;
      this.max = max;
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      TupleEntry tupleEntry = functionCall.getArguments();
      Object value = tupleEntry.getObject(0);

      Object fieldName = tupleEntry.getFields().get(0);

      if (isValue(value)) { // Nothing to check if there is no value (null or empty string)
        checkState(value instanceof Number, "Value is expected to be a number at this point, instead got '%s'", value);
        Number num = (Number) value;
        if (num.longValue() < this.min.longValue() || num.longValue() > this.max.longValue()) {

          ValidationFields.state(tupleEntry).reportError(ErrorType.OUT_OF_RANGE_ERROR, fieldName.toString(),
              num.longValue(), min.longValue(), max.longValue());
        }
      }

      functionCall.getOutputCollector().add(tupleEntry.getTupleCopy());
    }

    private boolean isValue(Object value) {
      return value != null && !String.valueOf(value).isEmpty();
    }
  }
}
