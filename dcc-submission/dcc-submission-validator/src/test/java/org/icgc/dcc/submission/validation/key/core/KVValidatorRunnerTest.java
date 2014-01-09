package org.icgc.dcc.submission.validation.key.core;

import static org.fest.assertions.api.Assertions.assertThat;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;
import org.junit.Test;

import cascading.flow.hadoop.HadoopFlowStep;
import cascading.flow.hadoop.util.HadoopUtil;

@Slf4j
public class KVValidatorRunnerTest {

  /**
   * Test whether or not the object can be successfully serialized by Cascading. This test is intended to catch such
   * issues before testing in a clustered environment.
   * 
   * @see {@link HadoopFlowStep#pack()}
   */
  @Test
  public void testSerializable() {
    val runner = new KVValidatorRunner("", "", "");
    val serialized = cascadingSerialize(runner);
    log.info("runner: {}, serialized: {}", runner, serialized);

    assertThat(serialized).isNotNull().isNotEmpty();
  }

  /**
   * Method that reproduces what Cascading does to serialize a job step.
   * 
   * @param object the object to serialize.
   * @return the base 64 encoded object
   */
  @SneakyThrows
  private String cascadingSerialize(Object object) {
    return HadoopUtil.serializeBase64(object, new JobConf(new Configuration()));
  }

}
