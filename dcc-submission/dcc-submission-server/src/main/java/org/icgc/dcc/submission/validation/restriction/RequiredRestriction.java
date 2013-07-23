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
package org.icgc.dcc.submission.validation.restriction;

import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.validation.FlowType;
import org.icgc.dcc.submission.validation.InternalPlanElement;
import org.icgc.dcc.submission.validation.PlanElement;
import org.icgc.dcc.submission.validation.RestrictionType;
import org.icgc.dcc.submission.validation.RestrictionTypeSchema;
import org.icgc.dcc.submission.validation.ValidationErrorCode;
import org.icgc.dcc.submission.validation.cascading.ValidationFields;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.TupleEntry;

public class RequiredRestriction implements InternalPlanElement {

  public static final String NAME = "required";// TODO: create enum for valid Restriction types?

  public static final String ACCEPT_MISSING_CODE = "acceptMissingCode";

  private final String field;

  private final boolean acceptMissingCode;

  protected RequiredRestriction(String field, boolean acceptMissingCode) {
    this.field = field;
    this.acceptMissingCode = acceptMissingCode;
  }

  @Override
  public String describe() {
    return String.format("%s[%s]", NAME, field);
  }

  @Override
  public Pipe extend(Pipe pipe) {
    return new Each(pipe, new ValidationFields(field), new SpecifiedFunction(this.isAcceptMissingCode()),
        Fields.REPLACE);
  }

  private boolean isAcceptMissingCode() {
    return acceptMissingCode;
  }

  public static class Type implements RestrictionType {

    private final RestrictionTypeSchema schema = new RestrictionTypeSchema();

    @Override
    public String getType() {
      return NAME;
    }

    @Override
    public FlowType flow() {
      return FlowType.INTERNAL;
    }

    @Override
    public boolean builds(String name) {
      return getType().equals(name);
    }

    @Override
    public RestrictionTypeSchema getSchema() {
      return schema;
    }

    @Override
    public PlanElement build(Field field, Restriction restriction) {
      if(restriction.getConfig() == null || restriction.getConfig().get(ACCEPT_MISSING_CODE) == null) {
        return new RequiredRestriction(field.getName(), true);
      }
      Boolean acceptMissingCode = (Boolean) restriction.getConfig().get(ACCEPT_MISSING_CODE);
      return new RequiredRestriction(field.getName(), acceptMissingCode);

    }

  }

  @SuppressWarnings("rawtypes")
  public static class SpecifiedFunction extends BaseOperation implements Function {
    private final boolean acceptMissingCode;

    protected SpecifiedFunction(boolean acceptMissingCode) {
      super(2, Fields.ARGS);
      this.acceptMissingCode = acceptMissingCode;
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      TupleEntry tupleEntry = functionCall.getArguments();
      String value = tupleEntry.getString(0);

      boolean isFieldMissing =
          ValidationFields.state(tupleEntry).isFieldMissing((String) tupleEntry.getFields().get(0));
      // TODO The IF conditions seem to lead to the same thing
      // TODO: DCC-1076 - This should also check for -999 rather than have {@link ForbiddenValuesFunction} do it.
      if(isFieldMissing == false && (value == null || value.isEmpty())) {
        Object fieldName = tupleEntry.getFields().get(0);
        ValidationFields.state(tupleEntry).reportError(ValidationErrorCode.MISSING_VALUE_ERROR, fieldName.toString(),
            value);
      } else if(isFieldMissing == true && !acceptMissingCode) {
        Object fieldName = tupleEntry.getFields().get(0);
        ValidationFields.state(tupleEntry).reportError(ValidationErrorCode.MISSING_VALUE_ERROR, fieldName.toString(),
            value);
      }
      functionCall.getOutputCollector().add(tupleEntry.getTupleCopy());
    }

  }
}
