/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.cascading;

import static cascading.flow.FlowDef.flowDef;
import static cascading.flow.hadoop.util.HadoopUtil.deserializeBase64;
import static cascading.flow.hadoop.util.HadoopUtil.readStateFromDistCache;
import static cascading.flow.hadoop.util.HadoopUtil.serializeBase64;
import static cascading.flow.hadoop.util.HadoopUtil.writeStateToDistCache;
import static cascading.util.Util.createUniqueID;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.icgc.dcc.hadoop.io.NullInputFormat;
import org.icgc.dcc.hadoop.io.NullOutputFormat;

import cascading.flow.Flow;
import cascading.flow.FlowStep;
import cascading.flow.hadoop.MapReduceFlow;
import cascading.flow.hadoop.util.JavaObjectSerializer;
import cascading.flow.local.LocalFlowConnector;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tap.Tap;

import com.google.common.collect.ImmutableMap;

@RequiredArgsConstructor
public class FlowExecutor implements Executor {

  @NonNull
  private final Map<Object, Object> properties;

  @Override
  @SneakyThrows
  public void execute(Runnable runnable) {
    val flow = createFlow(runnable);
    flow.complete();
  }

  private Flow<?> createFlow(Runnable runnable) throws IOException {
    if (isLocal()) {
      return createLocalFlow(runnable);
    } else {
      return createHadoopFlow(runnable);
    }
  }

  @SneakyThrows
  private boolean isLocal() {
    val fsUrl = properties.get(FS_DEFAULT_NAME_KEY);
    if (fsUrl == null) {
      return true;
    }

    return new URI(fsUrl.toString()).getScheme().equals("file");
  }

  private Flow<?> createLocalFlow(Runnable runnable) {
    Pipe pipe = new Each("flow-executor", new ExecuteFunction(runnable));

    Flow<?> flow = new LocalFlowConnector(properties)
        .connect(flowDef()
            .addSource(pipe, new EmptySourceTap<Void>("empty"))
            .addTailSink(pipe, new EmptySinkTap<Void>("empty")));

    return flow;
  }

  private Flow<?> createHadoopFlow(Runnable runnable) throws IOException {
    val jobConf = createJobConf(runnable);
    val flow = new MapReduceFlow(jobConf, false) {

      @Override
      @SuppressWarnings("rawtypes")
      protected Map<String, Tap> createSources(JobConf jobConf) {
        return ImmutableMap.of("empty", (Tap) new EmptySourceTap<Void>("empty"));
      }

      @Override
      @SuppressWarnings("rawtypes")
      protected Map<String, Tap> createSinks(JobConf jobConf) {
        return ImmutableMap.of("empty", (Tap) new EmptySinkTap("empty"));
      }

    };

    return flow;
  }

  private JobConf createJobConf(Runnable runnable) throws IOException {
    val jobConf = new JobConf();
    jobConf.setJarByClass(runnable.getClass());
    jobConf.setInputFormat(NullInputFormat.class);
    jobConf.setOutputFormat(NullOutputFormat.class);
    jobConf.setOutputKeyClass(NullWritable.class);
    jobConf.setOutputValueClass(NullWritable.class);
    jobConf.setMapperClass(FlowExecutorMapper.class);
    jobConf.setSpeculativeExecution(false);
    jobConf.setNumMapTasks(1);
    jobConf.setMaxMapAttempts(1);
    jobConf.setNumReduceTasks(0);

    addProperties(jobConf);
    writeRunnable(runnable, jobConf);

    return jobConf;
  }

  private void writeRunnable(Runnable runnable, JobConf jobConf) throws IOException {
    // Hadoop 20.2 doesn't like dist cache when using local mode
    val executorState = serializeBase64(runnable, jobConf, true);
    val maxSize = Short.MAX_VALUE;

    jobConf.set("cascading.util.serializer", JavaObjectSerializer.class.getName());
    jobConf.set("org.icgc.dcc.class.name", runnable.getClass().getName());
    if (isHadoopLocalMode(jobConf) || executorState.length() < maxSize) {
      jobConf.set("cascading.flow.step", executorState);
    } else {
      jobConf.set("cascading.flow.step.path", writeStateToDistCache(jobConf, getId(), executorState));
    }
  }

  private boolean isHadoopLocalMode(JobConf conf) {
    return "local".equals(conf.get("mapred.job.tracker"));
  }

  private void addProperties(JobConf jobConf) {
    for (val entry : properties.entrySet()) {
      jobConf.set(entry.getKey().toString(), entry.getValue().toString());
    }
  }

  private String getId() {
    return createUniqueID();
  }

  @Slf4j
  private static final class FlowExecutorMapper implements
      Mapper<NullWritable, NullWritable, NullWritable, NullWritable> {

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
      log.info("Starting...");
      val runnable = readRunnable();
      val beat = new FlowExecutorHeartbeat(reporter) {

        @Override
        protected void progress() {
          log.info("Sending heartbeat");
        }

      };

      try {
        beat.start();
        runnable.run();
      } finally {
        beat.stop();
      }
      log.info("Finished");
    }

    private Runnable readRunnable() throws IOException, ClassNotFoundException {
      val executorState = readExecutorState();
      System.out.println("Executor state:" + executorState);
      val runnable =
          (Runnable) deserializeBase64(executorState, jobConf, Class.forName(jobConf.get("org.icgc.dcc.class.name")));
      return runnable;
    }

    private String readExecutorState() throws IOException {
      String executorState = jobConf.getRaw("cascading.flow.step");
      if (executorState == null) {
        executorState = readStateFromDistCache(jobConf, jobConf.get(FlowStep.CASCADING_FLOW_STEP_ID));
      }

      return executorState;
    }

  }

  private static class FlowExecutorHeartbeat {

    private final AtomicInteger latch = new AtomicInteger();
    private final Thread beat;

    public FlowExecutorHeartbeat(final Reporter reporter, final long periodMillis) {
      beat = new Thread(
          new Runnable() {

            @Override
            public void run() {
              boolean interrupted = false;
              while (latch.get() == 0 && !interrupted) {
                try {
                  Thread.sleep(periodMillis);
                } catch (InterruptedException e) {
                  interrupted = true;
                }

                // keep the task alive
                reporter.progress();

                // call the optional custom progress method
                progress();
              }
            }
          });
    }

    public FlowExecutorHeartbeat(Reporter reporter) {
      this(reporter, 60 * 1000);
    }

    public void start() {
      beat.start();
    }

    public void stop() {
      latch.incrementAndGet();
      beat.interrupt();
    }

    protected void progress() {
    }

  }

}
