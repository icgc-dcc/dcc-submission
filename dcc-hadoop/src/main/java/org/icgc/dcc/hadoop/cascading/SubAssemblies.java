/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.hadoop.cascading;

import static cascading.tuple.Fields.ALL;
import static cascading.tuple.Fields.ARGS;
import static cascading.tuple.Fields.REPLACE;
import static cascading.tuple.Fields.RESULTS;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Iterables.transform;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.util.Optionals.ABSENT_STRING;
import static org.icgc.dcc.core.util.Strings2.EMPTY_STRING;
import static org.icgc.dcc.hadoop.cascading.Fields2.checkFieldsCardinalityOne;
import static org.icgc.dcc.hadoop.cascading.Fields2.cloneFields;
import static org.icgc.dcc.hadoop.cascading.Fields2.getFieldNames;
import static org.icgc.dcc.hadoop.cascading.Fields2.getRedundantFieldCounterparts;
import static org.icgc.dcc.hadoop.cascading.Fields2.keyValuePair;
import static org.icgc.dcc.hadoop.cascading.Fields2.swapTwoFields;
import static org.icgc.dcc.hadoop.cascading.TupleEntries.contains;
import static org.icgc.dcc.hadoop.cascading.TupleEntries.getFirstInteger;
import static org.icgc.dcc.hadoop.cascading.TupleEntries.toJson;
import static org.icgc.dcc.hadoop.cascading.Tuples2.isNullTuple;
import static org.icgc.dcc.hadoop.cascading.Tuples2.nestValue;
import static org.icgc.dcc.hadoop.cascading.Tuples2.setFirstLong;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import lombok.experimental.Builder;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.hadoop.cascading.operation.BaseBuffer;
import org.icgc.dcc.hadoop.cascading.operation.BaseFunction;

import cascading.flow.FlowProcess;
import cascading.operation.BufferCall;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.operation.OperationCall;
import cascading.pipe.Each;
import cascading.pipe.Every;
import cascading.pipe.HashJoin;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.AggregateBy;
import cascading.pipe.assembly.CountBy;
import cascading.pipe.assembly.Discard;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.pipe.assembly.SumBy;
import cascading.pipe.assembly.Unique;
import cascading.pipe.joiner.InnerJoin;
import cascading.pipe.joiner.Joiner;
import cascading.pipe.joiner.LeftJoin;
import cascading.pipe.joiner.OuterJoin;
import cascading.pipe.joiner.RightJoin;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleEntryCollector;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;

/**
 * Useful sub-assemblies.
 * <p>
 * TODO: separate {@link AggregateBy}s.
 */
