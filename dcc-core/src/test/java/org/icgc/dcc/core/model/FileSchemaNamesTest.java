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
import org.icgc.dcc.core.model.FileSchemaNames.FileSchemaType;
import org.icgc.dcc.core.model.FileSchemaNames.SubmissionFileSubType;
import org.junit.Test;

import com.google.common.base.Optional;

public class FileSchemaNamesTest {

  @Test
  public void test_FileSchemaType_valid_clinical() {
    assertThat(FileSchemaType.SSM_M.getTypeName()).isEqualTo("ssm_m");
    assertThat(FileSchemaType.SSM_M.getType()).isEqualTo(FeatureType.SSM_TYPE);
    assertThat(FileSchemaType.from("ssm_m")).isEqualTo(FileSchemaType.SSM_M);

    assertThat(FileSchemaType.MIRNA_S.getTypeName()).isEqualTo("mirna_s");
    assertThat(FileSchemaType.MIRNA_S.getType()).isEqualTo(FeatureType.MIRNA_TYPE);
    assertThat(FileSchemaType.from("mirna_s")).isEqualTo(FileSchemaType.MIRNA_S);

    assertThat(FileSchemaType.DONOR.getTypeName()).isEqualTo("donor");
    assertThat(FileSchemaType.from("donor")).isEqualTo(FileSchemaType.DONOR);
    assertThat(FileSchemaType.DONOR.getType()).isEqualTo(ClinicalType.CLINICAL_TYPE);

    assertThat(FileSchemaType.SPECIMEN.getTypeName()).isEqualTo("specimen");
    assertThat(FileSchemaType.from("specimen")).isEqualTo(FileSchemaType.SPECIMEN);
    assertThat(FileSchemaType.SPECIMEN.getType()).isEqualTo(ClinicalType.CLINICAL_TYPE);

    assertThat(FileSchemaType.BIOMARKER.getTypeName()).isEqualTo("biomarker");
    assertThat(FileSchemaType.from("biomarker")).isEqualTo(FileSchemaType.BIOMARKER);
    assertThat(FileSchemaType.BIOMARKER.getType()).isEqualTo(ClinicalType.CLINICAL_TYPE);
  }

  @Test
  public void test_SubmissionFileSubType_valid_clinical() {
    // assertThat(SubmissionFileSubType.fromFileSchemaType(FileSchemaType.DONOR))
    // .isEqualTo(SubmissionFileSubType.DONOR);
    // assertThat(SubmissionFileSubType.fromFileSchemaType(FileSchemaType.SPECIMEN))
    // .isEqualTo(SubmissionFileSubType.SPECIMEN);
    // assertThat(SubmissionFileSubType.fromFileSchemaType(FileSchemaType.SAMPLE))
    // .isEqualTo(SubmissionFileSubType.SAMPLE);
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid() {
    SubmissionFileSubType.DONOR.getAbbreviation();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_specimen() {
    SubmissionFileSubType.SPECIMEN.getAbbreviation();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_sample() {
    SubmissionFileSubType.SAMPLE.getAbbreviation();
  }

  @Test
  public void test_SubmissionFileSubType_valid_non_clinical() {
    assertThat(SubmissionFileSubType.META.getAbbreviation())
        .isEqualTo("m");
    assertThat(SubmissionFileSubType.PRIMARY.getAbbreviation())
        .isEqualTo("p");
    assertThat(SubmissionFileSubType.SECONDARY.getAbbreviation())
        .isEqualTo("s");
    assertThat(SubmissionFileSubType.GENE.getAbbreviation())
        .isEqualTo("g");

    assertThat(FileSchemaType.from(FeatureType.SSM_TYPE, SubmissionFileSubType.META))
        .isEqualTo(FileSchemaType.SSM_M);
    assertThat(FileSchemaType.from(FeatureType.CNSM_TYPE, SubmissionFileSubType.PRIMARY))
        .isEqualTo(FileSchemaType.CNSM_P);
    assertThat(FileSchemaType.from(FeatureType.MIRNA_TYPE, SubmissionFileSubType.SECONDARY))
        .isEqualTo(FileSchemaType.MIRNA_S);
    assertThat(FileSchemaType.from(FeatureType.EXP_TYPE, SubmissionFileSubType.GENE))
        .isEqualTo(FileSchemaType.EXP_G);

    assertThat(SubmissionFileSubType.fromAbbreviation("m"))
        .isEqualTo(Optional.of(SubmissionFileSubType.META));
    assertThat(SubmissionFileSubType.fromAbbreviation("p"))
        .isEqualTo(Optional.of(SubmissionFileSubType.PRIMARY));
    assertThat(SubmissionFileSubType.fromAbbreviation("s"))
        .isEqualTo(Optional.of(SubmissionFileSubType.SECONDARY));
    assertThat(SubmissionFileSubType.fromAbbreviation("g"))
        .isEqualTo(Optional.of(SubmissionFileSubType.GENE));

    // assertThat(SubmissionFileSubType.fromFileSchemaType(FileSchemaType.SSM_M))
    // .isEqualTo(SubmissionFileSubType.META);
    // assertThat(SubmissionFileSubType.fromFileSchemaType(FileSchemaType.CNSM_P))
    // .isEqualTo(SubmissionFileSubType.PRIMARY);
    // assertThat(SubmissionFileSubType.fromFileSchemaType(FileSchemaType.MIRNA_S))
    // .isEqualTo(SubmissionFileSubType.SECONDARY);
    // assertThat(SubmissionFileSubType.fromFileSchemaType(FileSchemaType.EXP_G))
    // .isEqualTo(SubmissionFileSubType.GENE);
  }
}
