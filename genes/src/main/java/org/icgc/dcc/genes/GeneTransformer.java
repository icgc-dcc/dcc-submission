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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @see https://wiki.oicr.on.ca/display/DCCSOFT/ElasticSearch+Index+Specification#ElasticSearchIndexSpecification-Donor-
 * Genepaircentricindex
 */
public class GeneTransformer {

  private final ObjectMapper mapper = new ObjectMapper();

  public BSONObject transform(BSONObject gene) {
    JsonNode node = node(gene);

    BSONObject t = new BasicBSONObject();
    t.put("symbol", symbol(node));
    t.put("name", name(node));
    t.put("synonyms", synonyms(node));
    t.put("chromosome", location(node).path("chromosome").asText());
    t.put("strand", location(node).path("strand").asInt());
    t.put("start", location(node).path("txStart").asLong());
    t.put("end", location(node).path("txEnd").asLong());
    t.put("ensembl_gene_id", id(node));
    t.put("canonical_transcript_id", transcripts(node).path("canonicalTranscriptId").asText());
    t.put("transcripts", transcripts(node).path("records"));

    return t;
  }

  private JsonNode node(BSONObject gene) {
    return mapper.valueToTree(gene);
  }

  private String symbol(JsonNode node) {
    return node.path("name").asText();
  }

  private String id(JsonNode node) {
    return node.path("id").asText();
  }

  private Object name(JsonNode node) {
    return node.path("sections").path("description").path("data").path("fullName").asText();
  }

  private List<String> synonyms(JsonNode node) {
    String symbol = symbol(node);
    Iterator<JsonNode> synonyms = node.path("sections").path("description").path("data").path("synonyms").elements();
    List<String> values = new ArrayList<String>();

    while(synonyms.hasNext()) {
      String synonym = synonyms.next().asText();

      final boolean unique = synonym.equals(symbol) == false;
      if(unique) {
        values.add(synonym);
      }
    }

    return values;
  }

  private JsonNode location(JsonNode node) {
    return node.path("sections").path("location").path("data");
  }

  private JsonNode transcripts(JsonNode node) {
    return node.path("sections").path("transcripts").path("data");
  }

}
