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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Transforms from Heliotrope JSON structure to DCC JSON structure
 * 
 * @see https://wiki.oicr.on.ca/display/DCCSOFT/ElasticSearch+Index+Specification#ElasticSearchIndexSpecification-Donor-
 * Genepaircentricindex
 */
public class GeneTransformer {

  private final ObjectMapper mapper = new ObjectMapper();

  public JsonNode transform(JsonNode node) {
    ObjectNode result = mapper.createObjectNode();

    // Simple
    result.set("id", id(node));
    result.set("symbol", symbol(node));
    result.set("name", name(node));
    result.set("synonyms", synonyms(node));
    result.set("description", description(node));
    result.set("chromosome", location(node).path("chromosome"));
    result.set("strand", location(node).path("strand"));
    result.set("start", location(node).path("txStart"));
    result.set("end", location(node).path("txEnd"));
    result.set("canonical_transcript_id", canonicalTranscriptId(node));

    // Collection
    result.put("transcripts", transcripts(node));

    return result;
  }

  private JsonNode id(JsonNode node) {
    return node.path("id");
  }

  private JsonNode symbol(JsonNode node) {
    return node.path("name");
  }

  private JsonNode name(JsonNode node) {
    return node.path("sections").path("description").path("data").path("fullName");
  }

  private JsonNode description(JsonNode node) {
    return node.path("sections").path("description").path("data").path("summary");
  }

  private ArrayNode synonyms(JsonNode node) {
    String symbol = symbol(node).asText();
    JsonNode synonyms = node.path("sections").path("description").path("data").path("synonyms");

    ArrayNode values = mapper.createArrayNode();

    // Project additional values
    for(JsonNode synonym : synonyms) {
      String value = synonym.asText();
      final boolean additional = value.equals(symbol) == false;
      if(additional) {
        values.add(value);
      }
    }

    return values;
  }

  private JsonNode location(JsonNode node) {
    return node.path("sections").path("location").path("data");
  }

  private JsonNode canonicalTranscriptId(JsonNode node) {
    return node.path("sections").path("transcripts").path("data").path("canonicalTranscriptId");
  }

  private ArrayNode transcripts(JsonNode node) {
    JsonNode values = node.path("sections").path("transcripts").path("data").path("records");

    ArrayNode transcripts = mapper.createArrayNode();

    // Project transformed transcripts
    for(JsonNode value : values) {
      transcripts.add(transcript(value));
    }

    return transcripts;
  }

  private JsonNode transcript(JsonNode node) {
    ObjectNode transcript = mapper.createObjectNode();

    // Simple
    transcript.put("id", node.path("id").asText());
    transcript.put("name", node.path("name").asText());
    transcript.put("is_canonical", node.path("isCanonical").asBoolean());
    transcript.put("length", node.path("length").asInt());
    transcript.put("length_amino_acid", node.path("lengthAminoAcid").asInt());
    transcript.put("length_cds", node.path("lengthDNA").asInt());
    transcript.put("number_of_exons", node.path("numberOfExons").asInt());
    transcript.put("start_exon", node.path("startExon").asInt());
    transcript.put("seq_exon_start", node.path("seqExonStart").asInt());
    transcript.put("seq_exon_end", node.path("seqExonEnd").asInt());
    transcript.put("end_exon", node.path("endExon").asInt());
    transcript.put("translation_id", node.path("translationId").asText());

    // Collections
    transcript.put("exons", exons(node));
    transcript.put("domains", domains(node));

    return transcript;
  }

  private JsonNode exons(JsonNode node) {
    JsonNode values = node.path("exons");

    ArrayNode exons = mapper.createArrayNode();
    for(JsonNode value : values) {
      exons.add(exon(value));
    }

    return exons;
  }

  private JsonNode exon(JsonNode node) {
    ObjectNode exon = mapper.createObjectNode();

    // Simple
    exon.put("start", node.path("start").asInt());
    exon.put("start_phase", node.path("startPhase").asInt());
    exon.put("end", node.path("end").asInt());
    exon.put("end_phase", node.path("endPhase").asInt());

    return exon;
  }

  private JsonNode domains(JsonNode node) {
    JsonNode values = node.path("domains");

    ArrayNode domains = mapper.createArrayNode();
    for(JsonNode value : values) {
      final String gffSource = value.path("gffSource").asText();

      // Only add "Pfam" sources - Junjun
      final boolean pFam = "Pfam".equals(gffSource);
      if(pFam) {
        domains.add(domain(value));
      }
    }

    return domains;
  }

  private JsonNode domain(JsonNode node) {
    ObjectNode domain = mapper.createObjectNode();

    // Simple
    domain.put("interpro_id", node.path("interproId").asText());
    domain.put("hit_name", node.path("hitName").asText());
    domain.put("gff_source", node.path("gffSource").asText());
    domain.put("description", node.path("description").asText());
    domain.put("start", node.path("start").asInt());
    domain.put("end", node.path("end").asInt());

    return domain;
  }

}
