package org.icgc.dcc.submission.validation.cascading;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.icgc.dcc.common.cascading.FlowExecutor;
import org.icgc.dcc.common.cascading.FlowExecutorJob;
import org.icgc.dcc.submission.validation.MiniHadoop;
import org.junit.Ignore;
import org.junit.Test;

import cascading.flow.FlowException;

import com.google.common.collect.ImmutableMap;

@Slf4j
public class FlowExecutorTest {

  @Ignore
  @Test
  public void testExecuteHadoopSuccess() throws IOException {
    executeHadoop(new SuccessTask());
  }

  @Ignore
  @Test(expected = FlowException.class)
  public void testExecuteHadoopFailure() throws IOException {
    executeHadoop(new FailureTask());
  }

  @Test
  public void testExecuteLocalSuccess() throws IOException {
    executeLocal(new SuccessTask());
  }

  @Test(expected = FlowException.class)
  public void testExecuteLocalFailure() throws Exception {
    executeLocal(new FailureTask());
  }

  @SneakyThrows
  private static void executeHadoop(FlowExecutorJob job) throws IOException {
    val hadoop = createHadoop();
    val jobConf = hadoop.createJobConf();
    val properties = ImmutableMap.<Object, Object> of(
        "fs.defaultFS", jobConf.get("fs.defaultFS"),
        "mapred.job.tracker", jobConf.get("mapred.job.tracker")
        );

    val executor = new FlowExecutor(properties);
    executor.execute(job);
  }

  @SneakyThrows
  private static void executeLocal(FlowExecutorJob job) throws IOException {
    val properties = ImmutableMap.<Object, Object> of();

    val executor = new FlowExecutor(properties);
    executor.execute(job);
  }

  private static MiniHadoop createHadoop() throws IOException {
    return new MiniHadoop(new Configuration(), 1, 1, new File("/tmp/hadoop"));
  }

  private static class SuccessTask implements FlowExecutorJob, Serializable {

    @Override
    public void execute(Configuration configuration) {
      log.info("Running!");
    }

  }

  private static class FailureTask implements FlowExecutorJob, Serializable {

    @Override
    public void execute(Configuration configuration) {
      throw new RuntimeException();
    }

  }

}
