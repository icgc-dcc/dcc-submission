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
package org.icgc.dcc.core.model;

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public final class FieldNames {

  /**
   * Internal mongo ID field name.
   */
  public static final String MONGO_INTERNAL_ID_FIELD = "_id";

  /**
   * Field names in the original input format.
   */
  public static final String DONOR_ID_FIELD = "donor_id";
  public static final String SPECIMEN_ID_FIELD = "specimen_id";
  public static final String SAMPLE_ID_FIELD = "sample_id";
  public static final String ANALYZED_SAMPLE_ID_FIELD = "analyzed_sample_id";
  public static final String MATCHED_SAMPLE_ID_FIELD = "matched_sample_id";
  public static final String GENE_ID_FIELD = "gene_affected";
  public static final String TRANSCRIPT_ID_FIELD = "transcript_affected";

  /**
   * Project field names.
   */
  public static final String PROJECT_ID = "_project_id"; // Same as project_code for now
  public static final String PROJECT_CODE = "project_code"; // "BRCA-UK" for instance
  public static final String PROJECT_FORMER_SUBMISSION_ID = "former_submission_id"; // "1133.0" for instance
  public static final String PROJECT_INTERNAL_ID = "internal_id"; // Not really used for now... "PR3" for instance
  public static final String PROJECT_ICGC_ID = "icgc_id"; // "1133" for instance
  public static final String PROJECT_ALIAS = "alias";
  public static final String PROJECT_COLOUR = "colour";
  public static final String PROJECT_DISPLAY_NAME = "project_name";
  public static final String PROJECT_TUMOUR_TYPE = "tumour_type";
  public static final String PROJECT_TUMOUR_SUBTYPE = "tumour_subtype";
  public static final String PROJECT_PRIMARY_SITE = "primary_site";
  public static final String PROJECT_COUNTRIES = "countries";
  public static final String PROJECT_PUBMED_IDS = "pubmed_ids";
  public static final String PROJECT_SUMMARY = "_summary";

  /**
   * Special fields, usually appearing in all clinical files as well as primary/meta experimental files.
   */
  public static final String URI = "uri";
  public static final String DB_XREF = "db_xref";

  /**
   * Donor field names.
   */
  public static final String DONOR_ID = "_donor_id";

  public static final String DONOR_SUMMARY = "_summary";
  public static final String DONOR_SUMMARY_EXPERIMENTAL_ANALYSIS = "experimental_analysis_performed";
  public static final String DONOR_SUMMARY_AFFECTED_GENE_COUNT = "_affected_gene_count";

  public static final String DONOR_GENES = "gene";
  public static final String DONOR_GENE_GENE_ID = "_gene_id";
  public static final String DONOR_GENE_SUMMARY = "_summary";
  public static final String DONOR_PROJECT_ID = PROJECT_ID;
  public static final String DONOR_PROJECT = "project";
  public static final String DONOR_SPECIMEN = "specimen";
  public static final String DONOR_SPECIMEN_ID = "_specimen_id";
  public static final String DONOR_SAMPLE = "sample";
  public static final String DONOR_SAMPLE_ID = "_sample_id";
  public static final String DONOR_CONTROL_SAMPLE_ID = "_matched_sample_id";
  public static final String DONOR_SAMPLE_ANALYZED_SAMPLE_ID = "analyzed_sample_id";
  public static final String DONOR_SAMPLE_SEQUENCE_DATA = "available_raw_sequence_data";

  /**
   * CGHub field names.
   */
  public static final String SEQUENCE_DATA_LEGACY_SAMPLE_ID = "legacy_sample_id";
  public static final String SEQUENCE_DATA_LIBRARY_STRATEGY = "library_strategy";

  /**
   * Gene field names.
   */
  public static final String GENE_ID = "_gene_id";
  public static final String GENE_DONORS = "donor";
  public static final String GENE_DONOR_DONOR_ID = "_donor_id";
  public static final String GENE_DONOR_PROJECT = "project";
  public static final String GENE_DONOR_SUMMARY = "_summary";
  public static final String GENE_PROJECTS = "project";
  public static final String GENE_PROJECT_PROJECT_ID = PROJECT_ID;
  public static final String GENE_PROJECT_SUMMARY = "_summary";
  public static final String GENE_CANONICAL_TRANSCRIPT_ID = "canonical_transcript_id";
  public static final String GENE_TRANSCRIPTS = "transcripts";
  public static final String GENE_TRANSCRIPTS_TRANSCRIPT_ID = "id";
  public static final String GENE_TRANSCRIPTS_TRANSCRIPT_EXONS = "exons";
  public static final String GENE_TRANSCRIPTS_TRANSCRIPT_DOMAINS = "domains";

  public static final String GENE_SUMMARY = "_summary";
  public static final String GENE_SUMMARY_AFFECTED_TRANSCRIPT_IDS = "_affected_transcript_id";
  public static final String GENE_SUMMARY_AFFECTED_PROJECT_COUNT = "_affected_project_count";
  public static final String GENE_SUMMARY_AFFECTED_DONOR_COUNT = "_affected_donor_count";
  public static final String GENE_SUMMARY_UNIQUE_MUTATION_COUNT = "_unique_mutation_count";
  public static final String GENE_SUMMARY_TOTAL_MUTATION_COUNT = "_total_mutation_count";

  /**
   * Observation field names.
   */
  public static final String OBSERVATION_ID = "_id";
  public static final String OBSERVATION_MUTATION_ID = "_mutation_id";
  public static final String OBSERVATION_TYPE = "_type";
  public static final String OBSERVATION_DONOR = "donor";
  public static final String OBSERVATION_DONOR_ID = "_donor_id";
  public static final String OBSERVATION_PROJECT = "project";
  public static final String OBSERVATION_CONSEQUENCE_TYPES = "consequence_type";
  public static final String OBSERVATION_CONSEQUENCES = "consequence";
  public static final String OBSERVATION_CONSEQUENCES_CONSEQUENCE_TYPE = "consequence_type";
  public static final String OBSERVATION_CONSEQUENCES_CONSEQUENCE_CANONICAL = "_is_canonical_transcript";
  public static final String OBSERVATION_CONSEQUENCES_TRANSCRIPT_ID = "_transcript_id";
  public static final String OBSERVATION_CONSEQUENCES_GENE_ID = "_gene_id";
  public static final String OBSERVATION_CONSEQUENCES_GENE = "gene";
  public static final String OBSERVATION_PLATFORM = "platform";
  public static final String OBSERVATION_VALIDATION_STATUS = "validation_status";
  public static final String OBSERVATION_VALIDATION_PLATFORM = "validation_platform";
  public static final String OBSERVATION_VERIFICATION_STATUS = "verification_status";
  public static final String OBSERVATION_VERIFICATION_PLATFORM = "verification_platform";
  public static final String OBSERVATION_IS_ANNOTATED = "is_annotated";

  public static final String OBSERVATION_ASSEMBLY_VERSION = "assembly_version";
  public static final String OBSERVATION_ANALYSIS_ID = "analysis_id";
  public static final String OBSERVATION_ANALYZED_SAMPLE_ID = "analyzed_sample_id";

  public static final String OBSERVATION_CHROMOSOME = "chromosome";
  public static final String OBSERVATION_CHROMOSOME_START = "chromosome_start";
  public static final String OBSERVATION_CHROMOSOME_END = "chromosome_end";
  public static final String OBSERVATION_MUTATION_TYPE = "mutation_type";
  public static final String OBSERVATION_MUTATION = "mutation";

  public static String getPartitionTypeFieldName(String type) {
    return type;
  }

  /**
   * Mutation field names.
   */
  public static final String MUTATION_ID = "_mutation_id";
  public static final String MUTATION_OBSERVATIONS = "ssm_occurrence";
  public static final String MUTATION_OBSERVATION_DONOR = "donor";
  public static final String MUTATION_OBSERVATION_PROJECT = "project";
  public static final String MUTATION_TRANSCRIPTS = "transcript";
  public static final String MUTATION_TRANSCRIPTS_GENE = "gene";
  public static final String MUTATION_TRANSCRIPTS_CONSEQUENCE = "consequence";

  // TODO: Move to summary
  public static final String MUTATION_CONSEQUENCE_TYPES = "consequence_type";
  public static final String MUTATION_PLATFORM = "platform";
  public static final String MUTATION_IS_ANNOTATED = "is_annotated";
  public static final String MUTATION_VALIDATION_STATUS = "validation_status";

  public static final String MUTATION_SUMMARY = "_summary";
  public static final String MUTATION_SUMMARY_AFFECTED_PROJECT_IDS = "_affected_project_id";
  public static final String MUTATION_SUMMARY_AFFECTED_PROJECT_COUNT = "_affected_project_count";
  public static final String MUTATION_SUMMARY_AFFECTED_DONOR_COUNT = "_affected_donor_count";

  /**
   * Aggregate field names.
   */
  public static final String SYNTHETIC_PREFIX = "_";
  public static final String TYPE_COUNT_SUFFIX = "_count";
  public static final String TYPE_EXISTS_SUFFIX = "_exists";
  public static final String AVAILABLE_DATA_TYPES = "_available_data_type";
  public static final String TOTAL_DONOR_COUNT = "_total_donor_count";
  public static final String TOTAL_SPECIMEN_COUNT = "_total_specimen_count";
  public static final String TOTAL_SAMPLE_COUNT = "_total_sample_count";
  public static final String TESTED_DONOR_COUNT_SUFFIX = "_tested_donor_count";
  public static final String AFFECTED_DONOR_COUNT = "_affected_donor_count";
  public static final String EXPERIMENTAL_ANALYSIS_PERFORMED = "experimental_analysis_performed";

  public static String getTypeExistsFieldName(String type) {
    return SYNTHETIC_PREFIX + type + TYPE_EXISTS_SUFFIX;
  }

  public static String getTypeCountFieldName(String type) {
    return SYNTHETIC_PREFIX + type + TYPE_COUNT_SUFFIX;
  }

  public static String getTestedTypeCountFieldName(String type) {
    return SYNTHETIC_PREFIX + type + TESTED_DONOR_COUNT_SUFFIX;
  }

}
