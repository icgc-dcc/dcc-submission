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

import cascading.flow.FlowProcess;
import cascading.operation.Aggregator;
import cascading.operation.AggregatorCall;
import cascading.operation.BaseOperation;
import cascading.pipe.assembly.AggregateBy;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

@SuppressWarnings("rawtypes")
public class MinMaxBy extends AggregateBy {
  private static final long serialVersionUID = 1L;

  private static final String MIN = "min";

  private static final String MAX = "max";

  private static final int VALUE_OFFSET = 0;

  private static final int MIN_OFFSET = 0;

  private static final int MAX_OFFSET = 1;

  private static final Fields MIN_MAX_FIELDS = new Fields(MIN, MAX);

  public static class MinMaxPartial implements Functor {
    private static final long serialVersionUID = 1L;

    @Override
    public Fields getDeclaredFields() {
      return MIN_MAX_FIELDS;
    }

    @Override
    public Tuple complete(FlowProcess flowProcess, Tuple context) {
      return context;
    }

    @Override
    public Tuple aggregate(FlowProcess flowProcess, TupleEntry tupleEntry, Tuple tuple) {
      if(tuple == null) {
        tuple = Tuple.size(MIN_MAX_FIELDS.size());
      }

      Object object = tupleEntry.getObject(VALUE_OFFSET);
      if(object != null) {
        double value = tupleEntry.getDouble(VALUE_OFFSET);
        if(tuple.getObject(MIN_OFFSET) == null || value < tuple.getDouble(MIN_OFFSET)) {
          tuple.set(MIN_OFFSET, value);
        }
        if(tuple.getObject(MAX_OFFSET) == null || value > tuple.getDouble(MAX_OFFSET)) {
          tuple.set(MAX_OFFSET, value);
        }
      }

      return tuple;
    }

  }

  public static class MinMaxFinal extends BaseOperation<MinMaxFinal.Context> implements Aggregator<MinMaxFinal.Context> {
    private static final long serialVersionUID = 1L;

    protected static class Context {
      Double min;

      Double max;

      public Context reset() {
        min = null;
        max = null;
        return this;
      }
    }

    public MinMaxFinal(Fields fieldDeclaration) {
      super(MIN_MAX_FIELDS.size(), fieldDeclaration);
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
      if(tupleEntry.getObject(MIN_OFFSET) != null) {
        double min = tupleEntry.getDouble(MIN_OFFSET);
        if(context.min == null || min < context.min) {
          context.min = min;
        }
      }
      if(tupleEntry.getObject(MAX_OFFSET) != null) {
        double max = tupleEntry.getDouble(MAX_OFFSET);
        if(context.max == null || max > context.max) {
          context.max = max;
        }
      }

    }

    @Override
    public void complete(FlowProcess flowProcess, AggregatorCall<Context> aggregatorCall) {
      Context context = aggregatorCall.getContext();
      aggregatorCall.getOutputCollector().add(new Tuple(context.min, context.max));
    }
  }

  public MinMaxBy(Fields valueField, Fields minField) {
    super(valueField, new MinMaxPartial(), new MinMaxFinal(minField));
  }
}
