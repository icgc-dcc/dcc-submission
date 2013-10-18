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

import static cascading.tuple.Fields.ALL;
import static cascading.tuple.Fields.REPLACE;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.STATIC;
import static lombok.AccessLevel.PROTECTED;
import static org.icgc.dcc.submission.validation.ValidationErrorCode.SCRIPT_ERROR;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;

import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.validation.FlowType;
import org.icgc.dcc.submission.validation.InternalPlanElement;
import org.icgc.dcc.submission.validation.PlanElement;
import org.icgc.dcc.submission.validation.RestrictionType;
import org.icgc.dcc.submission.validation.RestrictionTypeSchema;
import org.icgc.dcc.submission.validation.RestrictionTypeSchema.FieldRestrictionParameter;
import org.icgc.dcc.submission.validation.RestrictionTypeSchema.ParameterType;
import org.icgc.dcc.submission.validation.cascading.ValidationFields;
import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.operation.OperationCall;
import cascading.operation.OperationException;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.TupleEntry;

@Value
@RequiredArgsConstructor(access = PROTECTED)
public class ScriptRestriction implements InternalPlanElement {

  /**
   * Constants.
   */
  public static final String NAME = "script";
  public static final String DESCRIPTION = "MVEL script based restriction used to express procedural constraints";
  public static final String PARAM = "script";

  /**
   * Configuration.
   */
  private final String field;
  private final String script;

  @Override
  public String describe() {
    return format("%s[%s:%s]", NAME, field, script);
  }

  @Override
  public Pipe extend(Pipe pipe) {
    val fields = new ValidationFields(ALL);
    val function = new ScriptFunction(script);

    return new Each(pipe, fields, function, REPLACE);
  }

  public static class Type implements RestrictionType {

    private final RestrictionTypeSchema schema = new RestrictionTypeSchema(
        new FieldRestrictionParameter(PARAM, ParameterType.TEXT, DESCRIPTION, true));

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
      val script = restriction.getConfig().getString(PARAM);

      return new ScriptRestriction(field.getName(), script);
    }

  }

  @SuppressWarnings("rawtypes")
  public static class ScriptFunction extends BaseOperation<ExecutableStatement> implements
      Function<ExecutableStatement> {

    private final String script;

    protected ScriptFunction(String script) {
      super(2, Fields.ARGS);
      this.script = script;
    }

    @Override
    public void prepare(FlowProcess flowProcess, OperationCall<ExecutableStatement> operationCall) {
      ExecutableStatement compiledScript = compile();
      operationCall.setContext(compiledScript);
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall<ExecutableStatement> functionCall) {
      // Shorthands
      val arguments = functionCall.getArguments();
      val compiledScript = functionCall.getContext();

      boolean valid = eval(compiledScript, arguments);
      if (!valid) {
        val state = ValidationFields.state(arguments);
        val fieldName = arguments.getFields().get(0).toString();

        state.reportError(
            SCRIPT_ERROR,
            fieldName,
            "Invalid script result for values: " + arguments,
            script);
      }

      val result = arguments.getTupleCopy();
      functionCall.getOutputCollector().add(result);
    }

    private ExecutableStatement compile() {
      val context = new ParserContext(configure());

      return (ExecutableStatement) MVEL.compileExpression(script, context);
    }

    private ParserConfiguration configure() {
      val config = new ParserConfiguration();
      config.addPackageImport("java.util");

      // TODO: Any more imports that may aid admins?
      for (val method : Math.class.getMethods()) {
        val staticMethod = (method.getModifiers() & STATIC) > 0;
        if (staticMethod) {
          config.addImport(method.getName(), method);
        }
      }

      return config;
    }

    private boolean eval(ExecutableStatement compiledScript, TupleEntry tupleEntry) {
      val result = compiledScript.getValue(null, variableResolverFactory(tupleEntry));

      boolean bool = result instanceof Boolean;
      if (!bool) {
        val message =
            format("Result of script restriction evaluation is not boolean: result = %s, class = %s, script = '%s'",
                result, (result == null ? null : result.getClass()), script);

        throw new OperationException(message);
      }

      return (Boolean) result;
    }

    private VariableResolverFactory variableResolverFactory(TupleEntry tupleEntry) {
      // TODO: Create custom TupleVariableResolver to prevent map creation?
      Map<String, Object> variables = newHashMap();
      for (int i = 0; i < tupleEntry.size(); i++) {
        val fieldName = tupleEntry.getFields().get(i).toString();
        val fieldValue = tupleEntry.getObject(i);

        variables.put(fieldName, fieldValue);
      }

      return new MapVariableResolverFactory(variables);
    }

  }

}
