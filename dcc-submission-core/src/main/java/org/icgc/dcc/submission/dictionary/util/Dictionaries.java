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
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;
import static org.icgc.dcc.common.core.json.Jackson.from;

import java.io.File;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;

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
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import lombok.Cleanup;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class Dictionaries {

  /**
   * Constants.
   */
  private static final ObjectReader FILE_SCHEMA_READER = Jackson.DEFAULT.reader(FileSchema.class);
  private static final ObjectReader DICTIONARY_SCHEMA_READER = Jackson.DEFAULT.reader(Dictionary.class);
  private static final ObjectReader CODELIST_SCHEMA_READER = Jackson.DEFAULT.reader(CodeList.class);

  @SneakyThrows
  public static FileSchema readResourcesFileSchema(FileType fileType) {
    val url = getResourceUrl("/filetypes/" + fileType.getId() + ".json");
    log.info("Augmenting dictionary with: '{}'", url);
    return FILE_SCHEMA_READER.readValue(url);
  }

  @SneakyThrows
  public static Dictionary readResourcesDictionary() {
    val url = getResourceUrl("/dictionaries/");
    val versions = readResourceListing(url);
    val latest = versions.last().replaceAll("/", "");

    return readResourcesDictionary(latest);
  }

  @SneakyThrows
  public static Dictionary readResourcesDictionary(String version) {
    return readDictionary(getResourceUrl("/dictionaries/" + version + "/dictionary.json"));
  }

  @SneakyThrows
  public static List<CodeList> readResourcesCodeLists() {
    return readCodeList(getResourceUrl("/codelists/codelists.json"));
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

  private static URL getResourceUrl(String path) {
    return Resources.getResource("org/icgc/dcc/submission/resources" + path);
  }

  @SneakyThrows
  private static NavigableSet<String> readResourceListing(URL url) {
    val uri = url.toURI();
    Path path;
    if (uri.getScheme().equals("jar")) {
      val fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
      path = fileSystem.getPath(uri.toString().split("!")[1]);
    } else {
      path = Paths.get(uri);
    }

    @Cleanup
    val listing = Files.list(path);
    val resources = Sets.<String> newTreeSet();
    for (Iterator<Path> it = listing.iterator(); it.hasNext();) {
      resources.add(it.next().getFileName().toString());
    }

    return resources;
  }

}