@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class SubAssemblies {

  /**
   * TODO
   */
  public static class NamingPipe extends SubAssembly {

    /**
     * Preferred if predictable.
     */
    public NamingPipe(Enum<?> instance, Pipe pipe) {
      this(instance.name(), pipe);
    }

    /**
     * TODO
     */
    public NamingPipe(String name, Pipe pipe) {
      setTails(new Pipe(name, pipe));
    }

  }

  public static class Nest extends BaseFunction<Void> {

    public Nest(HasSingleResultField subAssembly) {
      super(checkFieldsCardinalityOne(subAssembly.getResultField()));
    }

    @Override
    public void operate(
        @SuppressWarnings("rawtypes") FlowProcess flowProcess,
        FunctionCall<Void> functionCall) {

      functionCall
          .getOutputCollector()
          .add(nestValue(
              TupleEntries.clone(
                  functionCall.getArguments())));
    }

  }

  /**
   * Prints JSON representation of {@link Tuple}s.
   */
  public static class TupleEntriesLogger extends SubAssembly {

    public TupleEntriesLogger(Pipe pipe) {
      this(ABSENT_STRING, pipe);
    }

    public TupleEntriesLogger(String prefix, Pipe pipe) {
      this(Optional.of(prefix), pipe);
    }

    private TupleEntriesLogger(final Optional<String> prefix, Pipe pipe) {
      setTails(new Each(
          pipe,
          new BaseFunction<Void>(ARGS) {

            @Override
            public void operate(
                @SuppressWarnings("rawtypes") FlowProcess flowProcess,
                FunctionCall<Void> functionCall) {
              val entry = functionCall.getArguments();
              log.info(

                  // Optionally prefix it
                  (prefix.isPresent() ? prefix.get() : EMPTY_STRING)

                      // Pretty json string
                      + toJson(entry));

              functionCall.getOutputCollector().add(entry);
            }

          }

      ));
    }

  }

  /**
   * TODO
   * <p>
   * Only applicable for one {@link Fields} for now.
   */
  public static class NullReplacer<T> extends SubAssembly {

    public NullReplacer(Fields targetFields, NullReplacing<T> nullReplacing, Pipe pipe) {
      setTails(new Each(
          pipe,
          checkFieldsCardinalityOne(targetFields),
          getFunction(nullReplacing),
          REPLACE));
    }

    private static <T> Function<Void> getFunction(final NullReplacing<T> nullReplacing) {
      return new BaseFunction<Void>(ARGS) {

        @Override
        public void operate(
            @SuppressWarnings("rawtypes") FlowProcess flowProcess,
            FunctionCall<Void> functionCall) {
          checkFieldsCardinalityOne(functionCall.getArgumentFields());
          val tuple = functionCall.getArguments().getTuple();
          functionCall
              .getOutputCollector()
              .add(isNullTuple(tuple) ?

                  // Use replacement value
                  nestValue(checkNotNull(nullReplacing.get())) :

                  // Leave it unchanged
                  tuple);
        }
      };

    }

    /**
     * Returns a non-null replacement value for nulls. That the value is non-null will be checked for at runtime.
     */
    public static interface NullReplacing<T> extends Supplier<T>, Serializable {}

    /**
     * Specialized version of {@link NullReplacer} that replaces nulls with an empty {@link Tuple}.
     */
    public static class EmptyTupleNullReplacer extends SubAssembly {

      public EmptyTupleNullReplacer(Fields targetFields, Pipe pipe) {
        setTails(new NullReplacer<Tuple>(
            checkFieldsCardinalityOne(targetFields),
            new NullReplacing<Tuple>() {

              @Override
              public Tuple get() {
                return new Tuple();
              }

            },
            pipe));
      }

    }

  }

  /**
   * TODO: find better name?
   * <p>
   * TODO: add automatic ordering/reordering of fields
   */
  public static class Transformerge<T> extends SubAssembly {

    /**
     * TODO: builder like {@link ReadableHashJoin}.
     */
    public Transformerge(Iterable<T> iterable, com.google.common.base.Function<T, Pipe> function) {
      setTails(

      //
      new Merge(
          toArray(
              transform(
                  iterable,
                  function),
              Pipe.class)));
    }
  }

  /**
   * Insert-equivalent to discard's {@link Discard} sub-assembly.
   * <p>
   * TODO: more than one field (recursively)?
   * <p>
   * TODO: builder like {@link ReadableHashJoin}.
   */
  public static class Insert extends SubAssembly {

    public Insert(Entry<Fields, Object> keyValuePair, Pipe pipe) { // TODO: vararg entries
      setTails(

      //
      new Each(
          pipe,
          new cascading.operation.Insert(
              checkFieldsCardinalityOne(
              keyValuePair.getKey()),
              keyValuePair.getValue()),
          ALL));
    }

  }

  /**
   * TODO
   */
  public static class GroupBy extends SubAssembly {

    public GroupBy(GroupByData groupByData) {
      // TODO: add checks on cardinalities
      setTails(

      //
      groupByData.secondarySortFields != null && groupByData.secondarySortFields.isPresent() ? // TODO: how to assign
                                                                                               // absent() with
                                                                                               // @Value...
      new cascading.pipe.GroupBy(
          groupByData.pipe,
          groupByData.groupByFields,
          groupByData.secondarySortFields.get()) :
          new cascading.pipe.GroupBy(
              groupByData.pipe,
              groupByData.groupByFields));
    }

    @Value
    @Builder
    public static class GroupByData {

      Pipe pipe;
      Fields groupByFields;
      Optional<Fields> secondarySortFields; // TODO: how to assign absent() with @Value...

    }

  }

  /**
   * TODO
   */
  public static class ReadableCountBy extends CountBy {

    public ReadableCountBy(
        @NonNull final String name,
        final CountByData countByData) {
      // TODO: add checks on cardinalities

      super(
          name,
          countByData.pipe,
          countByData.countByFields,
          countByData.resultCountField);
    }
  }

  /**
   * For a count by in which there are few groups (fitting in memory and therefore not requiring a sort phase).
   * <p>
   * TODO: as {@link AggregateBy}?
   */
  public static class HashCountBy extends SubAssembly {

    private static final Fields TEMPORARY_PARTIAL_COUNT_FIELD = new Fields("_partial_count");

    public HashCountBy(CountByData data) {
      // TODO: add checks on cardinalities
      setTails(

      new SumBy(
          new Each(

              // Must insert a dummy value in order to use REPLACE further down
              new Insert(
                  keyValuePair(TEMPORARY_PARTIAL_COUNT_FIELD, null),
                  data.pipe),

              TEMPORARY_PARTIAL_COUNT_FIELD // Order matters
                  .append(data.countByFields),
              getFunction(),
              REPLACE),

          data.countByFields,
          TEMPORARY_PARTIAL_COUNT_FIELD,
          data.resultCountField,
          long.class));

    }

    private static Function<HashCountByContext> getFunction() {
      return new BaseFunction<HashCountByContext>(ARGS) {

        final long INITIAL_COUNT = 0L;
        boolean flushed = false;

        @Override
        public void operate(
            @SuppressWarnings("rawtypes") FlowProcess flowProcess,
            FunctionCall<HashCountByContext> functionCall) {

          val context = lazyContext(functionCall);
          val tuple = functionCall.getArguments().getTupleCopy(); // MUST use a copy
          val counts = context.getCounts();

          counts.put(
              tuple,
              (counts.containsKey(tuple) ?
                  counts.get(tuple) :
                  INITIAL_COUNT)
              + 1); // Increment

          // Emit nothing here (in flush instead)
        }

        /**
         * See https://groups.google.com/forum/#!topic/cascading-user/VDdyGY04vlg
         */
        @Override
        public void flush(
            @SuppressWarnings("rawtypes") FlowProcess flowProcess,
            OperationCall<HashCountByContext> operationCall) {

          // Safety net to ensure flush() only happens once
          if (!flushed) {
            log.info("First flushing");
            flushed = true;
          } else {
            log.warn("Flushing is only expected to happen once...");
            return;
          }

          val context = operationCall.getContext();
          if (context != null) {
            for (val entry : context.counts.entrySet()) {
              context
                  .getOutputCollector() // Cached from #operate()
                  .add(
                      setFirstLong(
                          new Tuple(entry.getKey()), // Create copy
                          entry.getValue()));
            } // Else emit nothing
          }
        }

        private final HashCountByContext lazyContext(
            @NonNull final FunctionCall<HashCountByContext> functionCall) {
          HashCountByContext context = functionCall.getContext();
          if (context == null) {
            context = new HashCountByContext(
                functionCall.getOutputCollector());
            functionCall.setContext(context);
          }

          return context;
        }

      };
    }

    @Value
    private static class HashCountByContext {

      Map<Tuple, Long> counts = new HashMap<Tuple, Long>();
      TupleEntryCollector outputCollector;

    }

  }

  /**
   * See https://gist.github.com/ceteri/4459908.
   * <p>
   * TODO: add checks on fields cardinality and field sets (has to be consistent)
   */
  public static class UniqueCountBy extends ReadableCountBy {

    public UniqueCountBy(
        @NonNull final String name,
        final UniqueCountByData data) {
      super(name, CountByData.builder()

          .pipe(

              // Remove duplicates *before* count by
              new Unique( // TODO: automatically retains?

                  //
                  new Retain(
                      data.pipe,
                      data.uniqueFields),

                  ALL))

          .countByFields(data.countByFields)
          .resultCountField(checkFieldsCardinalityOne(data.resultCountField))

          .build());
    }

    @Value
    @Builder
    public static class UniqueCountByData {

      Pipe pipe;
      Fields uniqueFields;
      Fields countByFields;
      Fields resultCountField;

    }

  }

  /**
   * TODO: generalize to CoGroup as well.
   */
  public static class ReadableHashJoin extends SubAssembly {

    public ReadableHashJoin(JoinData joinData) {
      // TODO: add checks on fields cardinalities
      validateJoiner(joinData.joiner);

      setTails(joinData.hasJoinFieldsCollision() ?
          new Discard(
              new HashJoin(
                  joinData.leftPipe,
                  joinData.leftJoinFields,
                  new Rename( // Rename right side since this could be a left join
                      joinData.rightPipe,
                      joinData.rightJoinFields,
                      joinData.getTemporaryRightJoinFields()),
                  joinData.getTemporaryRightJoinFields(),
                  joinData.joiner),
              joinData.getTemporaryRightJoinFields()) :
          new HashJoin(
              joinData.leftPipe,
              joinData.leftJoinFields,
              joinData.rightPipe,
              joinData.rightJoinFields,
              joinData.joiner));
    }

    /**
     * "Developers should thoroughly understand the limitations of this class"
     * (http://docs.cascading.org/cascading/2.5/userguide/htmlsingle/#N20276).
     * <p>
     * Note that it does work locally.
     */
    private void validateJoiner(@NonNull final Joiner joiner) {
      checkArgument(
          !(joiner instanceof RightJoin || joiner instanceof OuterJoin),
          "Cannot use a hash join in combination with a right or full outer join (see Cascading documentation)");
    }

    public static class JoinData { // TODO: add integrity check (fields cardinalities, ...)

      private Joiner joiner;

      private Pipe leftPipe;
      private Fields leftJoinFields;

      private Pipe rightPipe;
      private Fields rightJoinFields;

      public static final class JoinDataBuilder {

        private final JoinData joinData = new JoinData();

        public JoinDataBuilder innerJoin() {
          return setJoiner(new InnerJoin());
        }

        public JoinDataBuilder leftJoin() {
          return setJoiner(new LeftJoin());
        }

        public JoinDataBuilder rightJoin() {
          return setJoiner(new RightJoin());
        }

        public JoinDataBuilder outerJoin() {
          return setJoiner(new OuterJoin());
        }

        private JoinDataBuilder setJoiner(@NonNull final Joiner joiner) {
          checkState(
              joinData.joiner == null,
              "Joiner is already set: '%s'", joiner);
          joinData.joiner = joiner;
          return this;
        }

        public JoinDataBuilder leftPipe(@NonNull final Pipe leftPipe) {
          checkState(
              joinData.leftPipe == null,
              "Left pipe is already set: '%s'", leftPipe);
          joinData.leftPipe = leftPipe;
          return this;
        }

        public JoinDataBuilder rightPipe(@NonNull final Pipe rightPipe) {
          checkState(
              joinData.rightPipe == null,
              "Right pipe is already set: '%s'", rightPipe);
          joinData.rightPipe = rightPipe;
          return this;
        }

        public JoinDataBuilder joinFields(@NonNull final Fields joinFields) {
          leftJoinFields(cloneFields(joinFields));
          rightJoinFields(cloneFields(joinFields));
          return this;
        }

        public JoinDataBuilder leftJoinFields(@NonNull final Fields leftJoinFields) {
          checkState(
              joinData.leftJoinFields == null,
              "Left join fields are already set: '%s'", leftJoinFields);
          joinData.leftJoinFields = leftJoinFields;
          return this;
        }

        public JoinDataBuilder rightJoinFields(@NonNull final Fields rightJoinFields) {
          checkState(
              joinData.rightJoinFields == null,
              "Right join fields are already set: '%s'", rightJoinFields);
          joinData.rightJoinFields = rightJoinFields;
          return this;
        }

        public JoinData build() {
          checkNotNull(joinData.joiner);
          checkNotNull(joinData.leftPipe);
          checkNotNull(joinData.leftJoinFields);
          checkNotNull(joinData.rightPipe);
          checkNotNull(joinData.rightJoinFields);
          return joinData;
        }

      }

      public static final JoinDataBuilder builder() {
        return new JoinDataBuilder();
      }

      public boolean hasJoinFieldsCollision() {
        return leftJoinFields.equals(rightJoinFields);
      }

      public Fields getTemporaryRightJoinFields() {
        return getRedundantFieldCounterparts(rightJoinFields);
      }

    }

  }

  /**
   * TODO: cascading pre-defined buffer? look into cascading.operation.aggregator.Sum
   */
  public static class Sum extends SubAssembly {

    public Sum(Pipe pipe, Fields preCountField) {
      setTails(new Every(
          pipe,
          checkFieldsCardinalityOne(preCountField),
          getBuffer(),
          REPLACE));
    }

    private static BaseBuffer<Void> getBuffer() {
      return new BaseBuffer<Void>(ARGS) {

        @Override
        public void operate(
            @SuppressWarnings("rawtypes") FlowProcess flowProcess,
            BufferCall<Void> bufferCall) {

          long observationCount = 0;
          val entries = bufferCall.getArgumentsIterator();
          while (entries.hasNext()) {
            observationCount += getFirstInteger(entries.next());
          }
          bufferCall.getOutputCollector().add(new Tuple(observationCount));
        }

      };
    }

  }

  @Value
  @Builder
  public static class CountByData {

    Pipe pipe;
    Fields countByFields;
    Fields resultCountField;

  }

  public static class TransposeBuffer<T> extends BaseBuffer<Void> {

    private final Fields futureFieldsField;
    private final Fields futureValuesField;
    private final T defaultValue;

    public TransposeBuffer(Fields transpositionFields, Fields futureFields, Fields futureValues, T defaultValue) {
      super(transpositionFields);
      this.futureFieldsField = futureFields;
      this.futureValuesField = futureValues;
      this.defaultValue = defaultValue;
    }

    @Override
    public void operate(
        @SuppressWarnings("rawtypes") FlowProcess flowProcess,
        BufferCall<Void> bufferCall) {

      val entries = bufferCall.getArgumentsIterator();
      bufferCall
          .getOutputCollector()
          .add(transpose(entries));
    }

    private TupleEntry transpose(
        @NonNull final Iterator<TupleEntry> entries) {

      val counts = new HashMap<String, T>();
      while (entries.hasNext()) {
        val entry = entries.next();
        counts.put(
            checkKeysIntegrity(
                counts,
                entry.getString(futureFieldsField)),
            getT(entry, futureValuesField));
      }

      val transpositionTuple = new Tuple();
      for (val code : getFieldNames(fieldDeclaration)) {
        transpositionTuple.add(
            counts.containsKey(code) ?
                counts.get(code) :
                defaultValue);
      }

      return new TupleEntry(
          fieldDeclaration,
          transpositionTuple);
    }

    @SuppressWarnings("unchecked")
    private T getT(
        @NonNull final TupleEntry entry,
        @NonNull final Fields field) {

      return (T) entry.getObject(checkFieldsCardinalityOne(field));
    }

    private static <T> String checkKeysIntegrity(
        @NonNull final Map<String, T> counts,
        @NonNull final String key) {
      checkState(!counts.containsKey(key));

      return key;
    }

  }

  public static class ReorderAllFields extends SubAssembly {

    public ReorderAllFields(
        @NonNull final Pipe pipe,
        @NonNull final Fields orderedFields) {
      setTails(process(pipe, orderedFields));
    }

    private static Each process(
        @NonNull final Pipe pipe,
        @NonNull final Fields orderedFields) {
      return new Each(pipe, orderFields(orderedFields), RESULTS);
    }

  }

  public static class ReorderFields extends SubAssembly {

    public ReorderFields(
        @NonNull final Pipe pipe,
        @NonNull final Fields targetFields,
        @NonNull final Fields orderedFields) {
      checkArgument(
          targetFields.size() == orderedFields.size(),
          "Target fields are expected to have the same size as the re-ordered fields: '%s' != '%s'",
          targetFields.size(), orderedFields.size());
      setTails(process(pipe, targetFields, orderedFields));
    }

    private static Each process(
        @NonNull final Pipe pipe,
        @NonNull final Fields targetFields,
        @NonNull final Fields orderedFields) {
      return new Each(pipe, targetFields, orderFields(orderedFields), REPLACE);
    }

  }

  public static class SwapFields extends ReorderFields {

    public SwapFields(
        @NonNull final Pipe pipe,
        @NonNull final Fields targetFields) {
      super(pipe, targetFields, swapTwoFields(targetFields));
    }

  }

  private static Function<Void> orderFields(@NonNull final Fields orderedFields) {
    return new BaseFunction<Void>(orderedFields) {

      @Override
      public void operate(
          @SuppressWarnings("rawtypes") FlowProcess flowProcess,
          FunctionCall<Void> functionCall) {
        val entry = functionCall.getArguments();

        functionCall
            .getOutputCollector()
            .add(getReorderedTuple(
                entry,
                getFieldDeclaration()));
      }

      private Tuple getReorderedTuple(
          @NonNull final TupleEntry entry,
          @NonNull final Fields orderedFields) {
        val tuple = new Tuple();
        for (val fieldName : getFieldNames(orderedFields)) {
          checkState(contains(entry, fieldName),
              "Expecting field '%s' to be present within '%s'", fieldName, entry);
          tuple.add(entry.getObject(fieldName));
        }

        return tuple;
      }

    };
  }

}
