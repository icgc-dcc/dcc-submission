/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.genes;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;

import lombok.extern.slf4j.Slf4j;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.marshall.jackson.bson4jackson.MongoBsonFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;

/**
 * Loads from Heliotrope {@code genes.bson} {@code mongodump} file into DCC gene database.
 */
@Slf4j
public class GenesLoader {

  private final GeneTransformer transformer = new GeneTransformer();

  private final File bsonFile;

  private final MongoURI mongoUri;

  public GenesLoader(File bsonFile, MongoURI mongoUri) {
    this.bsonFile = bsonFile;
    this.mongoUri = mongoUri;
  }

  public void load() throws IOException {
    final MongoCollection genes = getTargetCollection(mongoUri);
    try {
      // Drop the current collection
      genes.drop();

      // Open BSON file stream
      MappingIterator<BSONObject> iterator = getSourceIterator(bsonFile);

      // Transform and save
      eachGene(iterator, new GeneCallback() {
        @Override
        public void handle(BSONObject gene) {
          BSONObject transformed = transformer.transform(gene);

          genes.save(transformed);
        }
      });
    } finally {
      // Close db connection
      genes.getDBCollection().getDB().getMongo().close();
    }
  }

  MongoCollection getTargetCollection(MongoURI mongoUri) throws UnknownHostException {
    String database = mongoUri.getDatabase();
    String collection = mongoUri.getCollection();

    Mongo mongo = new Mongo(mongoUri);
    DB db = mongo.getDB(database);
    Jongo jongo = new Jongo(db);

    MongoCollection genes = jongo.getCollection(collection);

    return genes;
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  MappingIterator<BSONObject> getSourceIterator(File bsonFile) throws IOException, JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper(new MongoBsonFactory());
    MappingIterator<BSONObject> iterator = (MappingIterator) mapper.reader(BasicBSONObject.class).readValues(bsonFile);

    return iterator;
  }

  void eachGene(MappingIterator<BSONObject> iterator, GeneCallback callback) throws IOException {
    try {
      int insertCount = 0;
      while(hasNext(iterator)) {
        BSONObject gene = iterator.next();
        callback.handle(gene);

        if(++insertCount % 1000 == 0) {
          log.info("Processed {} genes", insertCount);
        }
      }
      log.info("Finished processing {} genes total", insertCount);
    } finally {
      iterator.close();
    }
  }

  /**
   * Wrapper method for dealing with a similar bug described in https://github.com/vznet/mongo-jackson-mapper/issues/6
   * 
   * @param iterator
   * @return
   */
  boolean hasNext(MappingIterator<BSONObject> iterator) {
    try {
      return iterator.hasNextValue();
    } catch(IOException e) {
      // Erroneous exception?
      return false;
    }
  }

  /**
   * Abstraction to allow for templated transformation and persistence.
   * 
   * @author btiernay
   */
  interface GeneCallback {
    void handle(BSONObject gene);
  }

}
