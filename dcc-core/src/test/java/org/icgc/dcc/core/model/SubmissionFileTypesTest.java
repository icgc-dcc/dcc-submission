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

import static org.fest.assertions.api.Assertions.assertThat;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileSubType;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.junit.Test;

public class SubmissionFileTypesTest {

  @Test
  public void test_SubmissionFileType_valid_clinical() {
    assertThat(SubmissionFileType.SSM_M_TYPE.getTypeName()).isEqualTo("ssm_m");
    assertThat(SubmissionFileType.SSM_M_TYPE.getDataType()).isEqualTo(FeatureType.SSM_TYPE);
    assertThat(SubmissionFileType.from("ssm_m")).isEqualTo(SubmissionFileType.SSM_M_TYPE);

    assertThat(SubmissionFileType.MIRNA_S_TYPE.getTypeName()).isEqualTo("mirna_s");
    assertThat(SubmissionFileType.MIRNA_S_TYPE.getDataType()).isEqualTo(FeatureType.MIRNA_TYPE);
    assertThat(SubmissionFileType.from("mirna_s")).isEqualTo(SubmissionFileType.MIRNA_S_TYPE);

    assertThat(SubmissionFileType.DONOR_TYPE.getTypeName()).isEqualTo("donor");
    assertThat(SubmissionFileType.from("donor")).isEqualTo(SubmissionFileType.DONOR_TYPE);
    assertThat(SubmissionFileType.DONOR_TYPE.getDataType()).isEqualTo(ClinicalType.CLINICAL_TYPE);

    assertThat(SubmissionFileType.SPECIMEN_TYPE.getTypeName()).isEqualTo("specimen");
    assertThat(SubmissionFileType.from("specimen")).isEqualTo(SubmissionFileType.SPECIMEN_TYPE);
    assertThat(SubmissionFileType.SPECIMEN_TYPE.getDataType()).isEqualTo(ClinicalType.CLINICAL_TYPE);

    assertThat(SubmissionFileType.BIOMARKER_TYPE.getTypeName()).isEqualTo("biomarker");
    assertThat(SubmissionFileType.from("biomarker")).isEqualTo(SubmissionFileType.BIOMARKER_TYPE);
    assertThat(SubmissionFileType.BIOMARKER_TYPE.getDataType()).isEqualTo(ClinicalType.CLINICAL_TYPE);
  }

  @Test
  public void test_SubmissionFileSubType_valid() {
    assertThat(SubmissionFileSubType.META_SUBTYPE.getAbbreviation()).isEqualTo("m");
    assertThat(SubmissionFileSubType.GENE_SUBTYPE.getAbbreviation()).isEqualTo("g");
    assertThat(SubmissionFileSubType.DONOR_SUBTYPE.getFullName()).isEqualTo("donor");
    assertThat(SubmissionFileSubType.SAMPLE_SUBTYPE.getFullName()).isEqualTo("sample");
    assertThat(SubmissionFileSubType.BIOMARKER_SUBTYPE.getFullName()).isEqualTo("biomarker");
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_meta() {
    SubmissionFileSubType.META_SUBTYPE.getFullName();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_gene() {
    SubmissionFileSubType.GENE_SUBTYPE.getFullName();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid() {
    SubmissionFileSubType.DONOR_SUBTYPE.getAbbreviation();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_sample() {
    SubmissionFileSubType.SAMPLE_SUBTYPE.getAbbreviation();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_biomarker() {
    SubmissionFileSubType.BIOMARKER_SUBTYPE.getAbbreviation();
  }

}
