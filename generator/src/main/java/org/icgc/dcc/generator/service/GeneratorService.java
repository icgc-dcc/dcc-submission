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
package org.icgc.dcc.generator.service;

import java.io.IOException;
import java.util.List;

import lombok.SneakyThrows;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.icgc.dcc.generator.config.GeneratorConfig;
import org.icgc.dcc.generator.core.DataGenerator;
import org.icgc.dcc.generator.model.ExperimentalFile;

public class GeneratorService {

  private static final String DONOR_SCHEMA_NAME = "donor";

  private static final String SAMPLE_SCHEMA_NAME = "sample";

  private static final String SPECIMEN_SCHEMA_NAME = "specimen";

  private static final String META_FILE_TYPE = "m";

  private static final String PRIMARY_FILE_TYPE = "p";

  private static final String EXPRESSION_PRIMARY_FILE_TYPE = "g";

  private static final String SECONDARY_FILE_TYPE = "s";

  @SneakyThrows
  public void generate(GeneratorConfig config) {
    String outputDirectory = config.getOutputDirectory();
    Integer numberOfDonors = config.getNumberOfDonors();
    Integer numberOfSpecimensPerDonor = config.getNumberOfSpecimensPerDonor();
    Integer numberOfSamplesPerDonor = config.getNumberOfSamplesPerSpecimen();
    String leadJurisdiction = config.getLeadJurisdiction();
    String tumourType = config.getTumourType();
    String institution = config.getInstitution();
    String platform = config.getPlatform();
    Long seed = config.getSeed();
    // ArrayList<OptionalFile> optionalFiles = config.getOptionalFiles();
    List<ExperimentalFile> experimentalFiles = config.getExperimentalFiles();

    List<String> errors = DataGenerator.checkParameters(leadJurisdiction, tumourType, institution, platform);
    for(String error : errors) {
      throw new Exception(error); // Does this end the whole program?
    }

    generateFiles(outputDirectory, numberOfDonors, numberOfSpecimensPerDonor, numberOfSamplesPerDonor,
        leadJurisdiction, tumourType, institution, platform, seed, experimentalFiles);
  }

  private void generateFiles(String outputDirectory, Integer numberOfDonors, Integer numberOfSpecimensPerDonor,
      Integer numberOfSamplesPerDonor, String leadJurisdiction, String tumourType, String institution, String platform,
      Long seed, List<ExperimentalFile> experimentalFiles) throws JsonParseException, JsonMappingException, IOException {

    DataGenerator.init(outputDirectory, seed);

    DataGenerator
        .createCoreFile(DONOR_SCHEMA_NAME, numberOfDonors, leadJurisdiction, institution, tumourType, platform);
    DataGenerator.createCoreFile(SPECIMEN_SCHEMA_NAME, numberOfSamplesPerDonor, leadJurisdiction, institution,
        tumourType, platform);
    DataGenerator.createCoreFile(SAMPLE_SCHEMA_NAME, numberOfSpecimensPerDonor, leadJurisdiction, institution,
        tumourType, platform);

    for(ExperimentalFile experimentalFile : experimentalFiles) {
      String fileType = experimentalFile.getFileType();
      String schemaName = experimentalFile.getName() + "_" + fileType;
      Integer numberOfLines = experimentalFile.getNumberOfLinesPerForeignKey();

      DataGenerator.determineUniqueFields(DataGenerator.getSchema(schemaName));
      if(fileType.equals(META_FILE_TYPE)) {
        DataGenerator.createMetaFile(schemaName, numberOfLines, leadJurisdiction, institution, tumourType, platform);
      } else if(fileType.equals(PRIMARY_FILE_TYPE)) {
        DataGenerator.createPrimaryFile(schemaName, numberOfLines, leadJurisdiction, institution, tumourType, platform);
      } else if(fileType.equals(EXPRESSION_PRIMARY_FILE_TYPE)) {
        DataGenerator.createPrimaryFile(schemaName, numberOfLines, leadJurisdiction, institution, tumourType, platform);
      } else if(fileType.equals(SECONDARY_FILE_TYPE)) {
        DataGenerator.createSecondaryFile(schemaName, numberOfLines, leadJurisdiction, institution, tumourType,
            platform);
      }
    }
  }

}
