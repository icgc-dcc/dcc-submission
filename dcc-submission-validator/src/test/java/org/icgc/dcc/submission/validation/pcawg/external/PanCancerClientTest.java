package org.icgc.dcc.submission.validation.pcawg.external;

import org.junit.Ignore;
import org.junit.Test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Ignore("Site is down")
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
  public void testGetProjectSampleIds() {
    val projectSamples = client.getProjectSampleIds();

    for (val entry : projectSamples.entries()) {
      log.info("Project sample: {}", entry);
    }
  }

}
