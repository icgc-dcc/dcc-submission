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
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.FieldNames;
import org.icgc.dcc.genes.cli.MongoClientURIConverter;
import org.jongo.Jongo;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

/**
 * Create Pathway collection in mongodb, and subsequently embed pathway information into the gene-collection
 * 
 * Reactome pathway file: http://www.reactome.org/download/current/UniProt2PathwayBrowser.txt
 */
@Slf4j
@AllArgsConstructor
public class PathwayLoader {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final String PATHWAY_COLLECTION_NAME = "Pathway";
  private static final String GENE_COLLECTION_NAME = "Gene";
  private static final String HOMO_SAPIEN = "Homo sapiens";
  private static final String[] CSV_HEADER = {
      FieldNames.PATHWAY_UNIPROT_ID,
      FieldNames.PATHWAY_REACTOME_ID,
      FieldNames.PATHWAY_URL,
      FieldNames.PATHWAY_NAME,
      FieldNames.PATHWAY_EVIDENCE_CODE,
      FieldNames.PATHWAY_SPECIES };

  /**
   * Configuration.
   */
  private final MongoClientURI mongoUri;

  @SneakyThrows
  public void load(Reader reader) {
    @Cleanup
    val mongo = new MongoClient(mongoUri);
    val db = mongo.getDB(mongoUri.getDatabase());
    val jongo = new Jongo(db);

    // Start fresh
    val pathwayCollection = jongo.getCollection(PATHWAY_COLLECTION_NAME);
    pathwayCollection.drop();

    val geneCollection = jongo.getCollection(GENE_COLLECTION_NAME);
    geneCollection.ensureIndex("{external_db_ids.uniprotkb_swissprot:1}");
    geneCollection.update("{}").multi().with("{$unset: {reactome_pathways:''}}");

    @Cleanup
    val csvReader = new CsvMapReader(reader, CsvPreference.TAB_PREFERENCE);
    Map<String, String> map = null;

    long count = 0;
    while (null != (map = csvReader.read(CSV_HEADER))) {
      if (map.get(FieldNames.PATHWAY_SPECIES).equals(HOMO_SAPIEN)) {
        ++count;

        // Convert from Map to JsonNode
        val pathway = MAPPER.valueToTree(map);

        // Insert Pathway document
        pathwayCollection.save(pathway);

        val id = map.get(FieldNames.PATHWAY_UNIPROT_ID);
        val pathwayID = map.get(FieldNames.PATHWAY_REACTOME_ID);
        val pathwayName = map.get(FieldNames.PATHWAY_NAME);
        val pathwayURL = map.get(FieldNames.PATHWAY_URL);

        // Update Gene document
        log.info("Processing reactome {}", id);
        geneCollection
            .update("{external_db_ids.uniprotkb_swissprot:'" + id + "'}")
            .multi()
            .with("{$push: { reactome_pathways:{_reactome_id:#, name:#, url:#}}}",
                pathwayID, pathwayName, pathwayURL);

      }
    }

    log.info("Finished loading reactome {} pathways", count);
  }

  public static void main(String args[]) throws FileNotFoundException {
    String uri = args[0];
    String file = args[1];

    val converter = new MongoClientURIConverter();
    PathwayLoader pathwayLoader = new PathwayLoader(converter.convert(uri));
    pathwayLoader.load(new FileReader(file));

  }

}
