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
import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Iterables.transform;
import static lombok.AccessLevel.PRIVATE;

import java.util.Map.Entry;

import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.Builder;
import cascading.pipe.Each;
import cascading.pipe.HashJoin;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Discard;
import cascading.pipe.joiner.Joiner;
import cascading.tuple.Fields;

import com.google.common.base.Function;

/**
 * Useful sub-assemblies.
 */
@NoArgsConstructor(access = PRIVATE)
public class SubAssemblies {

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
      // TODO: add check field cardinality (from other branch)
      setTails(

      //
      new Each(
          pipe,
          new cascading.operation.Insert(
              keyValuePair.getKey(),
              keyValuePair.getValue()),
          ALL));
    }

  }

  /**
   * TODO: generalize.
   */
  public static class ReadableHashJoin extends SubAssembly {

    public ReadableHashJoin(JoinData joinData) {
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
    public static class JoinData {

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
