/**
 * From https://github.com/BertrandDechoux/cascading-deviation - commit 4a0b03470b (very slightly adapted)
 */
package org.icgc.dcc.validation.report;

import cascading.flow.FlowProcess;
import cascading.operation.Aggregator;
import cascading.operation.AggregatorCall;
import cascading.operation.BaseOperation;
import cascading.pipe.Pipe;
import cascading.pipe.assembly.AggregateBy;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

/**
 * Class DeviationBy is used to average and compute the standard deviation of values associated with duplicate keys in a
 * tuple stream.
 * <p/>
 * This strategy based on {@link AggregateBy} is similar to using {@code combiners}, except no sorting or serialization
 * is invoked and results in a much simpler mechanism.
 * <p/>
 * The {@code threshold} value tells the underlying partials functions how many unique key sums and counts to accumulate
 * in the LRU cache, before emitting the least recently used entry.
 * 
 * @see cascading.pipe.assembly.AggregateBy
 */
@SuppressWarnings("rawtypes")
public class DeviationBy extends AggregateBy {
  private static final long serialVersionUID = 1L;

  private static final Fields BIND_FIELDS = new Fields("sum", "sumOfSquare", "count");

  /**
   * Class DeviationPartials is a {@link cascading.pipe.assembly.AggregateBy.Functor} that is used to square, count and
   * sum observed duplicates from the tuple stream.
   * 
   * @see cascading.pipe.assembly.AverageBy
   */
  public static class DeviationPartials implements Functor {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor DeviationPartials creates a new DeviationPartials instance.
     */
    public DeviationPartials() {

    }

    @Override
    public Fields getDeclaredFields() {
      return BIND_FIELDS;
    }

    @Override
    public Tuple aggregate(FlowProcess flowProcess, TupleEntry args, Tuple context) {
      if(context == null) {
        context = Tuple.size(3);
      }
      context.set(0, context.getDouble(0) + args.getDouble(0));
      context.set(1, context.getDouble(1) + Math.pow(args.getDouble(0), 2));
      context.set(2, context.getLong(2) + 1);
      return context;
    }

    @Override
    public Tuple complete(FlowProcess flowProcess, Tuple context) {
      return context;
    }
  }

  /**
   * Class DeviationFinal is used to finalize the operation on the Reduce side of the process. It must be used in tandem
   * with a {@link DeviationPartials} Functor.
   */
  public static class DeviationFinal extends BaseOperation<DeviationFinal.Context> implements
      Aggregator<DeviationFinal.Context> {
    private static final long serialVersionUID = 1L;

    /** Class Context is used to hold intermediate values. */
    protected static class Context {
      double sum;

      double sumOfSquare;

      long count;

      public Context reset() {
        sum = 0;
        sumOfSquare = 0;
        count = 0;
        return this;
      }
    }

    /**
     * Constructs a new instance that returns the aggregates of the values encountered in the given fieldDeclaration
     * field name.
     */
    public DeviationFinal(Fields fieldDeclaration) {
      super(2, fieldDeclaration);

      if(!fieldDeclaration.isSubstitution() && fieldDeclaration.size() != 2) throw new IllegalArgumentException(
          "fieldDeclaration may only declare 2 fields, got: " + fieldDeclaration.size());
    }

    @Override
    public void start(FlowProcess flowProcess, AggregatorCall<Context> aggregatorCall) {
      if(aggregatorCall.getContext() != null) aggregatorCall.getContext().reset();
      else
        aggregatorCall.setContext(new Context());
    }

    @Override
    public void aggregate(FlowProcess flowProcess, AggregatorCall<Context> aggregatorCall) {
      Context context = aggregatorCall.getContext();
      TupleEntry arguments = aggregatorCall.getArguments();

      context.sum += arguments.getDouble(0);
      context.sumOfSquare += arguments.getDouble(1);
      context.count += arguments.getLong(2);
    }

    @Override
    public void complete(FlowProcess flowProcess, AggregatorCall<Context> aggregatorCall) {
      aggregatorCall.getOutputCollector().add(getResult(aggregatorCall));
    }

    private Tuple getResult(AggregatorCall<Context> aggregatorCall) {
      Context context = aggregatorCall.getContext();
      double average = context.sum / context.count;
      double squaredAverage = Math.pow(average, 2);
      double averageOfSquare = context.sumOfSquare / context.count;
      double deviation = Math.sqrt(averageOfSquare - squaredAverage);
      return new Tuple(average, deviation);
    }
  }

  /**
   * Use this constructor when used with a {@link cascading.pipe.assembly.AggregateBy} instance.
   */
  public DeviationBy(Fields valueField, Fields averageAndDeviationFields) {
    super(valueField, new DeviationPartials(), new DeviationFinal(averageAndDeviationFields));
  }

  // ////////////
  public DeviationBy(Pipe pipe, Fields groupingFields, Fields valueField, Fields averageAndDeviationFields) {
    this(null, pipe, groupingFields, valueField, averageAndDeviationFields, 10000);
  }

  public DeviationBy(Pipe pipe, Fields groupingFields, Fields valueField, Fields averageAndDeviationFields,
      int threshold) {
    this(null, pipe, groupingFields, valueField, averageAndDeviationFields, threshold);
  }

  public DeviationBy(String name, Pipe pipe, Fields groupingFields, Fields valueField, Fields averageAndDeviationFields) {
    this(name, pipe, groupingFields, valueField, averageAndDeviationFields, 10000);
  }

  public DeviationBy(String name, Pipe pipe, Fields groupingFields, Fields valueField,
      Fields averageAndDeviationFields, int threshold) {
    this(name, Pipe.pipes(pipe), groupingFields, valueField, averageAndDeviationFields, threshold);
  }

  public DeviationBy(Pipe[] pipes, Fields groupingFields, Fields valueField, Fields averageAndDeviationFields) {
    this(null, pipes, groupingFields, valueField, averageAndDeviationFields, 10000);
  }

  public DeviationBy(Pipe[] pipes, Fields groupingFields, Fields valueField, Fields averageAndDeviationFields,
      int threshold) {
    this(null, pipes, groupingFields, valueField, averageAndDeviationFields, threshold);
  }

  public DeviationBy(String name, Pipe[] pipes, Fields groupingFields, Fields valueField,
      Fields averageAndDeviationFields) {
    this(name, pipes, groupingFields, valueField, averageAndDeviationFields, 10000);
  }

  public DeviationBy(String name, Pipe[] pipes, Fields groupingFields, Fields valueField,
      Fields averageAndDeviationFields, int threshold) {
    super(name, pipes, groupingFields, valueField, new DeviationPartials(), new DeviationFinal(
        averageAndDeviationFields), threshold);
  }
}
