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

import java.io.IOException;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.generator.core.DataGenerator;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;

@Slf4j
public class ResourceWrapper {
  private static List<FileSchema> fileSchemas;

  private static List<CodeList> codeLists;

  private static ObjectMapper mapper = new ObjectMapper();

  public static void initDictionary(String dictionaryUrl) throws JsonParseException, JsonMappingException, IOException {
    log.info("Initializing dictionary: " + Resources.getResource(dictionaryUrl));
    fileSchemas = mapper.readValue(Resources.getResource(dictionaryUrl), Dictionary.class).getFiles();
  }

  public static void initCodeLists(String codeListUrl) throws JsonParseException, JsonMappingException, IOException {
    log.info("Initializing codelist: " + Resources.getResource(codeListUrl));
    codeLists = mapper.readValue(Resources.getResource(codeListUrl), new TypeReference<List<CodeList>>() {
    });
  }

  public static List<CodeList> getCodeLists() {
    return codeLists;
  }

  public static FileSchema getSchema(DataGenerator datagen, String schemaName) {
    for(FileSchema schema : fileSchemas) {
      if(schema.getName().equals(schemaName)) {
        return schema;
      }
    }
    return null;
  }

  public static boolean isUniqueField(List<String> list, String fieldName) {
    if(list.contains(fieldName)) {
      return true;
    }
    return false;
  }

}
