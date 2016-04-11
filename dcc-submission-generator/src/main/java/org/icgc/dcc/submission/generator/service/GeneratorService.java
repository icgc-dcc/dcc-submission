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
package org.icgc.dcc.submission.generator.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.repeat;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.sort;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.icgc.dcc.common.core.model.FileTypes.FileSubType.META_SUBTYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileSubType.PRIMARY_SUBTYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileSubType.SECONDARY_SUBTYPE;
import static org.icgc.dcc.submission.generator.utils.Dictionaries.DONOR_SCHEMA_NAME;
import static org.icgc.dcc.submission.generator.utils.Dictionaries.SAMPLE_SCHEMA_NAME;
import static org.icgc.dcc.submission.generator.utils.Dictionaries.SPECIMEN_SCHEMA_NAME;
import static org.icgc.dcc.submission.generator.utils.Files.getLineCount;

import java.io.File;
import java.util.List;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.generator.core.DataGenerator;
import org.icgc.dcc.submission.generator.core.DonorFileGenerator;
import org.icgc.dcc.submission.generator.core.MetaFileGenerator;
import org.icgc.dcc.submission.generator.core.OptionalFileGenerator;
import org.icgc.dcc.submission.generator.core.PrimaryFileGenerator;
import org.icgc.dcc.submission.generator.core.SampleFileGenerator;
import org.icgc.dcc.submission.generator.core.SecondaryFileGenerator;
import org.icgc.dcc.submission.generator.core.SpecimenFileGenerator;
import org.icgc.dcc.submission.generator.model.ExperimentalFile;
import org.icgc.dcc.submission.generator.model.OptionalFile;
import org.icgc.dcc.submission.generator.model.Project;
import org.icgc.dcc.submission.generator.utils.CodeLists;
import org.icgc.dcc.submission.generator.utils.FileSchemas;

import lombok.NonNull;
import lombok.Setter;
import lombok.val;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Accessors
public class GeneratorService {

  /**
   * I/O specification.
   */
  String outputDirectory = "target/";
  Integer stringSize = 10;

  /**
   * Donor specification.
   */
  Integer numberOfDonors = 500;
  Integer numberOfSpecimensPerDonor = 2;
  Integer numberOfSamplesPerSpecimen = 2;

  /**
   * Project information.
   */
  Project project;

  /**
   * Random seed for stable generator output.
   */
  Long seed;

  /**
   * File output specifications.
   */
  List<OptionalFile> optionalFiles = newArrayList();
  List<ExperimentalFile> experimentalFiles = newArrayList();

  /**
   * File metadata.
   */
  @NonNull
  final FileSchemas fileSchema;
  @NonNull
  final CodeLists codeList;

  /**
   * Source of generated data.
   */
  @NonNull
  final DataGenerator generator;

  @Accessors(fluent = true)
  public static class Builder {

    @Setter
    private String outputDirectory = "target/";
    @Setter
    private Integer stringSize = 10;

    @Setter
    private Integer numberOfDonors = 500;
    @Setter
    private Integer numberOfSpecimensPerDonor = 3;
    @Setter
    private Integer numberOfSamplesPerSpecimen = 3;

    @Setter
    private String leadJurisdiction = "ca";
    @Setter
    private String tumourType = "01";
    @Setter
    private String institution = "001";
    @Setter
    private String platform = "1";

    @Setter
    private Long seed;

    @Setter
    private List<OptionalFile> optionalFiles = newArrayList();
    @Setter
    private List<ExperimentalFile> experimentalFiles = newArrayList();

    @Setter
    private FileSchemas fileSchemas;
    @Setter
    private CodeLists codeLists;

    public GeneratorService build() {
      // Prevent any delays by lazy loading
      fileSchemas = fileSchemas == null ? new FileSchemas() : fileSchemas;
      codeLists = codeLists == null ? new CodeLists() : codeLists;

      Project project =
          Project.builder()
              .leadJurisdiction(leadJurisdiction)
              .tumourType(tumourType)
              .institution(institution)
              .platform(platform)
              .build();

      DataGenerator generator = new DataGenerator(codeLists, stringSize, seed);

      return new GeneratorService(
          outputDirectory, stringSize,
          numberOfDonors, numberOfSpecimensPerDonor, numberOfSamplesPerSpecimen,
          project, seed, optionalFiles,
          experimentalFiles, fileSchemas,
          codeLists, generator);
    }

