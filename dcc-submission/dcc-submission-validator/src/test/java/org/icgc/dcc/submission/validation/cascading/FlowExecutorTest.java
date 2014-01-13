package org.icgc.dcc.submission.validation.cascading;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.icgc.dcc.submission.validation.MiniHadoop;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

@Slf4j
public class FlowExecutorTest {

  @Test
  public void testExecute() throws IOException {
    val hadoop = createHadoop();
    val jobConf = hadoop.createJobConf();
    val properties = ImmutableMap.<Object, Object> of(
        "fs.defaultFS", jobConf.get("fs.defaultFS"),
        "mapred.job.tracker", jobConf.get("mapred.job.tracker")
        );

    val executor = new FlowExecutor(properties);
    executor.execute(new Task());

  }

  private MiniHadoop createHadoop() throws IOException {
    return new MiniHadoop(new Configuration(), 1, 1, new File("/tmp/hadoop"));
  }

  private static class Task implements Runnable, Serializable {

    @Override
    public void run() {
      log.info("Running!");
    }

  }

}
