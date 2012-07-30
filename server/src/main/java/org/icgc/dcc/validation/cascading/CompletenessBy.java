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
package org.icgc.dcc.validation.cascading;

import java.util.Arrays;
import java.util.List;

import cascading.flow.FlowProcess;
import cascading.operation.Aggregator;
import cascading.operation.AggregatorCall;
import cascading.operation.BaseOperation;
import cascading.pipe.assembly.AggregateBy;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

@SuppressWarnings("rawtypes")
public class CompletenessBy extends AggregateBy {
  private static final long serialVersionUID = 1L;

  // TODO: move elsewhere + get from dict (DCC-240)
  public static final List<String> MISSING_CODES = Arrays.asList("-777", "-888", "-999");

  private static final int VALUE_OFFSET = 0;

  private static final int NULLS_OFFSET = 0;

  private static final int MISSING_OFFSET = 1;

  private static final int POPULATED_OFFSET = 2;

  public static final String COMPLETENESS = "completeness";

  public static final String NULLS = "nulls";

  public static final String MISSING = "missing";

  public static final String POPULATED = "populated";

  private static final Fields COMPLETENESS_FIELDS = new Fields(NULLS, MISSING, POPULATED);

  public static class CompletenessPartial implements Functor {
    private static final long serialVersionUID = 1L;

    private final Fields declaredFields;

    public CompletenessPartial() {
      this(COMPLETENESS_FIELDS);
    }

    public CompletenessPartial(Fields declaredFields) {
      this.declaredFields = declaredFields;
    }

    @Override
    public Fields getDeclaredFields() {
      return declaredFields;
    }

    @Override
    public Tuple complete(FlowProcess flowProcess, Tuple context) {
      return context;
    }

    @Override
    public Tuple aggregate(FlowProcess flowProcess, TupleEntry tupleEntry, Tuple tuple) {
      if(tuple == null) {
        tuple = new Tuple(0, 0, 0);
      }

      String value = tupleEntry.getString(VALUE_OFFSET);
      if(value == null || value.isEmpty()) {
        tuple.set(NULLS_OFFSET, tuple.getInteger(NULLS_OFFSET) + 1);
      } else if(isMissingValue(value)) {
        tuple.set(MISSING_OFFSET, tuple.getInteger(MISSING_OFFSET) + 1);
      } else {
        tuple.set(POPULATED_OFFSET, tuple.getInteger(POPULATED_OFFSET) + 1);
      }

      return tuple;
    }

    public boolean isMissingValue(String value) {
      for(String missingCode : MISSING_CODES) {
        if(missingCode.equals(value)) {
          return true;
        }
      }
      return false;
    }
  }

  public static class CompletenessFinal extends BaseOperation<CompletenessFinal.Context> implements
      Aggregator<CompletenessFinal.Context> {
    private static final long serialVersionUID = 1L;

    protected static class Context {

      Integer nulls = 0;

      Integer missing = 0;

      Integer populated = 0;

      public Context reset() {
        nulls = 0;
        missing = 0;
        populated = 0;
        return this;
      }
    }

    public CompletenessFinal(Fields fieldDeclaration) {
      super(COMPLETENESS_FIELDS.size(), fieldDeclaration);
    }

    @Override
    public void start(FlowProcess flowProcess, AggregatorCall<Context> aggregatorCall) {
      if(aggregatorCall.getContext() != null) {
        aggregatorCall.getContext().reset();
      } else {
        aggregatorCall.setContext(new Context());
      }
    }

    @Override
    public void aggregate(FlowProcess flowProcess, AggregatorCall<Context> aggregatorCall) {
      TupleEntry tupleEntry = aggregatorCall.getArguments();
      Context context = aggregatorCall.getContext();
      context.nulls += tupleEntry.getInteger(NULLS_OFFSET);
      context.missing += tupleEntry.getInteger(MISSING_OFFSET);
      context.populated += tupleEntry.getInteger(POPULATED_OFFSET);
    }

    @Override
    public void complete(FlowProcess flowProcess, AggregatorCall<Context> aggregatorCall) {
      Context context = aggregatorCall.getContext();
      aggregatorCall.getOutputCollector().add(new Tuple(context.nulls, context.missing, context.populated));
    }
  }

  public CompletenessBy(Fields valueField, Fields minField) {
    super(valueField, new CompletenessPartial(minField), new CompletenessFinal(minField));
  }
}