    /**
     * Adds an ExperimentalFile object to the list of experimental files
     */
    public Builder addExperimentalFile(ExperimentalFile experimentalFile) {
      this.experimentalFiles.add(experimentalFile);
      return this;
    }
  }

  private GeneratorService(
      String outputDirectory,
      int stringSize,
      int numberOfDonors,
      int numberOfSpecimensPerDonor,
      int numberOfSamplesPerSpecimen,
      Project project,
      long seed,
      List<OptionalFile> optionalFiles,
      List<ExperimentalFile> experimentalFiles,
      FileSchemas fileSchema,
      CodeLists codeList,
      DataGenerator generator) {
    this.outputDirectory = outputDirectory;
    this.stringSize = stringSize;
    this.numberOfDonors = numberOfDonors;
    this.numberOfSpecimensPerDonor = numberOfSpecimensPerDonor;
    this.numberOfSamplesPerSpecimen = numberOfSamplesPerSpecimen;
    this.project = project;
    this.seed = seed;
    this.optionalFiles = optionalFiles;
    this.experimentalFiles = experimentalFiles;
    this.fileSchema = fileSchema;
    this.codeList = codeList;
    this.generator = generator;
  }

  public static Builder generatorService() {
    return new Builder();
  }

  public void generateFiles() {
    log.info("Checking validity of parameters");
    checkParameters();

    //
    // File order matters:
    //

    // Clinical
    createDonorFile(numberOfDonors);
    createSpecimenFile(numberOfSpecimensPerDonor);
    createSampleFile(numberOfSamplesPerSpecimen);
    createOptionalFiles();

    // Experimental
    createExperimentalFiles();

    reportFiles();
  }

  private void checkParameters() {
    String leadJurisdiction = project.getLeadJurisdiction();
    String institution = project.getInstitution();
    String platform = project.getPlatform();
    String tumourType = project.getTumourType();

    checkArgument(new File(outputDirectory).exists(),
        "Invalid parameter {%s = %s}. The output directory specified does not exist", "output directory",
        outputDirectory);
    checkArgument(
        leadJurisdiction.length() == 2,
        "Invalid parameter {%s = %s}. The lead jurisdiction parameter can only be of length 2. Please refer to http://dcc.icgc.org/pages/docs/ICGC_Data_Submission_Manual-0.6c-150512.pdf, for valid lead jurisdction values",
        "lead jurisdiction", leadJurisdiction);
    checkArgument(
        (Integer.parseInt(tumourType) >= 1 && Integer.parseInt(tumourType) <= 31 && tumourType.length() >= 2),
        "Invalid parameter {%s = %s}. The tumour type parameter can only be a number between 1 and 31 (inclusive) and must be of length 2. Please refer to http://dcc.icgc.org/pages/docs/ICGC_Data_Submission_Manual-0.6c-150512.pdf, for valid lead jurisdction values",
        "tumour type", tumourType);
    checkArgument(
        (Integer.parseInt(institution) >= 1 && Integer.parseInt(institution) <= 98 && institution.length() == 3),
        "Invalid parameter {%s = %s}. The institution parameter must be a number between 1 and 98 (inclusive) and must of length 3. Please refer to http://dcc.icgc.org/pages/docs/ICGC_Data_Submission_Manual-0.6c-150512.pdf, for valid lead jurisdction values",
        "institution", leadJurisdiction);
    checkArgument(
        (Integer.parseInt(platform) >= 1 && Integer.parseInt(platform) <= 75),
        "Invalid parameter {%s = %s}. The platform parameter can only be of length 2. Please refer to http://dcc.icgc.org/pages/docs/ICGC_Data_Submission_Manual-0.6c-150512.pdf, for valid lead jurisdction values",
        "platform", platform);
    checkArgument(
        numberOfSamplesPerSpecimen % 2 == 0,
        "Invalid parameter {%s = %s}. The numberOfSamplesPerSpecimen parameter must be divisible by 2. This simplification is so that a tumor and control sample is always present for downstream feature types that require both (e.g. ssm)",
        "numberOfSamplesPerSpecimen", numberOfSamplesPerSpecimen);
  }

  private void reportFiles() {
    List<File> files = newArrayList(new File(outputDirectory).listFiles());
    sort(files);

    log.info("Finished generating {} files to {}:", files.size(), outputDirectory);
    long totalSize = 0;
    for (val file : files) {
      log.info("   {}: {} ({} lines)",
          new Object[] { file.getName(), byteCountToDisplaySize(file.length()), getLineCount(file) });

      totalSize += file.length();
    }

    log.info("{}", repeat("-", 80));
    log.info("Total size: {} ({} bytes)", byteCountToDisplaySize(totalSize), totalSize);
  }

