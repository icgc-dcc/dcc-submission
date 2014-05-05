/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS AS IS AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.core.model;

import static lombok.AccessLevel.PRIVATE;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

@Getter
@RequiredArgsConstructor(access = PRIVATE)
public enum ConsequenceType {

  /**
   * In order of increasing priority.
   */
  CUSTOM("custom"),
  INTERGENIC_REGION("intergenic_region"),
  INTERGENIC_VARIANT("intergenic_variant"),
  INTERGENIC("intergenic"),
  INTRON_VARIANT("intron_variant"),
  DOWNSTREAM_GENE_VARIANT("downstream_gene_variant"),
  THREE_PRIME_UTR_VARIANT("3_prime_UTR_variant"),
  STOP_RETAINED_VARIANT("stop_retained_variant"),
  SYNONYMOUS_VARIANT("synonymous_variant"),
  UPSTREAM_GENE_VARIANT("upstream_gene_variant"),
  FIVE_PRIME_UTR("five_prime_UTR"),
  FIVE_PRIME_UTR_VARIANT("5_prime_UTR_variant"),
  NC_TRANSCRIPT_VARIANT("nc_transcript_variant"),
  NON_CODING_EXON_VARIANT("non_coding_exon_variant"),
  MICRO_RNA("micro-rna"),
  REGULATORY_REGION_VARIANT("regulatory_region_variant"),
  SPLICE_REGION_VARIANT("splice_region_variant"),
  INFRAME_INSERTION("inframe_insertion"),
  INFRAME_DELETION("inframe_deletion"),
  CODING_SEQUENCE_VARIANT("coding_sequence_variant"),
  EXON_LOST("exon_lost"),
  START_GAINED("start_gained"),
  STOP_LOST("stop_lost"),
  STOP_GAINED("stop_gained"),
  INITIATOR_CODON_VARIANT("initiator_codon_variant"),
  NON_CONSERVATIVE_MISSENSE_VARIANT("non_conservative_missense_variant"),
  MISSENSE("missense"),
  FRAMESHIFT_VARIANT("frameshift_variant");

  private final String id;

  public static ConsequenceType byId(@NonNull String id) {
    for (val value : values()) {
      if (value.getId().equals(id)) {
        return value;
      }
    }

    throw new IllegalArgumentException("Unknown id '" + id + "'  for " + ConsequenceType.class);
  }

  public int getPriority() {
    return ordinal();
  }

  @Override
  public String toString() {
    return id;
  }

}
