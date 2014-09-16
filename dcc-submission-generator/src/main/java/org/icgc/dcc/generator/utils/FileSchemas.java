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
package org.icgc.dcc.generator.utils;

import static org.icgc.dcc.generator.utils.Dictionaries.DEFAULT_DICTIONARY_URL;
import static org.icgc.dcc.generator.utils.Dictionaries.getDictionary;

import java.io.File;
import java.net.URL;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.FileSchema;

import com.google.common.collect.ImmutableList;

/**
 * The FileSchema initializes the Dictionary and stores all the file schemas.
 */
@Slf4j
public class FileSchemas {

  @NonNull
  @Getter
  final List<FileSchema> schemas;

  public FileSchemas() {
    log.info("Initializing dictionary from URL: {}", DEFAULT_DICTIONARY_URL.getPath());
    this.schemas = resolveFileSchemas(DEFAULT_DICTIONARY_URL);
  }

  @SneakyThrows
  public FileSchemas(File file) {
    log.info("Initializing dictionary from file: {}", file.getAbsolutePath());
    this.schemas = resolveFileSchemas(file.toURI().toURL());
  }

  public FileSchema getSchema(String schemaName) {
    for (val schema : schemas) {
      if (schema.getName().equals(schemaName)) {
        return schema;
      }
    }

    return null;
  }

  private List<FileSchema> resolveFileSchemas(URL url) {
    val dictionary = getDictionary(url);

    log.info("Dictionary version: {}", dictionary.getVersion());
    return ImmutableList.copyOf(dictionary.getFiles());
  }

}
