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
package org.icgc.dcc.test.mongodb;

import static org.icgc.dcc.test.json.JsonNodes.MAPPER;

import java.io.File;
import java.net.URL;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.bson.types.ObjectId;
import org.jongo.Jongo;
import org.jongo.MongoCollection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.mongodb.DB;

@Slf4j
public class MongoImporter extends BaseMongoImportExport {

  /**
   * Mongo id field.
   */
  private static final String ID_FIELD = "_id";

  private static final ObjectReader READER = MAPPER.reader(JsonNode.class);

  public MongoImporter(DB targetDatabase) {
    super(null, new Jongo(targetDatabase));
  }

  public MongoImporter(File sourceDirectory, DB targetDatabase) {
    super(sourceDirectory, new Jongo(targetDatabase));
  }

  @Override
  @SneakyThrows
  public void execute() {
    for(File collectionFile : directory.listFiles()) {
      String collectionName = getCollectionName(collectionFile);
      MongoCollection collection = jongo.getCollection(collectionName);

      importCollection(collectionFile, collection);
    }
  }

  @SneakyThrows
  private void importCollection(File collectionFile, MongoCollection collection) {
    log.info("Importing to '{}' from '{}'...", collection, collectionFile);
    MappingIterator<JsonNode> iterator = READER.readValues(collectionFile);

    while(iterator.hasNext()) {
      JsonNode object = iterator.next();
      handleObjectId(object);

      collection.save(object);
    }
  }

  @SneakyThrows
  public void importCollection(URL collectionFile) {
    String collectionName = getCollectionName(new File(collectionFile.getFile()));
    MongoCollection collection = jongo.getCollection(collectionName);

    log.info("Importing to '{}' from '{}'...", collection, collectionFile);
    MappingIterator<JsonNode> iterator = READER.readValues(collectionFile);

    while(iterator.hasNext()) {
      JsonNode object = iterator.next();
      handleObjectId(object);

      collection.save(object);
    }
  }

  private void handleObjectId(JsonNode object) {
    if(object.has(ID_FIELD)) {
      String value = object.get(ID_FIELD).textValue();
      if(ObjectId.isValid(value)) {
        ((ObjectNode) object).put(ID_FIELD, new POJONode(new ObjectId(value)));
      }
    }
  }

}
