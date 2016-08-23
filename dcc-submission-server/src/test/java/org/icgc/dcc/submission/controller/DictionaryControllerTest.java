package org.icgc.dcc.submission.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.service.DictionaryService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;

import lombok.val;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = DictionaryController.class, secure = true)
public class DictionaryControllerTest {

  private static final String DICTIONARY_VERSION1 = "0.6c";
  private static final String DICTIONARY_VERSION2 = "0.6d";

  /**
   * Test data.
   */
  private Dictionary dictionary1 = new Dictionary(DICTIONARY_VERSION1);
  private Dictionary dictionary2 = new Dictionary(DICTIONARY_VERSION2);

  /**
   * Collaborators.
   */
  @MockBean
  private DictionaryService dictionaryService;

  @Autowired
  private MockMvc mvc;

  @Before
  public void setUp() {
    when(dictionaryService.getCurrentDictionary()).thenReturn(dictionary2);
    when(dictionaryService.getDictionaries()).thenReturn(ImmutableList.of(dictionary1, dictionary2));
  }

  @Test
  @WithMockUser(username = "user", password = "password", roles = "USER")
  public void testAddInvalidDictionary() throws Exception {
    mvc
        .perform(
            post("/ws/dictionaries")
                .content("{\"x\":\"1\",\"y\":\"2\"}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "user", password = "password", roles = "USER")
  public void testDictionaries() throws Exception {
    val result = mvc
        .perform(get("/ws/dictionaries")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    List<Dictionary> dictionaries =
        DEFAULT.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Dictionary>>() {});

    assertThat(dictionaries).isNotNull().isNotEmpty().hasSize(2);
    assertThat(dictionaries.get(0).getVersion()).isEqualTo(DICTIONARY_VERSION1);
    assertThat(dictionaries.get(1).getVersion()).isEqualTo(DICTIONARY_VERSION2);
  }

  @Test
  @WithMockUser(username = "user", password = "password", roles = "USER")
  public void testCurrentDictionary() throws Exception {
    val result = mvc
        .perform(
            get("/ws/dictionaries/current")
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    Dictionary dictionary =
        DEFAULT.readValue(result.getResponse().getContentAsString(), Dictionary.class);

    assertThat(dictionary).isNotNull();
    assertThat(dictionary.getVersion()).isEqualTo(DICTIONARY_VERSION2);
  }

}
