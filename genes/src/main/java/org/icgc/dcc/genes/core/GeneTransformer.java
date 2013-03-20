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
package org.icgc.dcc.genes.core;

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
    result.put("_gene_id", id(node));
    result.put("symbol", symbol(node));
    result.put("name", name(node));
    result.put("biotype", biotype(node));
    result.put("synonyms", synonyms(node));
    result.put("description", description(node));
    result.put("chromosome", location(node).get("chromosome"));
    result.put("strand", asInteger(location(node).get("strand")));
    result.put("start", asInteger(location(node).get("txStart")));
    result.put("end", asInteger(location(node).get("txEnd")));
    result.put("canonical_transcript_id", canonicalTranscriptId(node));

    // Collection
    result.put("transcripts", transcripts(node));

    return result;
  }

  private JsonNode id(JsonNode node) {
    return node.get("id");
  }

  private JsonNode symbol(JsonNode node) {
    return node.get("name");
  }

  private JsonNode biotype(JsonNode node) {
    return node.path("biotype");
  }

  private JsonNode name(JsonNode node) {
    JsonNode fullName = node.path("sections").path("description").path("data").path("fullName");

    return fullName.isNull() ? symbol(node) : fullName;
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
    transcript.put("id", node.get("id"));
    transcript.put("name", node.get("name"));
    transcript.put("biotype", node.get("biotype"));
    transcript.put("is_canonical", node.get("isCanonical"));
    transcript.put("length", asInteger(node.get("length")));
    transcript.put("length_amino_acid", asInteger(node.get("lengthAminoAcid")));
    transcript.put("length_cds", asInteger(node.get("lengthDNA")));
    transcript.put("number_of_exons", asInteger(node.get("numberOfExons")));
    transcript.put("start_exon", asInteger(node.get("startExon")));
    transcript.put("seq_exon_start", asInteger(node.get("seqExonStart")));
    transcript.put("seq_exon_end", asInteger(node.get("seqExonEnd")));
    transcript.put("end_exon", asInteger(node.get("endExon")));
    transcript.put("translation_id", node.get("translationId"));

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
    exon.put("start", asInteger(node.get("start")));
    exon.put("start_phase", asInteger(node.get("startPhase")));
    exon.put("end", asInteger(node.get("end")));
    exon.put("end_phase", asInteger(node.get("endPhase")));

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
    domain.put("interpro_id", node.get("interproId"));
    domain.put("hit_name", node.get("hitName"));
    domain.put("gff_source", node.get("gffSource"));
    domain.put("description", node.get("description"));
    domain.put("start", asInteger(node.get("start")));
    domain.put("end", asInteger(node.get("end")));

    return domain;
  }

  private static Integer asInteger(JsonNode node) {
    return node == null || node.isNull() ? null : node.asInt();
  }

}
