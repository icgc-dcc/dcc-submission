package org.icgc.dcc.submission.web.resource;

import static javax.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.submission.web.util.Responses.UNPROCESSABLE_ENTITY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.service.DictionaryService;
import org.icgc.dcc.submission.service.ReleaseService;
import org.junit.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableList;

import lombok.val;

public class DictionaryResourceTest extends ResourceTest {

  private static final GenericType<ArrayList<Dictionary>> ENTITY_TYPE = new GenericType<ArrayList<Dictionary>>() {};
  private static final String DICTIONARY_VERSION1 = "0.6c";
  private static final String DICTIONARY_VERSION2 = "0.6d";

  @Resource
  private Dictionary dictionary1;
  @Resource
  private Dictionary dictionary2;

  @Configuration
  static class ResourceConfig {

    @Bean
    public Dictionary dictionary1() {
      return new Dictionary(DICTIONARY_VERSION1);
    }

    @Bean
    public Dictionary dictionary2() {
      return new Dictionary(DICTIONARY_VERSION2);
    }

    @Bean
    public DictionaryService dictionaryService() {
      val dictionaryService = mock(DictionaryService.class);
      when(dictionaryService.getDictionaries()).thenReturn(ImmutableList.of(dictionary1(), dictionary2()));

      return dictionaryService;
    }

    @Bean
    public ReleaseService releaseService() {
      val releaseService = mock(ReleaseService.class);
      when(releaseService.getNextDictionary()).thenReturn(dictionary2());

      return releaseService;
    }

  }

  @Override
  protected void register(SpringApplicationBuilder builder) {
    builder.sources(ResourceConfig.class);
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
