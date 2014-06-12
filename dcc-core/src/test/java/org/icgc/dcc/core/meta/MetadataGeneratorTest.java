package org.icgc.dcc.core.meta;

import lombok.val;

import org.icgc.dcc.core.util.RestfulDictionaryResolver;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class MetadataGeneratorTest {

  @Test
  public void testGenerate() {
    val generator = new MetadataGenerator(new RestfulDictionaryResolver());
    System.out.println(generator.generate());
  }

}
