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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.generator.config.GeneratorConfig;
import org.icgc.dcc.generator.core.CoreFileGenerator;
import org.icgc.dcc.generator.core.DataGenerator;
import org.icgc.dcc.generator.core.MetaFileGenerator;
import org.icgc.dcc.generator.core.OptionalFileGenerator;
import org.icgc.dcc.generator.core.PrimaryFileGenerator;
import org.icgc.dcc.generator.core.SecondaryFileGenerator;
import org.icgc.dcc.generator.model.ExperimentalFile;
import org.icgc.dcc.generator.model.OptionalFile;
import org.icgc.dcc.generator.utils.ResourceWrapper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

@Slf4j
public class GeneratorService {

  private static final String DONOR_SCHEMA_NAME = "donor";

  private static final String SAMPLE_SCHEMA_NAME = "sample";

  private static final String SPECIMEN_SCHEMA_NAME = "specimen";

  private static final String META_FILE_TYPE = "m";

  private static final String PRIMARY_FILE_TYPE = "p";

  private static final String EXPRESSION_PRIMARY_FILE_TYPE = "g";

  private static final String SECONDARY_FILE_TYPE = "s";

  @SneakyThrows
  public void generate(GeneratorConfig config, File dictionaryFile, File codeListFile) {
    // Retrieve all configuration parameters
    String outputDirectory = config.getOutputDirectory();
    Integer numberOfDonors = config.getNumberOfDonors();
    Integer numberOfSpecimensPerDonor = config.getNumberOfSpecimensPerDonor();
    Integer numberOfSamplesPerSpecimen = config.getNumberOfSamplesPerSpecimen();
    String leadJurisdiction = config.getLeadJurisdiction();
    String tumourType = config.getTumourType();
    String institution = config.getInstitution();
    String platform = config.getPlatform();
    Long seed = config.getSeed();
    ArrayList<OptionalFile> optionalFiles = config.getOptionalFiles();
    List<ExperimentalFile> experimentalFiles = config.getExperimentalFiles();

    log.info("Checking validity of parameters");
    checkParameters(leadJurisdiction, tumourType, institution, platform);

    log.info("Initializing Dictionary and CodeList files");
    initResources(dictionaryFile, codeListFile);

    generateFiles(outputDirectory, numberOfDonors, numberOfSpecimensPerDonor, numberOfSamplesPerSpecimen,
        leadJurisdiction, tumourType, institution, platform, seed, optionalFiles, experimentalFiles);
  }

  public static void checkParameters(String leadJurisdiction, String tumourType, String institution, String platform)
      throws Exception {
    if(leadJurisdiction.length() != 2) {
      throw new Exception("The lead jurisdiction is invalid");
    }
    if(Integer.parseInt(tumourType) > 31 || Integer.parseInt(tumourType) < 1) {
      throw new Exception("The tumour type is invalid");
    }
    if(Integer.parseInt(institution) > 98 || Integer.parseInt(institution) < 1) {
      throw new Exception("The insitute is invalid");
    }
    if(Integer.parseInt(platform) > 75 || Integer.parseInt(institution) < 1) {
      throw new Exception("The platform is invalid");
    }
  }

  @SneakyThrows
  private void generateFiles(String outputDirectory, Integer numberOfDonors, Integer numberOfSpecimensPerDonor,
      Integer numberOfSamplesPerSpecimen, String leadJurisdiction, String tumourType, String institution,
      String platform, Long seed, ArrayList<OptionalFile> optionalFiles, List<ExperimentalFile> experimentalFiles)
      throws JsonParseException, JsonMappingException, IOException {

    DataGenerator datagen = new DataGenerator(outputDirectory, seed);

    // Generate all core files
    createCoreFile(datagen, DONOR_SCHEMA_NAME, numberOfDonors, leadJurisdiction, institution, tumourType, platform);
    createCoreFile(datagen, SPECIMEN_SCHEMA_NAME, numberOfSpecimensPerDonor, leadJurisdiction, institution, tumourType,
        platform);
    createCoreFile(datagen, SAMPLE_SCHEMA_NAME, numberOfSamplesPerSpecimen, leadJurisdiction, institution, tumourType,
        platform);

    // Generate optional files
    for(OptionalFile optionalFile : optionalFiles) {
      String schemaName = optionalFile.getName();
      Integer numberOfLinesPerDonor = optionalFile.getNumberOfLinesPerDonor();

      datagen.buildPrimaryKey(ResourceWrapper.getSchema(datagen, schemaName));
      createTemplateFile(datagen, schemaName, numberOfLinesPerDonor, leadJurisdiction, institution, tumourType,
          platform);
    }

    // Generate experimental files
    for(ExperimentalFile experimentalFile : experimentalFiles) {
      String fileType = experimentalFile.getFileType();
      String schemaName = experimentalFile.getName() + "_" + fileType;
      Integer numberOfLines = experimentalFile.getNumberOfLinesPerForeignKey();

      datagen.buildPrimaryKey(ResourceWrapper.getSchema(datagen, schemaName));

      if(fileType.equals(META_FILE_TYPE)) {
        createMetaFile(datagen, schemaName, numberOfLines, leadJurisdiction, institution, tumourType, platform);
      } else if(fileType.equals(PRIMARY_FILE_TYPE)) {
        createPrimaryFile(datagen, schemaName, numberOfLines, leadJurisdiction, institution, tumourType, platform);
      } else if(fileType.equals(EXPRESSION_PRIMARY_FILE_TYPE)) {
        createPrimaryFile(datagen, schemaName, numberOfLines, leadJurisdiction, institution, tumourType, platform);
      } else if(fileType.equals(SECONDARY_FILE_TYPE)) {
        createSecondaryFile(datagen, schemaName, numberOfLines, leadJurisdiction, institution, tumourType, platform);
      }
    }
  }

