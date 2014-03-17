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
 * Recreates the Pathway collection in mongodb
 */
@AllArgsConstructor
public class PathwayLoader {

  private final String collection = "Pathway";
  private static final String HOMO_SAPIEN = "Homo sapiens";
  private final MongoClientURI mongoUri;
  private final String[] header =
      new String[] { "UniProtID", "PathwayID", "URL", "PathwayName", "EvidenceCode", "Species" };

  public void load(Reader reader) {
    try {
      String database = mongoUri.getDatabase();
      Mongo mongo = new MongoClient(mongoUri);
      DB db = mongo.getDB(database);
      Jongo jongo = new Jongo(db);
      MongoCollection pathwayCollection = jongo.getCollection(collection);
      MongoCollection geneCollection = jongo.getCollection("Gene");

      pathwayCollection.drop();
      CsvMapReader csvReader = new CsvMapReader(reader, CsvPreference.TAB_PREFERENCE);
      Map<String, String> map = null;

      geneCollection.ensureIndex("{external_db_ids.uniprotkb_swissprot:1}");
      geneCollection.update("{}").multi().with("{$unset: {reactome_pathways:''}}");

      long c = 0;
      while (null != (map = csvReader.read(header))) {
        if (map.get("Species").equals(HOMO_SAPIEN)) {
          ++c;
          ObjectMapper mapper = new ObjectMapper();
          JsonNode pathway = mapper.valueToTree(map);
          pathwayCollection.save(pathway);

          String id = map.get("UniProtID");
          String pathwayID = map.get("PathwayID");
          String pathwayName = map.get("PathwayName");
          String pathwayURL = map.get("URL");

          System.out.println("id " + id);

          geneCollection
              .update("{external_db_ids.uniprotkb_swissprot:'" + id + "'}")
              .multi()
              .with("{$push: { reactome_pathways:{_pathway_id:#, pathway_name:#, url:#}}}",
                  pathwayID, pathwayName, pathwayURL);

        }
      }
      System.out.println(c);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String args[]) throws FileNotFoundException {
    MongoClientURIConverter converter = new MongoClientURIConverter();
    String uri = "mongodb://localhost/dcc-genome";
    String file = "/Users/dchang/Downloads/UniProt2PathwayBrowser.txt";

    PathwayLoader pathwayLoader =
        new PathwayLoader(converter.convert(uri));
    pathwayLoader.load(new FileReader(file));

  }
}
