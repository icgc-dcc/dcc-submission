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
package org.icgc.dcc.reporter;

import static org.icgc.dcc.core.model.FieldNames.OBSERVATION_TYPE;
import static org.icgc.dcc.core.model.FieldNames.PROJECT_ID;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_ANALYZED_SAMPLE_ID;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_DONOR_ID;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_ANALYSIS_ID;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_SEQUENCING_STRATEGY;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_SPECIMEN_ID;
import static org.icgc.dcc.hadoop.cascading.Fields2.getCountFieldCounterpart;
import static org.icgc.dcc.hadoop.cascading.Fields2.getRedundantFieldCounterpart;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import cascading.tuple.Fields;

/**
 * Fields pertaining to the reporter.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class ReporterFields {

  static final Fields TYPE_FIELD = new Fields(OBSERVATION_TYPE);
  static final Fields PROJECT_ID_FIELD = new Fields(PROJECT_ID);
  static final Fields DONOR_ID_FIELD = new Fields(SUBMISSION_DONOR_ID);
  static final Fields SPECIMEN_ID_FIELD = new Fields(SUBMISSION_SPECIMEN_ID);
  static final Fields SAMPLE_ID_FIELD = new Fields(SUBMISSION_ANALYZED_SAMPLE_ID);
  static final Fields ANALYSIS_ID_FIELD = new Fields(SUBMISSION_OBSERVATION_ANALYSIS_ID);
  static final Fields SEQUENCING_STRATEGY_FIELD = new Fields(SUBMISSION_OBSERVATION_SEQUENCING_STRATEGY);

  static final Fields DONOR_UNIQUE_COUNT_FIELD = getCountFieldCounterpart(DONOR_ID_FIELD);
  static final Fields SPECIMEN_UNIQUE_COUNT_FIELD = getCountFieldCounterpart(SPECIMEN_ID_FIELD);
  static final Fields SAMPLE_UNIQUE_COUNT_FIELD = getCountFieldCounterpart(SAMPLE_ID_FIELD);
  static final Fields SEQUENCING_STRATEGY_COUNT_FIELD = getCountFieldCounterpart(SEQUENCING_STRATEGY_FIELD);
  static final Fields _ANALYSIS_OBSERVATION_COUNT_FIELD = getCountFieldCounterpart("analysis_observation");

  static final Fields REDUNDANT_PROJECT_ID_FIELD = getRedundantFieldCounterpart(PROJECT_ID_FIELD);
  static final Fields REDUNDANT_SPECIMEN_ID_FIELD = getRedundantFieldCounterpart(SPECIMEN_ID_FIELD);
  static final Fields REDUNDANT_SAMPLE_ID_FIELD = getRedundantFieldCounterpart(SAMPLE_ID_FIELD);
  static final Fields REDUNDANT_ANALYSIS_ID_FIELD = getRedundantFieldCounterpart(ANALYSIS_ID_FIELD);

}
