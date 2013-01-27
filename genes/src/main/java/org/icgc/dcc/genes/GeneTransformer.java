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

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.bson.BSONObject;
import org.bson.BasicBSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Transforms from Heliotrope JSON structure to DCC JSON structure
 * 
 * @see https://wiki.oicr.on.ca/display/DCCSOFT/ElasticSearch+Index+Specification#ElasticSearchIndexSpecification-Donor-
 * Genepaircentricindex
 */
public class GeneTransformer {

  private final ObjectMapper mapper = new ObjectMapper();

  public BSONObject transform(BSONObject gene) {
    // For easier navigation, manipulation and to avoid casting
    JsonNode node = node(gene);

    BSONObject result = new BasicBSONObject();
    result.put("symbol", symbol(node));
    result.put("name", name(node));
    result.put("synonyms", synonyms(node));
    result.put("chromosome", location(node).path("chromosome").asText());
    result.put("strand", location(node).path("strand").asInt());
    result.put("start", location(node).path("txStart").asLong());
    result.put("end", location(node).path("txEnd").asLong());
    result.put("ensembl_gene_id", id(node));
    result.put("canonical_transcript_id", canonicalTranscriptId(node));
    result.put("transcripts", transcripts(node));

    return result;
  }

  private JsonNode node(BSONObject gene) {
    return mapper.valueToTree(gene);
  }

  private Object name(JsonNode node) {
    return node.path("sections").path("description").path("data").path("fullName").asText();
  }

  private String symbol(JsonNode node) {
    return node.path("name").asText();
  }

  private String id(JsonNode node) {
    return node.path("id").asText();
  }

  private List<String> synonyms(JsonNode node) {
    String symbol = symbol(node);
    JsonNode synonyms = node.path("sections").path("description").path("data").path("synonyms");

    List<String> values = newArrayList();
    for(JsonNode synonym : synonyms) {
      String value = synonym.asText();
      final boolean unique = value.equals(symbol) == false;
      if(unique) {
        values.add(value);
      }
    }

    return values;
  }

  private JsonNode location(JsonNode node) {
    return node.path("sections").path("location").path("data");
  }

  private String canonicalTranscriptId(JsonNode node) {
    return node.path("sections").path("transcripts").path("data").path("canonicalTranscriptId").asText();
  }

  private List<JsonNode> transcripts(JsonNode node) {
    JsonNode records = node.path("sections").path("transcripts").path("data").path("records");

    List<JsonNode> transcripts = newArrayList();
    for(JsonNode record : records) {
      transcripts.add(transcript(record));
    }

    return transcripts;
  }

  private JsonNode transcript(JsonNode record) {
    ObjectNode transcript = mapper.createObjectNode();
    transcript.put("transcript_id", record.path("id").asText());
    transcript.put("transcript_name", record.path("name").asText());
    transcript.put("isCanonical", record.path("isCanonical").asBoolean());
    transcript.put("length", record.path("length").asLong());
    transcript.put("lengthAminoAcid", record.path("lengthAminoAcid").asLong());
    transcript.put("lengthDNA", record.path("lengthDNA").asLong());
    transcript.put("numberOfExons", record.path("numberOfExons").asLong());
    transcript.put("startExon", record.path("startExon").asLong());
    transcript.put("seqExonStart", record.path("seqExonStart").asLong());
    transcript.put("seqExonEnd", record.path("seqExonEnd").asLong());
    transcript.put("endExon", record.path("endExon").asLong());
    transcript.put("translationId", record.path("translationId").asText());
    transcript.put("exons", record.path("exons"));
    transcript.put("domains", record.path("domains"));

    return transcript;
  }

}
