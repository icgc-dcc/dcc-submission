package org.icgc.dcc.submission.web;

import static javax.ws.rs.client.Entity.json;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.icgc.dcc.submission.web.Responses.UNPROCESSABLE_ENTITY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import lombok.val;

import org.icgc.dcc.submission.core.AbstractDccModule;
import org.icgc.dcc.submission.dictionary.DictionaryService;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.release.ReleaseService;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;

public class DictionaryResourceTest extends ResourceTest {

  private static final GenericType<ArrayList<Dictionary>> ENTITY_TYPE = new GenericType<ArrayList<Dictionary>>() {};
  private static final String DICTIONARY_VERSION1 = "0.6c";
  private static final String DICTIONARY_VERSION2 = "0.6d";

  private Dictionary dictionary1;
  private Dictionary dictionary2;

  private DictionaryService mockDictionaryService;
  private ReleaseService mockReleaseService;

  @Override
  protected Application configure() {
    // Cannot use MockitoJUnitRunner since this gets called before the class constructor is called
    dictionary1 = new Dictionary(DICTIONARY_VERSION1);
    dictionary2 = new Dictionary(DICTIONARY_VERSION2);

    mockDictionaryService = mock(DictionaryService.class);
    mockReleaseService = mock(ReleaseService.class);

    when(mockDictionaryService.list()).thenReturn(ImmutableList.of(dictionary1, dictionary2));
    when(mockReleaseService.getNextDictionary()).thenReturn(dictionary2);

    return super.configure();
  }

  @Override
  protected Collection<? extends Module> configureModules() {
    return ImmutableList.of(new AbstractDccModule() {

      @Override
      protected void configure() {
        bind(DictionaryService.class).toInstance(mockDictionaryService);
        bind(ReleaseService.class).toInstance(mockReleaseService);
      }

    });
  }

  @Test
  public void testDictionaries() {
    List<Dictionary> dictionaries = target().path("dictionaries").request(MIME_TYPE).get(ENTITY_TYPE);

    assertThat(dictionaries).isNotNull().isNotEmpty().hasSize(2);
    assertThat(dictionaries.get(0).getVersion()).isEqualTo(DICTIONARY_VERSION1);
    assertThat(dictionaries.get(1).getVersion()).isEqualTo(DICTIONARY_VERSION2);
  }

  @Test
  public void testCurrentDictionary() {
    Dictionary dictionary = target().path("nextRelease/dictionary").request(MIME_TYPE).get(Dictionary.class);

    assertThat(dictionary).isNotNull();
    assertThat(dictionary.getVersion()).isEqualTo(DICTIONARY_VERSION2);
  }

  @Test
  public void testMalformedDictionary() {
    val malformedDictionaryJson = json("{\"x\":\"1\",\"y\":\"2\"}");
    Response response = target().path("dictionaries").request(MIME_TYPE).post(malformedDictionaryJson);

    assertThat(response.getStatus()).isEqualTo(UNPROCESSABLE_ENTITY.getStatusCode());
    assertThat(response.readEntity(String.class)).contains("Unrecognized field \"x\"");
  }

}
