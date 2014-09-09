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
import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.icgc.dcc.core.model.FileTypes.FileSubType.BIOMARKER_SUBTYPE;
import static org.icgc.dcc.core.model.FileTypes.FileSubType.DONOR_SUBTYPE;
import static org.icgc.dcc.core.model.FileTypes.FileSubType.META_SUBTYPE;
import static org.icgc.dcc.core.model.FileTypes.FileSubType.SAMPLE_SUBTYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.BIOMARKER_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.CNSM_S_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.DONOR_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.SSM_M_TYPE;
import static org.icgc.dcc.core.model.FileTypes.FileType.from;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.junit.Test;

public class FileTypesTest {

  @Test
  public void test_SubmissionFileType_valid_clinical() {
    assertThat(SSM_M_TYPE.getId()).isEqualTo("ssm_m");
    assertThat(SSM_M_TYPE.getDataType()).isEqualTo(FeatureType.SSM_TYPE);
    assertThat(from("ssm_m")).isEqualTo(SSM_M_TYPE);

    assertThat(CNSM_S_TYPE.getId()).isEqualTo("cnsm_s");
    assertThat(CNSM_S_TYPE.getDataType()).isEqualTo(FeatureType.CNSM_TYPE);
    assertThat(from("cnsm_s")).isEqualTo(CNSM_S_TYPE);

    assertThat(DONOR_TYPE.getId()).isEqualTo("donor");
    assertThat(from("donor")).isEqualTo(DONOR_TYPE);
    assertThat(DONOR_TYPE.getDataType()).isEqualTo(ClinicalType.CLINICAL_CORE_TYPE);

    assertThat(SPECIMEN_TYPE.getId()).isEqualTo("specimen");
    assertThat(from("specimen")).isEqualTo(SPECIMEN_TYPE);
    assertThat(SPECIMEN_TYPE.getDataType()).isEqualTo(ClinicalType.CLINICAL_CORE_TYPE);

    assertThat(BIOMARKER_TYPE.getId()).isEqualTo("biomarker");
    assertThat(from("biomarker")).isEqualTo(BIOMARKER_TYPE);
    assertThat(BIOMARKER_TYPE.getDataType()).isEqualTo(ClinicalType.CLINICAL_OPTIONAL_TYPE);

    assertThat(FileType.MANDATORY_TYPES).isEqualTo(
        newLinkedHashSet(newArrayList(
            DONOR_TYPE,
            SPECIMEN_TYPE,
            SAMPLE_TYPE
        )));

  }

  @Test
  public void test_SubmissionFileSubType_valid() {
    assertThat(META_SUBTYPE.getAbbreviation()).isEqualTo("m");
    assertThat(DONOR_SUBTYPE.getFullName()).isEqualTo("donor");
    assertThat(SAMPLE_SUBTYPE.getFullName()).isEqualTo("sample");
    assertThat(BIOMARKER_SUBTYPE.getFullName()).isEqualTo("biomarker");
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_meta() {
    META_SUBTYPE.getFullName();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid() {
    DONOR_SUBTYPE.getAbbreviation();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_sample() {
    SAMPLE_SUBTYPE.getAbbreviation();
  }

  @Test(expected = IllegalStateException.class)
  public void test_SubmissionFileSubType_invalid_biomarker() {
    BIOMARKER_SUBTYPE.getAbbreviation();
  }

}