  /**
   * @param dictionaryFile
   * @param codeListFile
   * @throws JsonParseException
   * @throws JsonMappingException
   * @throws IOException
   * @throws JsonProcessingException
   */
  private void initResources(File dictionaryFile, File codeListFile) throws JsonParseException, JsonMappingException,
      IOException, JsonProcessingException {
    if(dictionaryFile != null) {
      ResourceWrapper.initDictionary(dictionaryFile);
    } else {
      ResourceWrapper.initDictionary();
    }
    if(codeListFile != null) {
      ResourceWrapper.initCodeLists(codeListFile);
    } else {
      ResourceWrapper.initCodeLists();
    }
  }

  private static void createCoreFile(DataGenerator datagen, String schemaName, Integer numberOfLinesPerPrimaryKey,
      String leadJurisdiction, String institution, String tumourType, String platform) throws IOException {
    log.info("Creating " + schemaName + " file");
    datagen.buildPrimaryKey(ResourceWrapper.getSchema(datagen, schemaName));
    FileSchema schema = ResourceWrapper.getSchema(datagen, schemaName);
    new CoreFileGenerator().createFile(datagen, schema, numberOfLinesPerPrimaryKey, leadJurisdiction, institution,
        tumourType, platform);
  }

  private static void createTemplateFile(DataGenerator datagen, String schemaName, Integer numberOfLinesPerDonor,
      String leadJurisdiction, String institution, String tumourType, String platform) throws IOException {
    log.info("Creating " + schemaName + " file");
    FileSchema schema = ResourceWrapper.getSchema(datagen, schemaName);
    new OptionalFileGenerator().createFile(datagen, schema, numberOfLinesPerDonor, leadJurisdiction, institution,
        tumourType, platform);
  }

  private static void createMetaFile(DataGenerator datagen, String schemaName, Integer numberOfLinesPerForeignKey,
      String leadJurisdiction, String institution, String tumourType, String platform) throws IOException {
    log.info("Creating " + schemaName + " file");
    FileSchema schema = ResourceWrapper.getSchema(datagen, schemaName);
    new MetaFileGenerator().createFile(datagen, schema, numberOfLinesPerForeignKey, leadJurisdiction, institution,
        tumourType, platform);
  }

  private static void createPrimaryFile(DataGenerator datagen, String schemaName, Integer numberOfLinesPerForeignKey,
      String leadJurisdiction, String institution, String tumourType, String platform) throws IOException {
    log.info("Creating " + schemaName + " file");
    FileSchema schema = ResourceWrapper.getSchema(datagen, schemaName);
    new PrimaryFileGenerator().createFile(datagen, schema, numberOfLinesPerForeignKey, leadJurisdiction, institution,
        tumourType, platform);
  }

  private static void createSecondaryFile(DataGenerator datagen, String schemaName, Integer numberOfLinesPerForeignKey,
      String leadJurisdiction, String institution, String tumourType, String platform) throws IOException {
    log.info("Creating " + schemaName + " file");
    FileSchema schema = ResourceWrapper.getSchema(datagen, schemaName);
    new SecondaryFileGenerator().createFile(datagen, schema, numberOfLinesPerForeignKey, leadJurisdiction, institution,
        tumourType, platform);
  }
}