  private void createDonorFile(Integer numberOfDonors) {
    String schemaName = DONOR_SCHEMA_NAME;
    log.info("Creating {} file", schemaName);

    val schema = getFileSchema(schemaName);
    generator.addUniqueKeys(schema);

    val fileGenerator = new DonorFileGenerator(schema, generator);
    fileGenerator.createFile(outputDirectory, schema, numberOfDonors, project);
  }

  private void createSpecimenFile(Integer linesPerForeignKey) {
    String schemaName = SPECIMEN_SCHEMA_NAME;
    log.info("Creating {} file", schemaName);

    val schema = getFileSchema(schemaName);
    generator.addUniqueKeys(schema);

    val fileGenerator = new SpecimenFileGenerator(schema, generator);
    fileGenerator.createFile(outputDirectory, schema, linesPerForeignKey, project);
  }

  private void createSampleFile(Integer linesPerForeignKey) {
    String schemaName = SAMPLE_SCHEMA_NAME;
    log.info("Creating {} file", schemaName);

    val schema = getFileSchema(schemaName);
    generator.addUniqueKeys(schema);

    val fileGenerator = new SampleFileGenerator(schema, generator);
    fileGenerator.createFile(outputDirectory, schema, linesPerForeignKey, project);
  }

  private void createOptionalFiles() {
    for (val optionalFile : optionalFiles) {
      String schemaName = optionalFile.getName();
      Integer numberOfLinesPerDonor = optionalFile.getNumberOfLinesPerDonor();

      generator.addUniqueKeys(getFileSchema(schemaName));
      createOptionalFile(schemaName, numberOfLinesPerDonor);
    }
  }

  private void createOptionalFile(String schemaName, Integer numberOfLinesPerDonor) {
    log.info("Creating {} file", schemaName);
    val schema = getFileSchema(schemaName);

    val fileGenerator = new OptionalFileGenerator(schema, generator);
    fileGenerator.createFile(outputDirectory, schema, numberOfLinesPerDonor, project);
  }

  private void createExperimentalFiles() {
    for (val experimentalFile : experimentalFiles) {
      String fileType = experimentalFile.getFileType().getAbbrev();
      String schemaName = experimentalFile.getSchemaName();
      Integer numberOfLines = experimentalFile.getNumberOfLinesPerForeignKey();

      val schema = getFileSchema(schemaName);
      if (schema == null) {
        log.warn("File schema '{}' is missing. Skipping...", schemaName);
        continue;
      }

      generator.addUniqueKeys(schema);

      if (fileType.equals(META_SUBTYPE.getAbbreviation())) {
        createMetaFile(schemaName, numberOfLines);
      } else if (fileType.equals(PRIMARY_SUBTYPE.getAbbreviation())) {
        createPrimaryFile(schemaName, numberOfLines);
      } else if (fileType.equals(SECONDARY_SUBTYPE.getAbbreviation())) {
        createSecondaryFile(schemaName, numberOfLines);
      } else {
        throw new GeneratorException("Invalid file type: %s", fileType);
      }
    }
  }

  private void createMetaFile(String schemaName, Integer linesPerForeignKey) {
    log.info("Creating {} file", schemaName);
    val schema = getFileSchema(schemaName);

    val fileGenerator = new MetaFileGenerator(schema, generator);
    fileGenerator.createFile(outputDirectory, schema, linesPerForeignKey, project);
  }

  private void createPrimaryFile(String schemaName, Integer linesPerForeignKey) {
    log.info("Creating {} file", schemaName);
    val schema = getFileSchema(schemaName);

    val fileGenerator = new PrimaryFileGenerator(schema, generator);
    fileGenerator.createFile(outputDirectory, schema, linesPerForeignKey, project);
  }

  private void createSecondaryFile(String schemaName, Integer linesPerForeignKey) {
    log.info("Creating {} file", schemaName);
    val schema = getFileSchema(schemaName);

    val fileGenerator = new SecondaryFileGenerator(schema, generator);
    fileGenerator.createFile(outputDirectory, schema, linesPerForeignKey, project);
  }

  private FileSchema getFileSchema(String schemaName) {
    if (schemaName.equals("ssm_s")) {
      // Special case now that this has been removed from the dictionary
      return null;
    }

    return checkNotNull(fileSchema.getSchema(schemaName),
        "Could not find matching file schema for name %s", schemaName);
  }

}
