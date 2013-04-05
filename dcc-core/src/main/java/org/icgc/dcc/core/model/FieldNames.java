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

public final class FieldNames {

  // @formatter:off
  /**
   * Internal mongo ID field name.
   */
  public static final String MONGO_INTERNAL_ID_FIELD = "_id";
  
  /**
   * The non-surrogate version of _project_id (formerly known as "project_key").
   */
  public static final String PROJECT_ID_FIELD = "project_id";
  
  /**
   * Field names in the original input format.
   */
  public static final String DONOR_ID_FIELD = "donor_id";
  public static final String SPECIMEN_ID_FIELD = "specimen_id";
  public static final String SAMPLE_ID_FIELD = "sample_id";
  public static final String ANALYZED_SAMPLE_ID_FIELD = "analyzed_sample_id";
  public static final String GENE_ID_FIELD = "gene_affected";
  public static final String TRANSCRIPT_ID_FIELD = "transcript_affected";

  /**
   * Project field names.
   */  
  public static final String PROJECT_ID = "_project_id";
  public static final String PROJECT_SUMMARY = "_summary";
  
  /**
   * Donor field names.
   */
  public static final String DONOR_ID = "_donor_id";
  public static final String DONOR_SUMMARY = "_summary";
  public static final String DONOR_GENES = "gene";
  public static final String DONOR_GENE_GENE_ID = "_gene_id";
  public static final String DONOR_GENE_SUMMARY = "_summary";
  public static final String DONOR_PROJECT_ID = "_project_id";
  public static final String DONOR_PROJECT = "project";
  public static final String DONOR_SPECIMEN = "specimen";
  public static final String DONOR_SPECIMEN_ID = "_specimen_id";
  public static final String DONOR_SAMPLE = "sample";
  public static final String DONOR_SAMPLE_ID = "_sample_id";

  /**
   * Gene field names.
   */  
  public static final String GENE_ID = "_gene_id";
  public static final String GENE_DONORS = "donor";
  public static final String GENE_DONOR_DONOR_ID = "_donor_id";
  public static final String GENE_DONOR_SUMMARY = "_summary";
  public static final String GENE_PROJECTS = "project";
  public static final String GENE_PROJECT_PROJECT_ID = "_project_id";
  public static final String GENE_PROJECT_SUMMARY = "_summary";
  public static final String GENE_TRANSCRIPTS = "transcripts";
  public static final String GENE_TRANSCRIPTS_TRANSCRIPT_ID = "id";

  /**
   * Observation field names.
   */  
  public static final String OBSERVATION_ID = "_id";
  public static final String OBSERVATION_MUTATION_ID = "_mutation_id";
  public static final String OBSERVATION_TYPE = "_type";
  public static final String OBSERVATION_DONOR = "donor";
  public static final String OBSERVATION_DONOR_ID = "_donor_id";
  public static final String OBSERVATION_CONSEQUENCE_TYPES = "consequence_type";
  public static final String OBSERVATION_CONSEQUENCES = "consequence";
  public static final String OBSERVATION_CONSEQUENCES_CONSEQUENCE_TYPE = "consequence_type";
  public static final String OBSERVATION_CONSEQUENCES_TRANSCRIPT_ID = "_transcript_id";
  public static final String OBSERVATION_CONSEQUENCES_GENE_ID = "_gene_id";
  public static final String OBSERVATION_CONSEQUENCES_GENE = "gene";
  public static final String OBSERVATION_PLATFORM = "platform";
  public static final String OBSERVATION_VALIDATION_STATUS = "validation_status";
  public static final String OBSERVATION_IS_ANNOTATED = "is_annotated";

  public static final String OBSERVATION_ASSEMBLY_VERSION = "assembly_version";
  public static final String OBSERVATION_ANALYSIS_ID = "analysis_id";
  public static final String OBSERVATION_ANALYZED_SAMPLE_ID = "analyzed_sample_id";
  
  public static final String OBSERVATION_CHROMOSOME = "chromosome";
  public static final String OBSERVATION_CHROMOSOME_START = "chromosome_start";
  public static final String OBSERVATION_CHROMOSOME_END = "chromosome_end";
  public static final String OBSERVATION_MUTATION_TYPE = "mutation_type";
  public static final String OBSERVATION_MUTATION = "mutation";
  public static final String OBSERVATION_REFERENCE_GENOME_ALLELE = "reference_genome_allele";
  
  public static String getPartitionTypeFieldName(String type) {
    return type;
  }

  /**
   * Mutation field names.
   */  
  public static final String MUTATION_ID = "_mutation_id";
  public static final String MUTATION_CONSEQUENCE_TYPES = "consequence_type";
  public static final String MUTATION_OBSERVATIONS = "ssm_occurrence";
  public static final String MUTATION_OBSERVATION_DONOR = "donor";
  public static final String MUTATION_OBSERVATION_PROJECT = "project";
  public static final String MUTATION_TRANSCRIPTS = "transcript";
  public static final String MUTATION_TRANSCRIPTS_GENE = "gene";
  public static final String MUTATION_TRANSCRIPTS_CONSEQUENCE = "consequence";
  public static final String MUTATION_PLATFORM = "platform";
  public static final String MUTATION_IS_ANNOTATED = "is_annotated";
  public static final String MUTATION_VALIDATION_STATUS = "validation_status";
  
  /**
   * Aggregate field names.
   */    
  public static final String SYNTHETIC_PREFIX = "_";
  public static final String TYPE_COUNT_SUFFIX =  "_count";
  public static final String TYPE_EXISTS_SUFFIX = "_exists";  
  public static final String AVAILABLE_DATA_TYPES = "_available_data_type";
  public static final String TOTAL_DONOR_COUNT = "_total_donor_count";
  public static final String TOTAL_SPECIMEN_COUNT = "_total_specimen_count";
  public static final String TOTAL_SAMPLE_COUNT = "_total_sample_count";
  public static final String TESTED_DONOR_COUNT_SUFFIX = "_tested_donor_count";
  public static final String AFFECTED_DONOR_COUNT = "_affected_donor_count";
  // @formatter:on

  public static String getTypeExistsFieldName(String type) {
    return SYNTHETIC_PREFIX + type + TYPE_EXISTS_SUFFIX;
  }

  public static String getTypeCountFieldName(String type) {
    return SYNTHETIC_PREFIX + type + TYPE_COUNT_SUFFIX;
  }

  public static String getTestedTypeCountFieldName(String type) {
    return SYNTHETIC_PREFIX + type + TESTED_DONOR_COUNT_SUFFIX;
  }

  private FieldNames() {
    // Prevent construction
  }

}
