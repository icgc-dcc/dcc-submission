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
package org.icgc.dcc.validation.restriction;

import java.util.regex.Pattern;

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.Restriction;
import org.icgc.dcc.validation.FlowType;
import org.icgc.dcc.validation.InternalPlanElement;
import org.icgc.dcc.validation.PlanElement;
import org.icgc.dcc.validation.RestrictionType;
import org.icgc.dcc.validation.RestrictionTypeSchema;
import org.icgc.dcc.validation.RestrictionTypeSchema.FieldRestrictionParameter;
import org.icgc.dcc.validation.cascading.ValidationFields;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.TupleEntry;

import com.mongodb.BasicDBObject;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.icgc.dcc.validation.RestrictionTypeSchema.ParameterType.TEXT;
import static org.icgc.dcc.validation.ValidationErrorCode.REGEX_ERROR;

public class RegexRestriction implements InternalPlanElement {

  public static final String NAME = "regex";

  public static final String DESCRIPTION = "Regex that values must match.";

  public static final String PARAM = "pattern";

  private final String field;

  private final String patternString;

  protected RegexRestriction(String field, String patternString) {
    this.field = checkNotNull(field);
    this.patternString = checkNotNull(patternString);
  }

  @Override
  public String describe() {
    return String.format("%s[%s:%s]", NAME, field, patternString);
  }

  @Override
  public Pipe extend(Pipe pipe) {
    return new Each(pipe, new ValidationFields(field), new RegexFunction(field, patternString), Fields.REPLACE);
  }

  public static class Type implements RestrictionType {

    private final RestrictionTypeSchema schema = new RestrictionTypeSchema(//
        new FieldRestrictionParameter(PARAM, TEXT, DESCRIPTION, true));

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
      BasicDBObject restrictionConfig = checkNotNull(restriction.getConfig());
      String patternString = (String) restrictionConfig.get(PARAM);
      return new RegexRestriction(field.getName(), patternString);
    }
  }

  @SuppressWarnings("rawtypes")
  public static class RegexFunction extends BaseOperation implements Function {

    private final String fieldname;

    private final Pattern pattern;

    protected RegexFunction(String fieldname, String patternString) {
      super(2, Fields.ARGS);
      this.fieldname = fieldname;
      this.pattern = Pattern.compile(patternString);
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      TupleEntry tupleEntry = functionCall.getArguments();
      String value = tupleEntry.getString(fieldname);
      if(value != null && pattern.matcher(value).matches() == false) {
        ValidationFields.state(tupleEntry).reportError(REGEX_ERROR, fieldname, value, pattern.pattern());
      }
      functionCall.getOutputCollector().add(tupleEntry.getTupleCopy());
    }
  }
}
