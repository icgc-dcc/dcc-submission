/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 *
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
package org.icgc.dcc.generator.service;

import static org.icgc.dcc.generator.model.ExperimentalFile.FileType.EXPRESSION_PRIMARY;
import static org.icgc.dcc.generator.model.ExperimentalFile.FileType.META;
import static org.icgc.dcc.generator.model.ExperimentalFile.FileType.PRIMARY;
import static org.icgc.dcc.generator.model.ExperimentalFile.FileType.SECONDARY;
import static org.icgc.dcc.generator.service.GeneratorService.generatorService;
import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.common.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.generator.model.ExperimentalFile;
import org.icgc.dcc.generator.model.ExperimentalFile.FileType;
import org.icgc.dcc.generator.utils.FileSchemas;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@Ignore
public class GeneratorServiceTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private static final FileSchemas schemas = new FileSchemas();

  @Test
  public void testFeatureTypes() {
    val builder = generatorService()
        .outputDirectory(outputDirectory())
        .seed(1234567L)

        .leadJurisdiction("au")
        .institution("001")
        .platform("1")
        .tumourType("01")

        .numberOfDonors(1)
        .numberOfSpecimensPerDonor(1)
        .numberOfSamplesPerSpecimen(2);

    for (FeatureType type : FeatureType.values()) {
      addMetaFile(builder, type.getId());
      addPrimaryFile(builder, type.getId());
      addSecondaryFile(builder, type.getId());
    }

    builder.build().generateFiles();
  }

  /**
   * TODO: change to use {@link FeatureType} instead of String (can use {@link FeatureType#from(String)} to help).
   */
  private GeneratorService.Builder addMetaFile(GeneratorService.Builder builder, String featureType) {
    if (schemas.getSchema(featureType + "_m") != null) {
      addExperimentalFile(builder, featureType, META);
    }

    return builder;
  }

  /**
   * TODO: change to use {@link FeatureType} instead of String (can use {@link FeatureType#from(String)} to help).
   */
  private GeneratorService.Builder addPrimaryFile(GeneratorService.Builder builder, String featureType) {
    if (schemas.getSchema(featureType + "_p") != null || schemas.getSchema(featureType + "_g") != null) {
      // TODO: Encapsulate
      FileType fileType = schemas.getSchema(featureType + "_p") != null ? PRIMARY : EXPRESSION_PRIMARY;
      addExperimentalFile(builder, featureType, fileType);
    }

    return builder;
  }

  /**
   * TODO: change to use {@link FeatureType} instead of String (can use {@link FeatureType#from(String)} to help).
   */
  private GeneratorService.Builder addSecondaryFile(GeneratorService.Builder builder, String featureType) {
    if (schemas.getSchema(featureType + "_s") != null) {
      addExperimentalFile(builder, featureType, SECONDARY);
    }

    return builder;
  }

  private void addExperimentalFile(GeneratorService.Builder builder, String featureType,
      ExperimentalFile.FileType fileType) {
    builder.addExperimentalFile(
        ExperimentalFile.builder()
            .name(featureType)
            .fileType(fileType)
            .numberOfLinesPerForeignKey(1)
            .build());
  }

  @SneakyThrows
  private String outputDirectory() {
    return tmp.newFolder().getAbsolutePath();
  }

}
