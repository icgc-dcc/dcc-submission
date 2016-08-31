/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.server.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.server.service.DictionaryService;
import org.icgc.dcc.submission.server.web.controller.DictionaryController;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableList;

import lombok.val;

@WebMvcTest(DictionaryController.class)
public class DictionaryControllerTest extends ControllerTest {

  /**
   * Test data.
   */
  private static final String DICTIONARY_VERSION1 = "0.6c";
  private static final String DICTIONARY_VERSION2 = "0.6d";

  private Dictionary dictionary1 = new Dictionary(DICTIONARY_VERSION1);
  private Dictionary dictionary2 = new Dictionary(DICTIONARY_VERSION2);

  /**
   * Collaborators.
   */
  @MockBean
  private DictionaryService dictionaryService;

  @Before
  public void setUp() {
    when(dictionaryService.getCurrentDictionary()).thenReturn(dictionary2);
    when(dictionaryService.getDictionaries()).thenReturn(ImmutableList.of(dictionary1, dictionary2));
  }

  @Test
  public void testAddInvalidDictionary() throws Exception {
    mvc
        .perform(
            post("/ws/dictionaries")
                .content("{\"x\":\"1\",\"y\":\"2\"}")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .with(admin()))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void testDictionaries() throws Exception {
    val result = mvc
        .perform(get("/ws/dictionaries")
            .accept(MediaType.APPLICATION_JSON)
            .with(admin()))
        .andExpect(status().isOk())
        .andReturn();

    List<Dictionary> dictionaries =
        DEFAULT.readValue(result.getResponse().getContentAsString(), new TypeReference<List<Dictionary>>() {});

    assertThat(dictionaries).isNotNull().isNotEmpty().hasSize(2);
    assertThat(dictionaries.get(0).getVersion()).isEqualTo(DICTIONARY_VERSION1);
    assertThat(dictionaries.get(1).getVersion()).isEqualTo(DICTIONARY_VERSION2);
  }

  @Test
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
