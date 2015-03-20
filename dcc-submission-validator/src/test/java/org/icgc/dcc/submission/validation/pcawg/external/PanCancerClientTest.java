package org.icgc.dcc.submission.validation.pcawg.external;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.pcawg.external.PanCancerClient;
import org.junit.Test;

@Slf4j
public class PanCancerClientTest {

  PanCancerClient client = new PanCancerClient();

  @Test
  public void testGetProjects() {
    val projects = client.getProjects();

    for (val project : projects) {
      log.info("Project: {}", project);
    }
  }

  @Test
  public void testGetProjectSamples() {
    val projectSamples = client.getProjectSamples();

    for (val entry : projectSamples.entries()) {
      log.info("Project sample: {}", entry);
    }
  }

}
