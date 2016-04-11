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
package org.icgc.dcc.submission.dictionary.util;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.common.core.dcc.DccResources.getDictionaryDccResource;
import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;
import static org.icgc.dcc.common.core.json.Jackson.from;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.icgc.dcc.common.core.dcc.DccResources;
import org.icgc.dcc.common.core.json.Jackson;
import org.icgc.dcc.common.core.meta.RestfulCodeListsResolver;
import org.icgc.dcc.common.core.meta.RestfulDictionaryResolver;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Optional;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = PRIVATE)
@Slf4j
public class Dictionaries {

  private static final ObjectReader FILE_SCHEMA_READER = Jackson.DEFAULT.reader(FileSchema.class);
  private static final ObjectReader DICTIONARY_SCHEMA_READER = Jackson.DEFAULT.reader(Dictionary.class);
  private static final ObjectReader CODELIST_SCHEMA_READER = Jackson.DEFAULT.reader(CodeList.class);
  private static final String FILE_SCHEMATA_PARENT_PATH = "dictionary";

  @SneakyThrows
  public static FileSchema readFileSchema(FileType fileType) {
    val fileSchemaPath = format("%s/%s.json", FILE_SCHEMATA_PARENT_PATH, fileType.getId());
    log.info("Augmenting dictionary with: '{}'", fileSchemaPath);
    return FILE_SCHEMA_READER.readValue(getResource(fileSchemaPath));
  }

  @SneakyThrows
  public static Dictionary readDccResourcesDictionary() {
    return readDictionary(getDictionaryDccResource());
  }

  @SneakyThrows
  public static List<CodeList> readDccResourcesCodeLists() {
    return readCodeList(DccResources.getCodeListsDccResource());
  }

  @SneakyThrows
  public static Dictionary readDictionary(String dictionaryResourcePath) {
    return readDictionary(getResource(dictionaryResourcePath));
  }

  @SneakyThrows
  public static Dictionary readDictionary(URL dictionaryURL) {
    return DICTIONARY_SCHEMA_READER.readValue(dictionaryURL);
  }

  @SneakyThrows
  public static List<CodeList> readCodeList(URL codeListsURL) {
    Iterator<CodeList> iterator = CODELIST_SCHEMA_READER.readValues(codeListsURL);
    return newArrayList(iterator);
  }

  public static void writeDictionary(Dictionary dictionary, String filePath) {
    writeDictionary(dictionary, new File(filePath));
  }

  @SneakyThrows
  public static void writeDictionary(Dictionary dictionary, File file) {
    Jackson.PRETTY_WRITTER.writeValue(file, dictionary);
  }

  public static Dictionary getDictionary(String submissionWebAppUri, String dictionaryVersion) {
    return from(
        new RestfulDictionaryResolver(submissionWebAppUri)
            .apply(Optional.of(dictionaryVersion)),
        Dictionary.class);
  }

  public static List<CodeList> getCodeLists(String submissionWebAppUri) {
    return DEFAULT.<List<CodeList>> convertValue(
        new RestfulCodeListsResolver(submissionWebAppUri)
            .get(),
        new TypeReference<List<CodeList>>() {});
  }

}
