package org.icgc.dcc.web;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.GenericType;

import org.icgc.dcc.dictionary.model.Dictionary;
import org.junit.Test;

public class DictionaryResourceTest extends ResourceTest {

  private static final GenericType<ArrayList<Dictionary>> ENTITY_TYPE = new GenericType<ArrayList<Dictionary>>() {
  };

  @Test
  public void testDictionaries() {
    List<Dictionary> dictionaries = target().path("dictionaries").request(MIME_TYPE).get(ENTITY_TYPE);

    // TODO: Assert
    System.out.println(dictionaries);
  }

}
