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
package org.icgc.dcc.genes.service;

import static org.icgc.dcc.core.util.FormatUtils.formatCount;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.genes.core.GeneTransformer;
import org.jongo.Jongo;
import org.jongo.MongoCollection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import de.undercouch.bson4jackson.BsonFactory;

/**
 * Loads from Heliotrope {@code genes.bson} {@code mongodump} file into DCC gene database.
 */
@Slf4j
@AllArgsConstructor
public class GeneLoaderService {

  private final GeneTransformer transformer = new GeneTransformer();

  @NonNull
  private final MongoClientURI mongoUri;

  @SneakyThrows
  public void load(InputStream inputStream) {
    log.info("Loading gene model from {} into {}...", inputStream, mongoUri);

    final MongoCollection genes = getTargetCollection(mongoUri);
    try {
      // Drop the current collection
      genes.drop();

      // Open BSON file stream
      MappingIterator<JsonNode> iterator = getSourceIterator(inputStream);

      // Transform and save
      eachGene(iterator, new GeneCallback() {
        @Override
        public void handle(JsonNode gene) {
          JsonNode transformed = transformer.transform(gene);

          genes.save(transformed);
        }
      });
    } finally {
      // Close db connection
      genes.getDBCollection().getDB().getMongo().close();
    }
  }

  @SneakyThrows
  public void load(File file) {
    InputStream inputStream = new FileInputStream(file);
    try {
      load(inputStream);
    } finally {
      inputStream.close();
    }
  }

  MongoCollection getTargetCollection(MongoClientURI mongoUri) throws UnknownHostException {
    String database = mongoUri.getDatabase();
    String collection = mongoUri.getCollection();

    Mongo mongo = new MongoClient(mongoUri);
    DB db = mongo.getDB(database);
    Jongo jongo = new Jongo(db);

    MongoCollection genes = jongo.getCollection(collection);

    return genes;
  }

  MappingIterator<JsonNode> getSourceIterator(InputStream inputStream) throws IOException, JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper(new BsonFactory());
    MappingIterator<JsonNode> iterator = mapper.reader(JsonNode.class).readValues(inputStream);

    return iterator;
  }

  void eachGene(MappingIterator<JsonNode> iterator, GeneCallback callback) throws IOException {
    try {
      int insertCount = 0;
      while(hasNext(iterator)) {
        JsonNode gene = iterator.next();
        callback.handle(gene);

        if(++insertCount % 10000 == 0) {
          log.info("Loaded {} genes", formatCount(insertCount));
        }
      }
      log.info("Finished processing {} gene(s) total", formatCount(insertCount));
    } finally {
      iterator.close();
    }
  }

  /**
   * Wrapper method for working around with https://github.com/michel-kraemer/bson4jackson/issues/25
   * 
   * @param iterator
   * @return
   */
  boolean hasNext(MappingIterator<JsonNode> iterator) {
    try {
      return iterator.hasNextValue();
    } catch(IOException e) {
      // Erroneous bson4jackson exception?
      return false;
    }
  }

  /**
   * Abstraction to allow for templated transformation and persistence.
   * 
   * @author btiernay
   */
  interface GeneCallback {
    void handle(JsonNode gene);
  }

}
