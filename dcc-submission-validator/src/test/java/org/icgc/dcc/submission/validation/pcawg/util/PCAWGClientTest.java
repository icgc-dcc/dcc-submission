package org.icgc.dcc.submission.validation.pcawg.util;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

@Slf4j
public class PCAWGClientTest {

  PCAWGClient client = new PCAWGClient();

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
