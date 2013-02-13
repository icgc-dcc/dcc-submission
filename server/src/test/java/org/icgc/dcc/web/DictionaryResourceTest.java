package org.icgc.dcc.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.GenericType;

import org.icgc.dcc.core.AbstractDccModule;
import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.release.DccLocking;
import org.icgc.dcc.release.ReleaseService;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.typesafe.config.Config;

import static org.fest.assertions.api.Assertions.assertThat;

public class DictionaryResourceTest extends ResourceTest {

  private static final GenericType<ArrayList<Dictionary>> DICTIONARY_LIST = new GenericType<ArrayList<Dictionary>>() {
  };

  private static final GenericType<Dictionary> DICTIONARY = new GenericType<Dictionary>() {
  };

  private static final String DICTIONARY_VERSION1 = "0.6c";

  private static final String DICTIONARY_VERSION2 = "0.6d";

  private static final Dictionary dictionary1 = new Dictionary();

  private static final Dictionary dictionary2 = new Dictionary();
  static {
    dictionary1.setVersion(DICTIONARY_VERSION1);
    dictionary2.setVersion(DICTIONARY_VERSION2);
  }

  @Test
  public void testDictionaries() {
    List<Dictionary> dictionaries = target().path("dictionaries").request(MIME_TYPE).get(DICTIONARY_LIST);

    assertThat(dictionaries).isNotNull().isNotEmpty().hasSize(2);
    assertThat(dictionaries.get(0).getVersion()).isEqualTo(DICTIONARY_VERSION1);
    assertThat(dictionaries.get(1).getVersion()).isEqualTo(DICTIONARY_VERSION2);
  }

  @Test
  public void testCurrentDictionary() {
    Dictionary dictionary = target().path("nextRelease/dictionary").request(MIME_TYPE).get(DICTIONARY);

    assertThat(dictionary).isNotNull();
    assertThat(dictionary.getVersion()).isEqualTo(DICTIONARY_VERSION2);
  }

  @Override
  protected Collection<? extends Module> configureModule() {
    return ImmutableList.of(new AbstractDccModule() {
      @Override
      protected void configure() {
        bind(DictionaryService.class).to(MockDictionaryService.class);
        bind(ReleaseService.class).to(MockReleaseService.class);
      }
    });
  }

  private static class MockDictionaryService extends DictionaryService {

    @Inject
    public MockDictionaryService(Morphia morphia, Datastore datastore, DccFileSystem fs, ReleaseService releases) {
      super(morphia, datastore, fs, releases);
    }

    @Override
    public List<Dictionary> list() {
      return ImmutableList.of(dictionary1, dictionary2);
    }
  }

  private static class MockReleaseService extends ReleaseService {

    @Inject
    public MockReleaseService(DccLocking dccLocking, Morphia morphia, Datastore datastore, DccFileSystem fs,
        Config config) {
      super(dccLocking, morphia, datastore, fs, config);
    }

    @Override
    public Dictionary getNextDictionary() {
      return dictionary2;
    }
  }
}
