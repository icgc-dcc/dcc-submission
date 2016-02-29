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
package org.icgc.dcc.submission.loader.meta;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;
import static org.icgc.dcc.submission.loader.util.DatabaseFields.PROJECT_ID_FIELD_NAME;

import java.util.Collection;
import java.util.stream.Collectors;

import lombok.val;

import org.icgc.dcc.submission.loader.model.TypeDef;
import org.icgc.dcc.submission.loader.util.Services;
import org.junit.Before;
import org.junit.Test;

public class SubmissionMetadataServiceTest {

  SubmissionMetadataService metadataService;

  @Before
  public void setUp() {
    this.metadataService = Services.createSubmissionService();
  }

  @Test
  public void testGetDictionaryTypeDefs() throws Exception {
    val typeDefs = metadataService.getFileTypes();
    assertThat(typeDefs).hasSize(33);

    val donor = findType(typeDefs, "donor");
    assertThat(donor.getParent()).isEmpty();

    val biomarker = findType(typeDefs, "biomarker");
    assertThat(biomarker.getParent()).hasSize(2);
  }

  private static TypeDef findType(Collection<TypeDef> typeDefs, String type) {
    val typeDef = typeDefs.stream()
        .filter(td -> td.getType().equals(type))
        .collect(Collectors.toList());

    checkState(typeDef.size() == 1, "Failed to resolve type definition. %s", typeDef);

    return typeDef.get(0);
  }

  @Test
  public void testGetFields() throws Exception {
    val donorFields = metadataService.getFields("donor");
    assertThat(donorFields).hasSize(20);
  }

  @Test
  public void testGetChildren() throws Exception {
    assertThat(metadataService.getChildren("donor")).containsOnly("biomarker", "exposure", "family", "specimen",
        "surgery", "therapy");
    assertThat(metadataService.getChildren("biomarker")).isEmpty();
    assertThat(metadataService.getChildren("specimen")).containsOnly("sample", "biomarker", "surgery");
  }

  @Test
  public void testGetParentPrimaryKey() throws Exception {
    assertThat(metadataService.getParentPrimaryKey("specimen", "donor")).containsOnly(entry("donor_id", "donor_id"));
    assertThat(metadataService.getParentPrimaryKey("donor", "specimen")).isEmpty();
    assertThat(metadataService.getParentPrimaryKey("sample", "specimen")).containsOnly(
        entry("specimen_id", "specimen_id"));
    assertThat(metadataService.getParentPrimaryKey("biomarker", "specimen"))
        .containsOnly(entry("donor_id", "donor_id"), entry("specimen_id", "specimen_id"));
    assertThat(metadataService.getParentPrimaryKey("biomarker", "donor")).containsOnly(entry("donor_id", "donor_id"));
  }

  @Test
  public void testGetChildPrimaryKey() throws Exception {
    assertThat(metadataService.getChildPrimaryKey("specimen", "donor")).containsOnly("donor_id");
    assertThat(metadataService.getChildPrimaryKey("donor", "specimen")).isEmpty();
    assertThat(metadataService.getChildPrimaryKey("sample", "specimen")).containsOnly("specimen_id");
    assertThat(metadataService.getChildPrimaryKey("biomarker", "specimen")).containsOnly("donor_id", "specimen_id");
    assertThat(metadataService.getChildPrimaryKey("biomarker", "donor")).containsOnly("donor_id");
  }

  @Test
  public void testGetPrimaryKey() throws Exception {
    assertThat(metadataService.getPrimaryKey("sample")).containsOnly("analyzed_sample_id", PROJECT_ID_FIELD_NAME);
    assertThat(metadataService.getPrimaryKey("ssm_m")).containsOnly("analysis_id", "analyzed_sample_id",
        PROJECT_ID_FIELD_NAME);
    assertThat(metadataService.getPrimaryKey("ssm_p")).containsOnly(PROJECT_ID_FIELD_NAME);
  }

}
