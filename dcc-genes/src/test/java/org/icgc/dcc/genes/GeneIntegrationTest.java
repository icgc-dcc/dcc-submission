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
package org.icgc.dcc.genes;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;

import lombok.SneakyThrows;

import org.icgc.dcc.test.mongodb.EmbeddedMongo;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ValidationReport;
import com.github.fge.jsonschema.util.JsonLoader;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

public class GeneIntegrationTest {

  /**
   * Schema file.
   */
  private static final String SCHEMA_BASE_PATH = "org/icgc/dcc/genes";
  private static final String SCHEMA_FILE_NAME = "genes.schema.json";
  private static final String SCHEMA_PATH = SCHEMA_BASE_PATH + "/" + SCHEMA_FILE_NAME;

  /**
   * Test data.
   */
  protected static final String FIXTURES_DIR = "src/test/resources/fixtures";

  // See TODO below
  private final JsonSchema schema = null; // getSchema();

  @Rule
  public final EmbeddedMongo embeddedMongo = new EmbeddedMongo();

  @Test
  public void testGenesLoader() throws IOException {
    // To update genes.bson:
    // mongorestore -d test -c Gene src/test/resources/data/genes.bson
    // mongo
    // mongodump -d test -c Gene -o - > src/test/resources/data/genes.bson

    String bsonFile = FIXTURES_DIR + "/genes.bson";
    String mongoUri = getMongoUri();
    Main.main("-f", bsonFile, "-d", mongoUri);

    JsonNode gene = getGene(mongoUri);
    System.out.println(gene);

    // TODO: Re-enable after upgrading Jackson to 2.2.2 or higher, Jongo to 0.5 (for bson4jackson).
    // There seems to be some dependency conflicts
    // ValidationReport report = validate(gene);
    // assertThat(report.getMessages()).isEmpty();
  }

  @SuppressWarnings("unused")
  private ValidationReport validate(JsonNode gene) {
    ValidationReport report = schema.validate(gene);

    return report;
  }

  private JsonNode getGene(String uri) {
    MongoCollection genes = getGenes(uri);
    JsonNode gene = genes.findOne().as(JsonNode.class);
    genes.getDBCollection().getDB().getMongo().close();

    return gene;
  }

  private MongoCollection getGenes(String uri) {
    MongoClientURI mongoUri = new MongoClientURI(uri);
    Jongo jongo = getJongo(mongoUri);
    MongoCollection genes = jongo.getCollection(mongoUri.getCollection());

    return genes;
  }

  private Jongo getJongo(MongoClientURI mongoUri) {
    MongoClient mongo = embeddedMongo.getMongo();
    DB db = mongo.getDB(mongoUri.getDatabase());
    Jongo jongo = new Jongo(db);

    return jongo;
  }

  @SneakyThrows
  private JsonSchema getSchema() {
    JsonNode schemaNode = JsonLoader.fromFile(new File("src/main/resources", SCHEMA_PATH));
    JsonSchemaFactory factory = JsonSchemaFactory.defaultFactory();
    JsonSchema schema = factory.fromSchema(schemaNode);

    return schema;
  }

  private String getMongoUri() {
    return format("mongodb://localhost:%s/dcc-genome.Genes", embeddedMongo.getPort());
  }

}
