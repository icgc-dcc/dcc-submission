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
package org.icgc.dcc.submission.validation.primary.restriction;

import static cascading.tuple.Fields.ALL;
import static cascading.tuple.Fields.REPLACE;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.lang.String.format;
import static java.lang.reflect.Modifier.STATIC;
import static lombok.AccessLevel.PROTECTED;
import static org.icgc.dcc.submission.core.report.ErrorType.SCRIPT_ERROR;
import static org.icgc.dcc.submission.validation.cascading.ValidationFields.OFFSET_FIELD_NAME;
import static org.icgc.dcc.submission.validation.cascading.ValidationFields.STATE_FIELD_NAME;
import static org.icgc.dcc.submission.validation.cascading.ValidationFields.state;

import java.util.Map;

import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.primary.core.FlowType;
import org.icgc.dcc.submission.validation.primary.core.PlanElement;
import org.icgc.dcc.submission.validation.primary.core.RestrictionType;
import org.icgc.dcc.submission.validation.primary.core.RestrictionTypeSchema;
import org.icgc.dcc.submission.validation.primary.core.RestrictionTypeSchema.FieldRestrictionParameter;
import org.icgc.dcc.submission.validation.primary.core.RestrictionTypeSchema.ParameterType;
import org.icgc.dcc.submission.validation.primary.core.RowBasedPlanElement;
import org.mvel2.MVEL;
import org.mvel2.ParserConfiguration;
import org.mvel2.ParserContext;
import org.mvel2.compiler.ExecutableStatement;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

import com.google.common.base.Joiner;

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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Restriction implementation that supports predicate expressions using the MVEL language.
 * 
 * @see http://mvel.codehaus.org/
 * @see https://github.com/mvel/mvel
 */
@Value
@RequiredArgsConstructor(access = PROTECTED)
public class ScriptRestriction implements RowBasedPlanElement {

  /**
   * Constants.
   */
  public static final String NAME = "script";
  public static final String DESCRIPTION = "MVEL script based restriction used to express procedural constraints";
  public static final String PARAM = "script";
  public static final String PARAM_DESCRIPTION = "description";

  /**
   * Configuration.
   */
  private final String projectKey;
  private final String reportedField;
  private final int number;
  private final String script;

  @Override
  public String describe() {
    return format("%s[%s:%s]", NAME, reportedField, script);
  }

