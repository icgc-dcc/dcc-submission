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
package org.icgc.dcc.validation.restriction;

import java.util.List;
import java.util.Set;

import org.icgc.dcc.model.dictionary.CodeList;
import org.icgc.dcc.model.dictionary.DictionaryService;
import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.Restriction;
import org.icgc.dcc.model.dictionary.Term;
import org.icgc.dcc.validation.ErrorCodeRegistry;
import org.icgc.dcc.validation.FlowType;
import org.icgc.dcc.validation.InternalPlanElement;
import org.icgc.dcc.validation.PlanElement;
import org.icgc.dcc.validation.RestrictionType;
import org.icgc.dcc.validation.RestrictionTypeSchema;
import org.icgc.dcc.validation.RestrictionTypeSchema.FieldRestrictionParameter;
import org.icgc.dcc.validation.RestrictionTypeSchema.ParameterType;
import org.icgc.dcc.validation.cascading.ValidationFields;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

public class CodeListRestriction implements InternalPlanElement {

  @Inject
  private DictionaryService dictionaries;

  private static final String NAME = "codeList";

  private static final int CODE = 504;

  private final String field;

  private final String codeListName;

  private final Set<String> codes;

  private final Set<String> values;

  private CodeListRestriction(String field, String codeListName) {
    this.field = field;
    this.codeListName = codeListName;
    CodeList codeList = dictionaries.getCodeList(codeListName);
    List<Term> terms = codeList.getTerms();
    codes = Sets.newHashSet(Iterables.transform(terms, new com.google.common.base.Function<Term, String>() {
      @Override
      public String apply(Term term) {
        return term.getCode();
      }
    }));
    values = Sets.newHashSet(Iterables.transform(terms, new com.google.common.base.Function<Term, String>() {
      @Override
      public String apply(Term term) {
        return term.getValue();
      }
    }));
  }

  @Override
  public String describe() {
    return String.format("%s[%s]", NAME, field);
  }

  @Override
  public Pipe extend(Pipe pipe) {
    return new Each(pipe, new ValidationFields(field), new InCodeListFunction(codeListName, codes, values),
        Fields.REPLACE);
  }

  public static class Type implements RestrictionType {

    private final RestrictionTypeSchema schema = new RestrictionTypeSchema(//
        new FieldRestrictionParameter("name", ParameterType.TEXT, "Name of codeList against which to check the value",
            true));

    public Type() {
      ErrorCodeRegistry.get().register(504, "invalid value %s for field %s. Expected code or value from CodeList %s");
    }

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
      String codeListName = restriction.getConfig().getString("name");
      return new CodeListRestriction(field.getName(), codeListName);
    }

  }

  @SuppressWarnings("rawtypes")
  public static class InCodeListFunction extends BaseOperation implements Function {

    private final String codeListName;

    private final Set<String> codes;

    private final Set<String> values;

    private InCodeListFunction(String codeListName, Set<String> codes, Set<String> values) {
      super(2, Fields.ARGS);
      this.codeListName = codeListName;
      this.codes = codes;
      this.values = values;
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      String value = functionCall.getArguments().getString(0);
      if(codes.contains(value) == false && values.contains(value) == false) {
        Object fieldName = functionCall.getArguments().getFields().get(0);
        ValidationFields.state(functionCall.getArguments()).reportError(CODE, value, fieldName, codeListName);
      }
      functionCall.getOutputCollector().add(functionCall.getArguments());
    }

  }

}
