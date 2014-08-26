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

import static cascading.flow.hadoop.util.HadoopUtil.deserializeBase64;
import static cascading.flow.hadoop.util.HadoopUtil.readStateFromDistCache;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.apache.hadoop.mapred.JobConf.MAPRED_MAP_TASK_JAVA_OPTS;
import static org.icgc.dcc.core.util.FormatUtils.formatMemory;

import java.io.IOException;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;

import cascading.flow.FlowStep;

@Slf4j
public class FlowExecutorMapper implements Mapper<NullWritable, NullWritable, NullWritable, NullWritable> {

  private JobConf jobConf;

  @Override
  public void configure(JobConf jobConf) {
    log.info("Configuring...");
    this.jobConf = jobConf;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  @SneakyThrows
  public void map(NullWritable key, NullWritable value, OutputCollector<NullWritable, NullWritable> output,
      Reporter reporter) throws IOException {
    log.info("{}", repeat("-", 90));
    log.info("Flow Execution");
    log.info("{}", repeat("-", 90));
    log.info("{}: {}", MAPRED_MAP_TASK_JAVA_OPTS, jobConf.get(MAPRED_MAP_TASK_JAVA_OPTS));
    log.info("Starting with memory: {}...", formatMemory());

    log.info("Reading job...");
    val job = readJob();
    log.info("Creating heatbeat...");
    val heartbeat = createHeartbeat(reporter);

    try {
      log.info("Starting heartbeat...");
      heartbeat.start();

      log.info("Executing job...");
      job.execute(jobConf);
    } catch (Exception e) {
      log.error("Error executing job:", e);
      throw e;
    } finally {
      log.info("Finished with memory: {}...", formatMemory());
      heartbeat.stop();
    }
  }

  private FlowExecutorJob readJob() throws Exception {
    try {
      val executorState = readExecutorState();
      return (FlowExecutorJob) deserializeBase64(executorState, jobConf,
          Class.forName(jobConf.get(FlowExecutor.JOB_NAME_PROPERTY)));
    } catch (Exception e) {
      log.error("Error reading job:", e);
      throw e;
    }
  }

  private String readExecutorState() throws Exception {
    try {
      String executorState = jobConf.getRaw(FlowExecutor.CASCADING_FLOW_STEP_PROPERTY);
      if (executorState == null) {
        executorState = readStateFromDistCache(jobConf, jobConf.get(FlowStep.CASCADING_FLOW_STEP_ID));
      }

      return executorState;
    } catch (Exception e) {
      log.error("Error reading executor state:", e);
      throw e;
    }
  }

  private FlowExecutorHeartbeat createHeartbeat(Reporter reporter) {
    return new FlowExecutorHeartbeat(reporter) {

      @Override
      protected void progress() {
        log.info("Sending heartbeat: {}", formatMemory());
      }

    };
  }

}