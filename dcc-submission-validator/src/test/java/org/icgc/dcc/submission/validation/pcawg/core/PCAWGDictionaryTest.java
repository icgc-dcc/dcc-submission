package org.icgc.dcc.submission.validation.pcawg.core;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.junit.Test;

@Slf4j
public class PCAWGDictionaryTest {

  private PCAWGDictionary dictionary = new PCAWGDictionary();

  @Test
  public void testGetWhitelistSampleIds() {
    val whitelistSamples = dictionary.getWhitelistSampleIds("TEST1-DCC");
    log.info("whitelistSamples: {}", whitelistSamples);
  }

}
