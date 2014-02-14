/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.model.FileTypes.FileType.METH_ARRAY_M_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.METH_ARRAY_P_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.METH_ARRAY_SYSTEM_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.METH_SEQ_M_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.METH_SEQ_P_TYPE;
import static org.icgc.dcc.submission.core.util.DccResources.getDccResource;

import java.net.URL;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.util.ObjectMappers;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;

import com.fasterxml.jackson.databind.ObjectReader;

@NoArgsConstructor(access = PRIVATE)
@Slf4j
public class Dictionaries {

  private static final ObjectReader FILE_SCHEMA_READER = ObjectMappers.DEFAULT.reader(FileSchema.class);
  private static final ObjectReader DICTIONARY_SCHEMA_READER = ObjectMappers.DEFAULT.reader(Dictionary.class);
  private static final String FILE_SCHEMATA_PARENT_PATH = "dictionary";

  @SneakyThrows
  public static FileSchema readFileSchema(FileType fileType) {
    val fileSchemaPath = format("%s/%s.json", FILE_SCHEMATA_PARENT_PATH, fileType.getTypeName());
    log.info("Augmenting dictionary with: '{}'", fileSchemaPath);
    return FILE_SCHEMA_READER.readValue(getResource(fileSchemaPath));
  }

  @SneakyThrows
  public static Dictionary readDccResourcesDictionary() {
    return readDictionary(getDccResource("Dictionary.json"));
  }

  @SneakyThrows
  public static Dictionary readDictionary(String dictionaryResourcePath) {
    return readDictionary(getResource(dictionaryResourcePath));
  }

  @SneakyThrows
  public static Dictionary readDictionary(URL dictionaryURL) {
    return DICTIONARY_SCHEMA_READER.readValue(dictionaryURL);
  }

  /**
   * Temporary method to augment the dictionary with the new models.
   */
  public static void addNewModels(Dictionary dictionary) {
    dictionary.addFile(readFileSchema(METH_ARRAY_M_TYPE));
    dictionary.addFile(readFileSchema(METH_ARRAY_P_TYPE));
    dictionary.addFile(readFileSchema(METH_ARRAY_SYSTEM_TYPE));

    val methSeqM = new FileSchema();
    methSeqM.setName(METH_SEQ_M_TYPE.getTypeName());
    methSeqM.setPattern("^meth_seq_m(\\.[a-zA-Z0-9]+)?\\.txt(?:\\.gz|\\.bz2)?$");
    dictionary.addFile(methSeqM);

    val methSeqP = new FileSchema();
    methSeqP.setName(METH_SEQ_P_TYPE.getTypeName());
    methSeqP.setPattern("^meth_seq_p(\\.[a-zA-Z0-9]+)?\\.txt(?:\\.gz|\\.bz2)?$");
    dictionary.addFile(methSeqP);
  }

}
