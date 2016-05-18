package org.icgc.dcc.submission.validation.pcawg.core;

import org.junit.Test;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PCAWGDictionaryTest {

  private PCAWGDictionary dictionary = new PCAWGDictionary();

  @Test
  public void testGetExcludedProjectKeys() {
    val excludedProjectKeys = dictionary.getExcludedProjectKeys();
    log.info("excludedProjectKeys: {}", excludedProjectKeys);
  }

  @Test
  public void testGetExcludedSampleIds() {
    val excludedSampleIds = dictionary.getExcludedSampleIds("PACA-CA");
    log.info("excludedProjectKeys: {}", excludedSampleIds);
  }

}
