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
package org.icgc.dcc.hadoop.cascading;

import static cascading.flow.FlowDef.flowDef;
import static cascading.flow.hadoop.util.HadoopUtil.serializeBase64;
import static cascading.flow.hadoop.util.HadoopUtil.writeStateToDistCache;
import static cascading.util.Util.createUniqueID;
import static java.lang.Integer.MAX_VALUE;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY;
import static org.icgc.dcc.hadoop.cascading.FlowReporter.reportFlowFailure;
import static org.icgc.dcc.hadoop.util.HadoopConstants.MR_JOBTRACKER_ADDRESS_KEY;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.JobConf;
import org.icgc.dcc.hadoop.io.NullInputFormat;
import org.icgc.dcc.hadoop.io.NullOutputFormat;

import cascading.flow.Flow;
import cascading.flow.FlowException;
import cascading.flow.hadoop.MapReduceFlow;
import cascading.flow.hadoop.util.JavaObjectSerializer;
import cascading.flow.local.LocalFlowConnector;
import cascading.pipe.Each;
import cascading.tap.Tap;

import com.google.common.collect.ImmutableMap;

public class FlowExecutor extends ThreadPoolExecutor {

  /**
   * Constants.
   */
  public static final String JOB_NAME_PROPERTY = "org.icgc.dcc.class.name";
  public static final String CASCADING_FLOW_STEP_PROPERTY = "cascading.flow.step";
  public static final String CASCADING_FLOW_STEP_PATH_PROPERTY = "cascading.flow.step.path";
  public static final String CASCADING_SERIALIZER_PROPERTY = "cascading.util.serializer";

  /**
   * Configuration.
   */
  private final Map<Object, Object> properties;

  public FlowExecutor(@NonNull Map<Object, Object> properties) {
    super(0, MAX_VALUE, 60L, SECONDS, new SynchronousQueue<Runnable>());
    this.properties = properties;
  }

  public FlowExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, @NonNull TimeUnit unit,
      @NonNull BlockingQueue<Runnable> workQueue, @NonNull Map<Object, Object> properties) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    this.properties = properties;
  }

  public Flow<?> execute(@NonNull FlowExecutorJob job) {
    val flow = createFlow(job);
    return complete(flow);
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(final Callable<T> callable) {
    val flow = createFlow(convert(callable));
    return convert(callable, flow);
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
    val flow = createFlow(convert(runnable));
    return convert(flow);
  }

  protected static Flow<?> complete(Flow<?> flow) {
    try {
      flow.complete();

      return flow;
    } catch (FlowException e) {
      reportFlowFailure(flow);

      throw e;
    }
  }

  private static <T> FutureTask<T> convert(final Callable<T> callable, final Flow<?> flow) {
    return new FutureTask<T>(new Callable<T>() {

      @SuppressWarnings("unchecked")
      @Override
      public T call() throws Exception {
        return (T) complete(flow);
      }

    });
  }

  @SuppressWarnings("unchecked")
  private static <T> FutureTask<T> convert(final Flow<?> flow) {
    return new FutureTask<T>(new Runnable() {

      @Override
      @SneakyThrows
      public void run() {
        complete(flow);
      }

    }, (T) flow);
  }

  private static FlowExecutorJob convert(final Runnable command) {
    return new FlowExecutorJob() {

      @Override
      public void execute(Configuration configuration) {
        command.run();
      }

    };
  }

  private static <T> FlowExecutorJob convert(final Callable<T> command) {
    return new FlowExecutorJob() {

      @Override
      @SneakyThrows
      public void execute(Configuration configuration) {
        command.call();
      }

    };
  }

  private Flow<?> createFlow(FlowExecutorJob job) {
    if (isLocal()) {
      return createLocalFlow(job);
    } else {
      return createHadoopFlow(job);
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

  private Flow<?> createLocalFlow(final FlowExecutorJob job) {
    val pipe = new Each("flow-executor", new ExecuteFunction(new Runnable() {

      @Override
      public void run() {
        job.execute(new Configuration());

      }
    }));

    return new LocalFlowConnector(properties)
        .connect(flowDef()
            .addSource(pipe, new EmptySourceTap<Void>("empty"))
            .addTailSink(pipe, new EmptySinkTap<Void>("empty")));
  }

  @SneakyThrows
  private Flow<?> createHadoopFlow(FlowExecutorJob job) {
    val jobConf = createJobConf(job);
    MapReduceFlow flow = new MapReduceFlow(jobConf, false) {

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

  private JobConf createJobConf(FlowExecutorJob job) throws IOException {
    val jobConf = new JobConf();
    jobConf.setJarByClass(job.getClass());
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
    writeJob(job, jobConf);

    return jobConf;
  }

  private void writeJob(FlowExecutorJob job, JobConf jobConf) throws IOException {
    // Hadoop 20.2 doesn't like dist cache when using local mode
    val executorState = serializeBase64(job, jobConf, true);
    val maxSize = Short.MAX_VALUE;

    jobConf.set(CASCADING_SERIALIZER_PROPERTY, JavaObjectSerializer.class.getName());
    jobConf.set(JOB_NAME_PROPERTY, job.getClass().getName());
    if (isHadoopLocalMode(jobConf) || executorState.length() < maxSize) {
      jobConf.set(CASCADING_FLOW_STEP_PROPERTY, executorState);
    } else {
      jobConf.set(CASCADING_FLOW_STEP_PATH_PROPERTY, writeStateToDistCache(jobConf, getId(), executorState));
    }
  }

  private boolean isHadoopLocalMode(JobConf conf) {
    return "local".equals(conf.get(MR_JOBTRACKER_ADDRESS_KEY));
  }

  private void addProperties(JobConf jobConf) {
    for (val entry : properties.entrySet()) {
      jobConf.set(entry.getKey().toString(), entry.getValue().toString());
    }
  }

  private String getId() {
    return createUniqueID();
  }

}
