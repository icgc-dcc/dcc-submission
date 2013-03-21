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
package org.icgc.dcc.mongodb;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.jongo.Jongo;
import org.jongo.MongoCollection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.mongodb.DB;

/**
 * FIXME: DUPLICATED FROM AGGREGATOR!!!
 */
@Slf4j
public class MongoExporter extends BaseMongoImportExport {

  public MongoExporter(File targetDirectory, DB targetDatabase) {
    super(targetDirectory, new Jongo(targetDatabase));
  }

  @Override
  @SneakyThrows
  public void execute() {
    for(String collectionName : jongo.getDatabase().getCollectionNames()) {
      MongoCollection collection = jongo.getCollection(collectionName);
      String fileName = getFileName(collectionName);
      File collectionFile = new File(directory, fileName);

      log.info("Exporting to '{}' from '{}'...", collectionFile, collection);
      exportCollection(collectionFile, collection);
    }
  }

  @SneakyThrows
  private void exportCollection(File collectionFile, MongoCollection collection) {
    Files.createParentDirs(collectionFile);
    checkState(collectionFile.delete(), "Collection file not deleted: %s", collectionFile);
    checkState(collectionFile.createNewFile(), "Collection file not created: %s", collectionFile);

    ObjectMapper mapper = new ObjectMapper();
    for(JsonNode jsonNode : collection.find().as(JsonNode.class)) {
      String json = mapper.writeValueAsString(jsonNode) + System.getProperty("line.separator");
      Files.append(json, collectionFile, Charsets.UTF_8);
    }
  }

}
