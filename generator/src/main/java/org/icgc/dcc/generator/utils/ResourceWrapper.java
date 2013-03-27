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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.generator.core.DataGenerator;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

@Slf4j
public class ResourceWrapper {
  private List<FileSchema> fileSchemas;

  private List<CodeList> codeList;

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final ObjectReader READER = MAPPER.reader(CodeList.class);

  private static final String RESOURCE_BASE_PATH = "org/icgc/dcc/resources/";

  private static final String CODE_LIST_DEFAULT_FILE_NAME = "CodeList.json";

  private static final String DICTIONARY_DEFAULT_FILE_NAME = "Dictionary.json";

  public ResourceWrapper(File dictionaryFile, File codeListFile) throws JsonParseException, JsonMappingException,
      IOException, JsonProcessingException {
    if(dictionaryFile != null) {
      initDictionary(dictionaryFile);
    } else {
      initDictionary();
    }

    if(codeListFile != null) {
      initCodeLists(codeListFile);
    } else {
      initCodeLists();
    }
  }

  private void initDictionary(File dictionaryFile) throws JsonParseException, JsonMappingException, IOException {
    log.info("Initializing dictionary: {}", dictionaryFile.getAbsolutePath());
    fileSchemas = MAPPER.readValue(dictionaryFile, Dictionary.class).getFiles();
  }

  private void initDictionary() throws JsonParseException, JsonMappingException, IOException {
    log.info("Initializing dictionary: {}", Resources.getResource(RESOURCE_BASE_PATH + DICTIONARY_DEFAULT_FILE_NAME));
    fileSchemas =
        MAPPER.readValue(Resources.getResource(RESOURCE_BASE_PATH + DICTIONARY_DEFAULT_FILE_NAME), Dictionary.class)
            .getFiles();
  }

  private void initCodeLists(File codeListFile) throws JsonParseException, JsonMappingException, IOException {
    log.info("Initializing codelist: {}", codeListFile.getAbsolutePath());
    Iterator<CodeList> cl = READER.readValues(codeListFile);
    codeList = Lists.newArrayList(cl);
  }

  private void initCodeLists() throws JsonProcessingException, IOException {
    log.info("Initializing codelist: {}", Resources.getResource(RESOURCE_BASE_PATH + CODE_LIST_DEFAULT_FILE_NAME));
    Iterator<CodeList> cl = READER.readValues(Resources.getResource(RESOURCE_BASE_PATH + CODE_LIST_DEFAULT_FILE_NAME));
    codeList = Lists.newArrayList(cl);
  }

  public List<CodeList> getCodeLists() {
    return codeList;
  }

  public FileSchema getSchema(DataGenerator datagen, String schemaName) {
    for(FileSchema schema : fileSchemas) {
      if(schema.getName().equals(schemaName)) {
        return schema;
      }
    }
    return null;
  }

  public boolean isUniqueField(List<String> uniqueFields, String fieldName) {
    return uniqueFields.contains(fieldName);
  }

}
