package org.icgc.dcc.submission.validation.key.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URISyntaxException;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.JobConf;
import org.icgc.dcc.common.core.model.DataType.DataTypes;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.validation.ValidationTests;
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
  public void testSerializable() throws URISyntaxException {
    val runner = new KVValidatorRunner(new URI("file:///"), DataTypes.values(), getDictionary(), "", "", "");
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

  private Dictionary getDictionary() {
    return ValidationTests.getTestDictionary();
  }

}
