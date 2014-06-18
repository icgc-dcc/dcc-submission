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
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Iterables.transform;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.util.Jackson.toJsonPrettyString;
import static org.icgc.dcc.core.util.Strings2.EMPTY_STRING;
import static org.icgc.dcc.hadoop.cascading.Fields2.checkFieldsCardinalityOne;
import static org.icgc.dcc.hadoop.cascading.TupleEntries.toJson;
import static org.icgc.dcc.hadoop.cascading.Tuples2.isNullTuple;
import static org.icgc.dcc.hadoop.cascading.Tuples2.nestValue;

import java.io.Serializable;
import java.util.Map.Entry;

import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.val;
import lombok.experimental.Builder;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.util.ObjectProviding;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.HashJoin;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Discard;
import cascading.pipe.joiner.Joiner;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

import com.google.common.base.Function;
import com.google.common.base.Optional;

/**
 * Useful sub-assemblies.
 */
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

  /**
   * TODO
   */
  public static class TupleEntriesLogger extends SubAssembly {

    public TupleEntriesLogger(Pipe pipe) {
      this(Optional.<String> absent(), pipe);
    }

    /**
     * TODO
     */
    public TupleEntriesLogger(Optional<String> prefix, Pipe pipe) {
      setTails(new Each(pipe, new Nonce(prefix)));
    }

    @Slf4j
    private static class Nonce extends BaseOperation<Void> implements cascading.operation.Function<Void> {

      private final Optional<String> prefix;

      public Nonce(Optional<String> prefix) {
        super(ARGS);
        this.prefix = prefix;
      }

      @Override
      public void operate(
          @SuppressWarnings("rawtypes") FlowProcess flowProcess,
          FunctionCall<Void> functionCall) {
        val entry = functionCall.getArguments();
        log.info(

            // Optionally prefix it
            (prefix.isPresent() ? prefix.get() : EMPTY_STRING)

                // Pretty json string
                + toJsonPrettyString(toJson(entry)));

        functionCall.getOutputCollector().add(entry);
      }

    }

  }

  /**
   * TODO
   * <p>
   * Only applicable for one {@link Fields} for now.
   */
  public static class NullReplacer extends SubAssembly {

    public NullReplacer(Fields targetFields, NullReplacing nullReplacing, Pipe pipe) {
      setTails(new Each(
          pipe,
          checkFieldsCardinalityOne(targetFields),
          new Nonce(nullReplacing),
          REPLACE));
    }

    /**
     * Returns a non-null replacement value for nulls. That the value is non-null will be checked for at runtime.
     */
    public static interface NullReplacing extends ObjectProviding, Serializable {}

    private static class Nonce extends BaseOperation<Void> implements cascading.operation.Function<Void> {

      private final NullReplacing nullReplacing;

      public Nonce(NullReplacing nullReplacing) {
        super(ARGS);
        this.nullReplacing = nullReplacing;
      }

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
    }

    /**
     * Specialized version of {@link NullReplacer} that replaces nulls with an empty {@link Tuple}.
     */
    public static class EmptyTupleNullReplacer extends SubAssembly {

      public EmptyTupleNullReplacer(Fields targetFields, Pipe pipe) {
        setTails(new NullReplacer(
            checkFieldsCardinalityOne(targetFields),
            new NullReplacing() {

              @Override
              public Object get() {
                return new Tuple();
              }

            },
            pipe));
      }

    }

  }

  /**
   * TODO: find better name?
   */
  public static class Transformerge<T> extends SubAssembly {

    /**
     * TODO: builder like {@link ReadableHashJoin}.
     */
    public Transformerge(Iterable<T> iterable, Function<T, Pipe> function) {
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

    public Insert(Entry<Fields, Object> keyValuePair, Pipe pipe) {
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
  public static class CountBy extends SubAssembly {

    public CountBy(CountByData countByData) {
      // TODO: add checks on cardinalities
      setTails(

      //
      new cascading.pipe.assembly.CountBy(
          countByData.pipe,
          countByData.countByFields,
          countByData.resultField));
    }

    @Value
    @Builder
    public static class CountByData {

      Pipe pipe;
      Fields countByFields;
      Fields resultField;

    }

  }

  /**
   * TODO: generalize.
   */
  public static class ReadableHashJoin extends SubAssembly {

    public ReadableHashJoin(JoinData joinData) {
      // TODO: add checks on cardinalities
      setTails(

      //
      new Discard(

          //
          new HashJoin(
              joinData.leftPipe,
              joinData.leftJoinFields,
              joinData.rightPipe,
              joinData.rightJoinFields,
              joinData.resultFields,
              joinData.joiner),
          joinData.discardFields));
    }

    /**
     * TODO: offer innerJoin(), leftJoin(), ...?
     */
    @Value
    @Builder
    public static class JoinData { // TODO: add integrity check (cardinalities, ...)

      Joiner joiner;

      Pipe leftPipe;
      Fields leftJoinFields;

      Pipe rightPipe;
      Fields rightJoinFields;

      Fields resultFields;
      Fields discardFields; // TODO: derive from result fields rather

    }

  }

}
