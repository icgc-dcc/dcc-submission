package org.icgc.dcc.submission.validation.pcawg.util;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

@Slf4j
public class PCAWGRepositoryTest {

  PCAWGRepository repository = new PCAWGRepository();

  @Test
  public void testGetProjects() {
    val projects = repository.getProjects();

    for (val project : projects) {
      log.info("Project: {}", project);
    }
  }

  @Test
  public void testGetProjectSamples() {
    val projectSamples = repository.getProjectSamples();

    for (val entry : projectSamples.entries()) {
      log.info("Project sample: {}", entry);
    }
  }

}