  @Override
  public Pipe extend(Pipe pipe) {
    val fields = ALL;
    val function = new ScriptFunction(projectKey, reportedField, number, script);

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
    public FlowType flowType() {
      return FlowType.ROW_BASED;
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
    public PlanElement build(String projectKey, Field field, Restriction restriction) {
      val number = getNumber(field, restriction);
      val script = restriction.getConfig().getString(PARAM);

      return new ScriptRestriction(projectKey, field.getName(), number, script);
    }

    private static int getNumber(Field field, Restriction restriction) {
      // Use the index as the number
      int number = 0;
      for (val r : field.getRestrictions()) {
        val scriptType = r.getType() == org.icgc.dcc.submission.dictionary.model.RestrictionType.SCRIPT;
        if (scriptType) {
          if (r.equals(restriction)) {
            return number;
          }

          number++;
        }
      }

      throw new RuntimeException("Could not find script restriction " + restriction + " in field " + field);
    }
  }

  public static class InvalidScriptException extends RuntimeException {

    private InvalidScriptException(String message) {
      super(message);
    }

  }

  public static class ScriptFunctionException extends OperationException {

    private ScriptFunctionException(String message, Object... args) {
      super(format(message, args));
    }

  }

  @SuppressWarnings("rawtypes")
  @Slf4j
  public static class ScriptFunction extends BaseOperation<ScriptContext>implements Function<ScriptContext> {

    private static final Joiner.MapJoiner VARIABLE_JOINER = Joiner.on(", ").withKeyValueSeparator(" = ")
        .useForNull("null");

    private final String projectKey;
    private final String reportedField;
    private final int number;
    private final String script;

    protected ScriptFunction(String projectKey, String reportedField, int number, String script) {
      super(2, Fields.ARGS);
      this.projectKey = projectKey;
      this.reportedField = reportedField;
      this.number = number;
      this.script = script;
    }

    @Override
    public void prepare(FlowProcess flowProcess, OperationCall<ScriptContext> operationCall) {
      val context = new ScriptContext(projectKey, script);

      operationCall.setContext(context);
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall<ScriptContext> functionCall) {
      val arguments = functionCall.getArguments();
      val context = functionCall.getContext();
      val state = state(arguments);

      try {
        boolean passed = context.evaluate(arguments);
        if (!passed) {
          val values = context.references(arguments);

          reportError(state, values);
        }
      } catch (Exception e) {
        val errorMessage = format("Error invoking script restriction: '%s', arguments: '%s'",
            e.getMessage(), arguments);
        log.error(errorMessage + ", context: " + context, e);

        reportError(state, errorMessage);
      }

      val result = arguments.getTupleCopy();
      functionCall.getOutputCollector().add(result);
    }

    private void reportError(TupleState state, Map<String, Object> values) {
      val reportedValue = VARIABLE_JOINER.join(values);
      reportError(state, reportedValue);
    }

    private void reportError(TupleState state, String reportedValue) {
      state.reportError(number, SCRIPT_ERROR, reportedField, reportedValue);
    }

  }

  public static class ScriptContext {

    private final String projectKey;
    private final String script;

    @Getter
    private final Map<String, Class<?>> inputs;
    private final ParserContext parserContext;
    private final ExecutableStatement compiledScript;

    public ScriptContext(String projectKey, String script) {
      this.projectKey = projectKey;
      this.script = script;
      this.parserContext = new ParserContext(configuration());
      this.compiledScript = (ExecutableStatement) MVEL.compileExpression(script, parserContext);
      this.inputs = inputs();

      validate();
    }

    public boolean evaluate(TupleEntry tupleEntry) {
      val result = compiledScript.getValue(null, variableResolverFactory(tupleEntry));

      if (!isPredicate(result)) {
        val resultClass = result == null ? null : result.getClass();

        throw new ScriptFunctionException(
            "Result of script restriction evaluation is not boolean: result = %s, class = %s, script = '%s'",
            result, resultClass, script);
      }

      return (Boolean) result;
    }

    public Map<String, Object> references(TupleEntry tupleEntry) {
      val variables = variables();

      for (int i = 0; i < tupleEntry.size(); i++) {
        val fieldName = tupleEntry.getFields().get(i).toString();
        val fieldValue = tupleEntry.getObject(i);

        // Referenced in script
        val referenced = isInput(fieldName);
        if (!referenced) {
          continue;
        }

        variables.put(fieldName, fieldValue);
      }

      return variables;
    }

    private void validate() {
      val returnType = returnType();
      if (!isPredicate(returnType)) {
        throw new InvalidScriptException("Script restriction has non boolean return type: '" + returnType + "'");
      }
    }

    @SuppressWarnings("rawtypes")
    private Class<?> returnType() {
      val returnType = compiledScript.getKnownEgressType();

      return returnType;
    }

    private static ParserConfiguration configuration() {
      val config = new ParserConfiguration();
      config.addPackageImport("java.util");

      for (val method : Math.class.getMethods()) {
        val staticMethod = (method.getModifiers() & STATIC) > 0;
        if (staticMethod) {
          config.addImport(method.getName(), method);
        }
      }

      return config;
    }

    private boolean isInput(String fieldName) {
      return inputs.keySet().contains(fieldName);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Class<?>> inputs() {
      return (Map<String, Class<?>>) (Object) parserContext.getInputs();
    }

    private static boolean isPredicate(Class<?> clazz) {
      return clazz == null || Boolean.class.equals(clazz);
    }

    private static boolean isPredicate(Object result) {
      return result instanceof Boolean;
    }

    private static Map<String, Object> variables(TupleEntry tupleEntry) {
      val variables = variables();
      for (int i = 0; i < tupleEntry.size(); i++) {
        val fieldName = tupleEntry.getFields().get(i).toString();
        val fieldValue = tupleEntry.getObject(i);

        // Skip validation book-keeping
        val internal = fieldName.equals(STATE_FIELD_NAME) || fieldName.equals(OFFSET_FIELD_NAME);
        if (internal) {
          continue;
        }

        variables.put(fieldName, fieldValue);
      }

      return variables;
    }

    private static Map<String, Object> variables() {
      // Preserve order
      return newLinkedHashMap();
    }

    private VariableResolverFactory variableResolverFactory(TupleEntry tupleEntry) {
      val factory = new MapVariableResolverFactory(variables(tupleEntry));
      factory.createVariable("project", projectKey);

      return factory;
    }

  }

}
