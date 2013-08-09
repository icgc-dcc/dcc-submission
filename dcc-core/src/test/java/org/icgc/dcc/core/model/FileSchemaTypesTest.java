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
import org.icgc.dcc.core.model.FileSchemaTypes.FileSchemaType;
import org.icgc.dcc.core.model.FileSchemaTypes.SubmissionSubType;
import org.junit.Test;

public class FileSchemaTypesTest {

  @Test
  public void test_FileSchemaType_valid_clinical() {
    assertThat(FileSchemaType.SSM_M_TYPE.getTypeName()).isEqualTo("ssm_m");
    assertThat(FileSchemaType.SSM_M_TYPE.getDataType()).isEqualTo(FeatureType.SSM_TYPE);
    assertThat(FileSchemaType.from("ssm_m")).isEqualTo(FileSchemaType.SSM_M_TYPE);

    assertThat(FileSchemaType.MIRNA_S_TYPE.getTypeName()).isEqualTo("mirna_s");
    assertThat(FileSchemaType.MIRNA_S_TYPE.getDataType()).isEqualTo(FeatureType.MIRNA_TYPE);
    assertThat(FileSchemaType.from("mirna_s")).isEqualTo(FileSchemaType.MIRNA_S_TYPE);

    assertThat(FileSchemaType.DONOR_TYPE.getTypeName()).isEqualTo("donor");
    assertThat(FileSchemaType.from("donor")).isEqualTo(FileSchemaType.DONOR_TYPE);
    assertThat(FileSchemaType.DONOR_TYPE.getDataType()).isEqualTo(ClinicalType.CLINICAL_TYPE);

    assertThat(FileSchemaType.SPECIMEN_TYPE.getTypeName()).isEqualTo("specimen");
    assertThat(FileSchemaType.from("specimen")).isEqualTo(FileSchemaType.SPECIMEN_TYPE);
    assertThat(FileSchemaType.SPECIMEN_TYPE.getDataType()).isEqualTo(ClinicalType.CLINICAL_TYPE);

    assertThat(FileSchemaType.BIOMARKER_TYPE.getTypeName()).isEqualTo("biomarker");
    assertThat(FileSchemaType.from("biomarker")).isEqualTo(FileSchemaType.BIOMARKER_TYPE);
    assertThat(FileSchemaType.BIOMARKER_TYPE.getDataType()).isEqualTo(ClinicalType.CLINICAL_TYPE);
  }

  @Test
  public void test_SubmissionFileSubType_valid() {
    assertThat(SubmissionSubType.META_SUBTYPE.getAbbreviation()).isEqualTo("m");
    assertThat(SubmissionSubType.GENE_SUBTYPE.getAbbreviation()).isEqualTo("g");
    assertThat(SubmissionSubType.DONOR_SUBTYPE.getFullName()).isEqualTo("donor");
    assertThat(SubmissionSubType.SAMPLE_SUBTYPE.getFullName()).isEqualTo("sample");
    assertThat(SubmissionSubType.BIOMARKER_SUBTYPE.getFullName()).isEqualTo("biomarker");
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_meta() {
    SubmissionSubType.META_SUBTYPE.getFullName();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_gene() {
    SubmissionSubType.GENE_SUBTYPE.getFullName();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid() {
    SubmissionSubType.DONOR_SUBTYPE.getAbbreviation();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_sample() {
    SubmissionSubType.SAMPLE_SUBTYPE.getAbbreviation();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_biomarker() {
    SubmissionSubType.BIOMARKER_SUBTYPE.getAbbreviation();
  }

}
