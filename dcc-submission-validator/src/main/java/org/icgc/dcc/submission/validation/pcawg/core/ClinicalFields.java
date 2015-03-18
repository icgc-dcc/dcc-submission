/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.pcawg.core;

import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_ANALYZED_SAMPLE_ID;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_DONOR_ID;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_SPECIMEN_ID;

import java.util.Map;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ClinicalFields {

  public final String STUDY_FIELD_NAME = "study";

  public static String getDonorId(Map<String, String> record) {
    return record.get(SUBMISSION_DONOR_ID);
  }

  public static String getDonorDonorId(Map<String, String> donor) {
    return donor.get(SUBMISSION_DONOR_ID);
  }

  public static String getSpecimenSpecimenId(Map<String, String> specimen) {
    return specimen.get(SUBMISSION_SPECIMEN_ID);
  }

  public static String getSpecimenDonorId(Map<String, String> specimen) {
    return specimen.get(SUBMISSION_DONOR_ID);
  }

  public static String getSampleSampleId(Map<String, String> sample) {
    return sample.get(SUBMISSION_ANALYZED_SAMPLE_ID);
  }

  public static String getSampleSpecimenId(Map<String, String> sample) {
    return sample.get(SUBMISSION_SPECIMEN_ID);
  }

  public static String getSampleStudy(Map<String, String> sample) {
    return sample.get(STUDY_FIELD_NAME);
  }

}
