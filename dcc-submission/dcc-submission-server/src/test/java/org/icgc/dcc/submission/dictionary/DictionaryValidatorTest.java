/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.dictionary;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Resources.getResource;

import java.net.URL;
import java.util.Iterator;

import lombok.SneakyThrows;
import lombok.val;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.junit.Before;
import org.junit.Test;

public class DictionaryValidatorTest {

  /**
   * Dictionary.
   */
  private static final ObjectReader DICTIONARY_READER = new ObjectMapper().reader(Dictionary.class);
  private static final String DEFAULT_DICTIONARY_PATH = "org/icgc/dcc/resources/Dictionary.json";
  private static final URL DEFAULT_DICTIONARY_URL = getResource(DEFAULT_DICTIONARY_PATH);

  /**
   * Code lists.
   */
  private static final ObjectReader CODELISTS_READER = new ObjectMapper().reader(CodeList.class);
  private static final String DEFAULT_CODELISTS_PATH = "org/icgc/dcc/resources/CodeList.json";
  private static final URL DEFAULT_CODELISTS_URL = getResource(DEFAULT_CODELISTS_PATH);

  DictionaryValidator validator;

  @Before
  @SneakyThrows
  public void setUp() {
    Dictionary dictionary = DICTIONARY_READER.readValue(DEFAULT_DICTIONARY_URL);
    Iterator<CodeList> codeLists = CODELISTS_READER.readValues(DEFAULT_CODELISTS_URL);
    this.validator = new DictionaryValidator(dictionary, newArrayList(codeLists));
  }

  @Test
  public void testValidate() {
    val observations = validator.validate();

    System.out.println("**** WARNINGS ****");
    for (val warning : observations.getWarnings()) {
      System.out.println(warning);
    }

    System.out.println("**** ERRORS ****");
    for (val error : observations.getErrors()) {
      System.out.println(error);
    }
  }

}
