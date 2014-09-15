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
package org.icgc.dcc.submission.validation;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Resources.getResource;
import static lombok.AccessLevel.PRIVATE;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

@NoArgsConstructor(access = PRIVATE)
public final class TestUtils {

  /**
   * Jackson constants.
   */
  public static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
      .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

  @SneakyThrows
  public static List<CodeList> codeLists() {
    Iterator<CodeList> codeLists = MAPPER.reader(CodeList.class).readValues(getDccResource("CodeList.json"));
    return newArrayList(codeLists);
  }

  @SneakyThrows
  public static String codeListsToString() {
    return MAPPER.writeValueAsString(codeLists());
  }

  @SneakyThrows
  public static Dictionary dictionary() {
    return MAPPER.reader(Dictionary.class).readValue(getDccResource("Dictionary.json"));
  }

  public static List<String> getFieldNames(FileType type) {
    return newArrayList(dictionary().getFileSchema(type).getFieldNames());
  }

  private static URL getDccResource(String resourceName) {
    return getResource("org/icgc/dcc/resources/" + resourceName);
  }

}
