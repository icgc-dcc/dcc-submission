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

import static com.google.common.collect.Lists.newArrayList;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.model.FieldNames.NormalizerFieldNames.NORMALIZER_MUTATION;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_ASSEMBLY_VERSION;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME_END;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME_START;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_MUTATION_TYPE;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE;

import java.util.List;

import lombok.NoArgsConstructor;

import com.google.common.collect.ImmutableList;

/**
 * Contains business keys from the standpoint of the loader entities (not the submission files').
 */
@NoArgsConstructor(access = PRIVATE)
public final class BusinessKeys {

  /**
   * Part of the business key for mutations that is found in the meta files and is part of the identifying fields.
   */
  public static final List<String> MUTATION_META_IDENTIFYING_PART = newArrayList(
      SUBMISSION_OBSERVATION_ASSEMBLY_VERSION);

  /**
   * Part of the business key for mutations that is found in the primary files and is part of the identifying fields.
   */
  public static final List<String> MUTATION_PRIMARY_IDENTIFYING_PART = newArrayList(
      SUBMISSION_OBSERVATION_CHROMOSOME,
      SUBMISSION_OBSERVATION_CHROMOSOME_START,
      SUBMISSION_OBSERVATION_CHROMOSOME_END,
      SUBMISSION_OBSERVATION_MUTATION_TYPE,
      NORMALIZER_MUTATION);

  /**
   * Part of the business key for mutations that is found in the primary files and is <b>not</b> part of the identifying
   * fields (redundant info added for convenience).
   */
  public static final List<String> MUTATION_PRIMARY_REDUNDANT_INFO_PART = newArrayList(
      SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE);

  /**
   * Part of the business key for mutations that constitutes the identifying fields.
   */
  public static final List<String> MUTATION_IDENTIFYING_PART = ImmutableList.<String> builder()
      .addAll(MUTATION_PRIMARY_IDENTIFYING_PART)
      .addAll(MUTATION_META_IDENTIFYING_PART)
      .build();

  /**
   * Part of the business key for mutations that is <b>not</b> part of the identifying fields (redundant info added for
   * convenience).
   */
  public static final List<String> MUTATION_REDUNDANT_INFO_PART = ImmutableList.<String> builder()
      .addAll(MUTATION_PRIMARY_REDUNDANT_INFO_PART)
      .build();

  /**
   * Business key for mutations.
   */
  public static final List<String> MUTATION = ImmutableList.<String> builder()
      .addAll(MUTATION_IDENTIFYING_PART)
      .addAll(MUTATION_REDUNDANT_INFO_PART)
      .build();

}
