/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.accession.core;

import static lombok.AccessLevel.PRIVATE;

import java.util.Map;

import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Field accessors for accession validation.
 */
@NoArgsConstructor(access = PRIVATE)
public class AccessionFields {

  public static final String ANALYSIS_ID_FIELD_NAME = "analysis_id";
  public static final String ANALYZED_SAMPLE_ID_FIELD_NAME = "analyzed_sample_id";
  public static final String MATCHED_SAMPLE_ID_FIELD_NAME = "matched_sample_id";
  public static final String RAW_DATA_REPOSITORY_FIELD_NAME = "raw_data_repository";
  public static final String RAW_DATA_ACCESSION_FIELD_NAME = "raw_data_accession";

  public static String getAnalysisId(@NonNull Map<String, String> record) {
    return record.get(ANALYSIS_ID_FIELD_NAME);
  }

  public static String getAnalyzedSampleId(@NonNull Map<String, String> record) {
    return record.get(ANALYZED_SAMPLE_ID_FIELD_NAME);
  }

  public static String getMatchedSampleId(@NonNull Map<String, String> record) {
    return record.get(MATCHED_SAMPLE_ID_FIELD_NAME);
  }

  public static String getRawDataRepository(@NonNull Map<String, String> record) {
    return record.get(RAW_DATA_REPOSITORY_FIELD_NAME);
  }

  public static String getRawDataAccession(@NonNull Map<String, String> record) {
    return record.get(RAW_DATA_ACCESSION_FIELD_NAME);
  }

}
