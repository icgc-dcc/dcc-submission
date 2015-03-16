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
package org.icgc.dcc.submission.reporter.cascading.subassembly;

import static org.icgc.dcc.common.cascading.Flows.connectFlowDef;

import java.net.InetAddress;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.cascading.CascadingContext;
import org.icgc.dcc.common.cascading.Flows;
import org.icgc.dcc.common.core.util.Joiners;
import org.icgc.dcc.common.hadoop.fs.FileSystems;
import org.icgc.dcc.common.hadoop.fs.HadoopUtils;
import org.icgc.dcc.common.hadoop.util.HadoopConstants;

import cascading.flow.Flow;
import cascading.flow.FlowDef;
import cascading.pipe.CoGroup;
import cascading.pipe.HashJoin;
import cascading.pipe.Pipe;
import cascading.pipe.joiner.InnerJoin;
import cascading.pipe.joiner.Joiner;
import cascading.pipe.joiner.LeftJoin;
import cascading.pipe.joiner.OuterJoin;
import cascading.pipe.joiner.RightJoin;
import cascading.tuple.Fields;

import com.google.common.collect.ImmutableMap;

/**
 * <pre>
 *   $ cat /tmp/left # echo -e "l\tjl\nl1\tj1\nl2\tj2l"
 *   l  jl
 *   l1  j1
 *   l2  j2l
 *   
 *   $ cat /tmp/right # echo -e "jr\tr\nj1\tr1\nj2r\tr2"
 *   jr  r
 *   j1  r1
 *   j2r r2
 * </pre>
 */
@Slf4j
public class JoinsBug {

  @RequiredArgsConstructor
  enum Environment {
    LOCAL(CascadingContext.getLocal()),
    DISTRIBUTED(CascadingContext.getDistributed());

    @Getter
    private final CascadingContext context;

    public boolean isLocal() {
      return this == LOCAL;
    }

  }

  enum JoinType {
    HASH, COGROUP
  }

  @RequiredArgsConstructor
  enum JoinerType {
    INNER(new InnerJoin()), LEFT(new LeftJoin()), RIGHT(new RightJoin()), OUTER(new OuterJoin());

    @Getter
    private final Joiner joiner;
  }

  private static final String LEFT_FILE_PATH = "/tmp/left";
  private static final String RIGHT_FILE_PATH = "/tmp/right";

  private static final String LEFT_PIPE_NAME = "left_input";
  private static final String RIGHT_PIPE_NAME = "right_input";

  private static final Pipe LEFT_PIPE = new Pipe(LEFT_PIPE_NAME);
  private static final Pipe RIGHT_PIPE = new Pipe(RIGHT_PIPE_NAME);

  private static final Fields LEFT_JOIN_FIELD = new Fields("jl");
  private static final Fields RIGHT_JOIN_FIELD = new Fields("jr");

  static final boolean HAS_DISTRIBUTED = !isAcros();

  @SneakyThrows
  private static boolean isAcros() {
    return "acroslt".equals(InetAddress.getLocalHost().getHostName());
  }

  public static void main(String[] args) {

    for (val environment : Environment.values()) {
      if (environment.isLocal() || HAS_DISTRIBUTED) {
        for (val joinType : JoinType.values()) {
          for (val joinerType : JoinerType.values()) {
            process(environment, joinType, joinerType);
          }
        }
      }
    }
  }

  private static void process(Environment environment, JoinType joinType, JoinerType joinerType) {
    Pipe join = getJoinPipe(joinType, joinerType);
    CascadingContext context = environment.getContext();

    val flowDef = Flows.getFlowDef(JoinsBug.class);
    flowDef.addSource(
        LEFT_PIPE_NAME,
        context.getTaps().getNoCompressionTsvWithHeader(LEFT_FILE_PATH));
    flowDef.addSource(
        RIGHT_PIPE_NAME,
        context.getTaps().getNoCompressionTsvWithHeader(RIGHT_FILE_PATH));

    val outputDirFilePath = "/tmp/joins-" + getDescription(environment, joinType, joinerType);
    flowDef.addTailSink(join, context.getTaps().getNoCompressionTsvWithHeader(outputDirFilePath));

    connect(environment, flowDef).complete();
    printFile(
        getLines(environment, LEFT_FILE_PATH),
        LEFT_PIPE_NAME);
    printFile(
        getLines(environment, RIGHT_FILE_PATH),
        RIGHT_PIPE_NAME);
    printFile(
        getLines(environment, outputDirFilePath),
        getDescription(environment, joinType, joinerType));
    log.info("done: " + outputDirFilePath);
  }

  private static Pipe getJoinPipe(JoinType joinType, JoinerType joinerType) {
    return joinType == JoinType.HASH ?
        new HashJoin(LEFT_PIPE, LEFT_JOIN_FIELD, RIGHT_PIPE, RIGHT_JOIN_FIELD, joinerType.getJoiner()) :
        new CoGroup(LEFT_PIPE, LEFT_JOIN_FIELD, RIGHT_PIPE, RIGHT_JOIN_FIELD, joinerType.getJoiner());
  }

  static Flow<?> connect(Environment environment, final FlowDef flowDef) {
    val flowConnector = environment.getContext().getConnectors().getFlowConnector(
        environment.isLocal() ?
            ImmutableMap.of(
                CommonConfigurationKeys.FS_DEFAULT_NAME_KEY, "file:///",
                HadoopConstants.MR_JOBTRACKER_ADDRESS_KEY, "localhost:8021") :
            ImmutableMap.of(
                CommonConfigurationKeys.FS_DEFAULT_NAME_KEY, "***REMOVED***",
                HadoopConstants.MR_JOBTRACKER_ADDRESS_KEY, "***REMOVED***"));
    return connectFlowDef(flowConnector, flowDef);
  }

  private static void printFile(final java.util.List<java.lang.String> lines, String description) {
    System.out.println("\n\t# " + description
        + "\n\t" + Joiners.INDENT.join(lines) + "\n" + "\n");
  }

  @SneakyThrows
  private static java.util.List<java.lang.String> getLines(Environment environment, final String filePath) {
    val fs = environment.isLocal() ?
        FileSystems.getDefaultLocalFileSystem() :
        FileSystems.getFileSystem("***REMOVED***");
    return HadoopUtils.readSmallTextFile(fs, new Path(filePath));
  }

  private static String getDescription(Environment environment, JoinType joinType, JoinerType joinerType) {
    return Joiners.DASH.join(environment, joinType, joinerType);
  }
}
