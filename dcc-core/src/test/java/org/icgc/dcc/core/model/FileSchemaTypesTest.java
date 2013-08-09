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
    assertThat(FileSchemaType.SSM_M.getTypeName()).isEqualTo("ssm_m");
    assertThat(FileSchemaType.SSM_M.getDataType()).isEqualTo(FeatureType.SSM_TYPE);
    assertThat(FileSchemaType.from("ssm_m")).isEqualTo(FileSchemaType.SSM_M);

    assertThat(FileSchemaType.MIRNA_S.getTypeName()).isEqualTo("mirna_s");
    assertThat(FileSchemaType.MIRNA_S.getDataType()).isEqualTo(FeatureType.MIRNA_TYPE);
    assertThat(FileSchemaType.from("mirna_s")).isEqualTo(FileSchemaType.MIRNA_S);

    assertThat(FileSchemaType.DONOR.getTypeName()).isEqualTo("donor");
    assertThat(FileSchemaType.from("donor")).isEqualTo(FileSchemaType.DONOR);
    assertThat(FileSchemaType.DONOR.getDataType()).isEqualTo(ClinicalType.CLINICAL_TYPE);

    assertThat(FileSchemaType.SPECIMEN.getTypeName()).isEqualTo("specimen");
    assertThat(FileSchemaType.from("specimen")).isEqualTo(FileSchemaType.SPECIMEN);
    assertThat(FileSchemaType.SPECIMEN.getDataType()).isEqualTo(ClinicalType.CLINICAL_TYPE);

    assertThat(FileSchemaType.BIOMARKER.getTypeName()).isEqualTo("biomarker");
    assertThat(FileSchemaType.from("biomarker")).isEqualTo(FileSchemaType.BIOMARKER);
    assertThat(FileSchemaType.BIOMARKER.getDataType()).isEqualTo(ClinicalType.CLINICAL_TYPE);
  }

  @Test
  public void test_SubmissionFileSubType_valid() {
    assertThat(SubmissionSubType.META.getAbbreviation()).isEqualTo("m");
    assertThat(SubmissionSubType.GENE.getAbbreviation()).isEqualTo("g");
    assertThat(SubmissionSubType.DONOR.getFullName()).isEqualTo("donor");
    assertThat(SubmissionSubType.SAMPLE.getFullName()).isEqualTo("sample");
    assertThat(SubmissionSubType.BIOMARKER.getFullName()).isEqualTo("biomarker");
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_meta() {
    SubmissionSubType.META.getFullName();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_gene() {
    SubmissionSubType.GENE.getFullName();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid() {
    SubmissionSubType.DONOR.getAbbreviation();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_sample() {
    SubmissionSubType.SAMPLE.getAbbreviation();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_biomarker() {
    SubmissionSubType.BIOMARKER.getAbbreviation();
  }

}
