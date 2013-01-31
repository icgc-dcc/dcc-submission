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

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.IOException;
import java.net.UnknownHostException;

import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.report.ValidationReport;
import com.github.fge.jsonschema.util.JsonLoader;
import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;

public class IntegrationTest {

  private JsonSchema schema;

  @Before
  public void setUp() throws IOException {
    this.schema = getSchema();
  }

  @Test
  public void testSystem() throws IOException {
    String bsonFile = "src/test/resources/heliotrope/genes.bson";
    String uri = "mongodb://localhost/dcc-genome.Genes";
    Main.main("-f", bsonFile, "-d", uri);

    JsonNode gene = getGene(uri);
    ValidationReport report = validate(gene);

    assertThat(report.getMessages()).isEmpty();
  }

  private ValidationReport validate(JsonNode gene) {
    ValidationReport report = schema.validate(gene);

    return report;
  }

  private JsonNode getGene(String uri) throws UnknownHostException {
    MongoCollection genes = getGenes(uri);
    JsonNode gene = genes.findOne().as(JsonNode.class);
    genes.getDBCollection().getDB().getMongo().close();

    return gene;
  }

  private MongoCollection getGenes(String uri) throws UnknownHostException {
    MongoURI mongoUri = new MongoURI(uri);
    Jongo jongo = getJongo(mongoUri);
    MongoCollection genes = jongo.getCollection(mongoUri.getCollection());

    return genes;
  }

  private Jongo getJongo(MongoURI mongoUri) throws UnknownHostException {
    Mongo mongo = new Mongo(mongoUri);
    DB db = mongo.getDB(mongoUri.getDatabase());
    Jongo jongo = new Jongo(db);

    return jongo;
  }

  private JsonSchema getSchema() throws IOException {
    JsonNode schemaNode = JsonLoader.fromResource("/genes.schema.json");
    JsonSchemaFactory factory = JsonSchemaFactory.defaultFactory();
    JsonSchema schema = factory.fromSchema(schemaNode);

    return schema;
  }

}
