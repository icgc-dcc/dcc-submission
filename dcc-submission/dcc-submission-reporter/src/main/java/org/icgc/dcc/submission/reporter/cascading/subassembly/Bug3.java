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

import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.hadoop.cascading.Flows.connectFlowDef;

import java.io.File;
import java.net.InetAddress;
import java.util.Date;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.icgc.dcc.core.util.Joiners;
import org.icgc.dcc.hadoop.cascading.Flows;
import org.icgc.dcc.hadoop.cascading.connector.CascadingConnectors;
import org.icgc.dcc.hadoop.cascading.taps.CascadingTaps;
import org.icgc.dcc.hadoop.util.HadoopConstants;

import cascading.flow.Flow;
import cascading.flow.FlowDef;
import cascading.pipe.HashJoin;
import cascading.pipe.Pipe;
import cascading.pipe.joiner.LeftJoin;
import cascading.pipe.joiner.RightJoin;
import cascading.tuple.Fields;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

/**
 * 
 */
@Slf4j
public class Bug3 {

  public static void main(String[] args) {
    boolean left = "left".equalsIgnoreCase(args[0]);
    checkState(left || "right".equalsIgnoreCase(args[0]));
    log.info("Using " + (left ? "left" : "right"));

    String l = args[1]; // "/tmp/feature_types.tsv";
    String r = args[2]; // "/tmp/clinical.tsv";
    log.info("l: " + l);
    log.info("r: " + r);

    Pipe featureTypes = new Pipe("feature_types");
    Pipe clinical = new Pipe("clinical");
    Pipe join = new HashJoin(
        featureTypes,
        new Fields("analyzed_sample_id"),
        clinical,
        new Fields("sample_id"),
        left ?
            new LeftJoin() :
            new RightJoin());

    val flowDef = Flows.getFlowDef(PreComputation.class);
    val featureTypesTap = getTaps().getNoCompressionTsvWithHeader(l);
    val clinicalTap = getTaps().getNoCompressionTsvWithHeader(r);
    flowDef.addSource("feature_types", featureTypesTap);
    flowDef.addSource("clinical", clinicalTap);

    val outputDirFilePath = "/tmp/precomputation-" + new Date().getTime();
    val outputTap = getTaps().getNoCompressionTsvWithHeader(outputDirFilePath);
    flowDef.addTailSink(join, outputTap);

    connect(flowDef).complete();
    log.info("done: " + outputDirFilePath);
    printbug(outputDirFilePath);
  }

  static final boolean LOCAL = isLocal();

  @SneakyThrows
  private static boolean isLocal() {
    return "acroslt".equals(InetAddress.getLocalHost().getHostName());
  }

  static CascadingTaps getTaps() {
    return LOCAL ? CascadingTaps.LOCAL : CascadingTaps.DISTRIBUTED;
  }

  static Flow<?> connect(final FlowDef flowDef) {
    CascadingConnectors connectors = LOCAL ? CascadingConnectors.LOCAL : CascadingConnectors.DISTRIBUTED;
    val flowConnector = connectors.getFlowConnector(LOCAL ?
        ImmutableMap.of() :
        ImmutableMap.of(
            CommonConfigurationKeys.FS_DEFAULT_NAME_KEY, "***REMOVED***",
            HadoopConstants.MR_JOBTRACKER_ADDRESS_KEY, "***REMOVED***"));
    return connectFlowDef(flowConnector, flowDef);
  }

  @SneakyThrows
  static void printbug(final String outputDirFilePath) {
    val lines =
        Files.readLines(
            new File(LOCAL ?
                outputDirFilePath :
                "/hdfs/dcc" + outputDirFilePath + "/part-00000"),
            Charsets.UTF_8);
    System.out.println(Joiners.INDENT.join(lines.subList(0, 10)));
    System.out.println();
    System.out.println(Joiners.INDENT.join(lines.subList(lines.size() - 11, lines.size() - 1)));
  }

}
