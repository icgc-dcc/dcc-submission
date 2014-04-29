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
package org.icgc.dcc.genes.extra;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.genes.cli.MongoClientURIConverter;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

/**
 * Recreates the GeneList collection in mongodb
 */
@Slf4j
@AllArgsConstructor
public class GeneListLoader {

  private final String collection = "GeneList";
  private final MongoClientURI mongoUri;
  private final String[] header =
      new String[] {
          "symbol", "name",
          null, null, null,
          null, null, null, null,
          null, null, null, null,
          null, null, null,
          null, null, null, null, null
      };

  @SneakyThrows
  public void load(Reader reader) {
    String database = mongoUri.getDatabase();
    Mongo mongo = new MongoClient(mongoUri);
    DB db = mongo.getDB(database);
    Jongo jongo = new Jongo(db);

    final MongoCollection geneListCollection = jongo.getCollection(collection);
    final MongoCollection geneCollection = jongo.getCollection("Gene");

    geneListCollection.drop();

    CsvMapReader csvReader = new CsvMapReader(reader, CsvPreference.TAB_PREFERENCE);
    Map<String, String> map = null;

    geneCollection.ensureIndex("{symbol:1}");
    geneCollection.update("{}").multi().with("{$unset: {list:''}}");

    csvReader.getHeader(true);
    while (null != (map = csvReader.read(header))) {
      ObjectMapper mapper = new ObjectMapper();
      JsonNode geneList = mapper.valueToTree(map);
      geneListCollection.save(geneList);

      geneCollection.update("{symbol:'" + map.get("symbol") + "'}")
          .multi()
          .with("{$addToSet: {list:#}}", "Cancer Gene Census");

      log.info("Prcessing {}", map.get("symbol"));
    }
    csvReader.close();
  }

  public static void main(String args[]) throws FileNotFoundException {
    MongoClientURIConverter converter = new MongoClientURIConverter();
    String uri = args[0];
    String file = args[1];

    // String uri = "mongodb://localhost/dcc-genome";
    // String file = "/Users/dchang/Downloads/cancer_gene_census.tsv";

    GeneListLoader pathwayLoader =
        new GeneListLoader(converter.convert(uri));
    pathwayLoader.load(new FileReader(file));
  }
}
