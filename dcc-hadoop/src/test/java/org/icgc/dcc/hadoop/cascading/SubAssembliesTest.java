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

import static org.icgc.dcc.hadoop.cascading.Cascades.cascadingSerialize;
import static org.icgc.dcc.hadoop.cascading.Fields2.keyValuePair;

import java.util.ArrayList;

import org.icgc.dcc.hadoop.cascading.SubAssemblies.CountByData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.GroupBy.GroupByData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.Insert;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.NamingPipe;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.NullReplacer;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.NullReplacer.NullReplacing;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin.JoinData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.Transformerge;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.TupleEntriesLogger;
import org.junit.Test;

import cascading.pipe.Pipe;
import cascading.tuple.Fields;

import com.google.common.base.Function;

public class SubAssembliesTest {

  private static final NullReplacing<Void> DUMMY = new NullReplacing<Void>() {

    @Override
    public Void get() {
      return null;
    }

  };

  private static final Pipe DUMMY_PIPE = new Pipe("dummypipe");
  private static final Pipe DUMMY_PIPE2 = new Pipe("dummypipe2");
  private static final String DUMMY_NAME = "dummyname";
  private static final Fields DUMMY_FIELD = new Fields("dummyfield");
  private static final Fields DUMMY_FIELD2 = new Fields("dummyfield2");
  private static final JoinData DUMMY_JOIN_DATA =
      JoinData.builder()
          .innerJoin()
          .leftPipe(DUMMY_PIPE)
          .leftJoinFields(DUMMY_FIELD)
          .rightPipe(DUMMY_PIPE2)
          .rightJoinFields(DUMMY_FIELD)
          .build();
  private static final GroupByData DUMMY_GROUP_BY_DATA =
      GroupByData.builder()
          .pipe(DUMMY_PIPE)
          .groupByFields(DUMMY_FIELD)
          .build();
  private static final CountByData DUMMY_COUNT_BY_DATA =
      CountByData.builder()
          .pipe(DUMMY_PIPE)
          .countByFields(DUMMY_FIELD)
          .resultCountField(DUMMY_FIELD2)
          .build();
  private static final Function<Void, Pipe> DUMMY_FUNCTION = new Function<Void, Pipe>() {

    @Override
    public Pipe apply(Void input) {
      return DUMMY_PIPE;
    }

  };

  @Test
  public void test_serialization() {
    cascadingSerialize(new NamingPipe("", null));
    cascadingSerialize(new TupleEntriesLogger(null));
    cascadingSerialize(new NullReplacer<Void>(DUMMY_FIELD, DUMMY, null));
    cascadingSerialize(new Transformerge<Void>(new ArrayList<Void>(), DUMMY_FUNCTION));
    cascadingSerialize(new Insert(keyValuePair(DUMMY_FIELD, ""), null));
    cascadingSerialize(new SubAssemblies.NullReplacer.EmptyTupleNullReplacer(DUMMY_FIELD, null));
    cascadingSerialize(new SubAssemblies.GroupBy(DUMMY_GROUP_BY_DATA));
    cascadingSerialize(new SubAssemblies.ReadableCountBy(DUMMY_NAME, DUMMY_COUNT_BY_DATA));
    cascadingSerialize(new SubAssemblies.ReadableHashJoin(DUMMY_JOIN_DATA));
  }

}
