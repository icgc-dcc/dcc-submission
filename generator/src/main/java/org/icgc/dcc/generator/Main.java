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
package org.icgc.dcc.generator;

import static java.lang.System.err;
import static java.lang.System.out;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.icgc.dcc.model.ExperimentalFile;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Command line utility used to generate ICGC data sets.s In DataGenerator there's a a static
 * ArrayList<ArrayList<String>> called listOfPrimaryKeys. This ArrayList holds ArrayLists of primary keys for each file.
 * To identify which File and Field an ArrayList (with in the ArrayList) is associated with, the first element holds the
 * name of the associated FileSchema and the second element holds the name of the Field.
 */

public class Main {

  private final Options options = new Options();

  public static void main(String... args) throws JsonParseException, JsonMappingException, IOException {
    new Main().run(args);
  }

  private void run(String... args) throws JsonParseException, JsonMappingException, IOException {
    JCommander cli = new JCommander(options);
    cli.setProgramName(getProgramName());

    try {
      cli.parse(args);

      if(options.help) {
        cli.usage();

        return;
      } else if(options.version) {
        out.printf("ICGC DCC Data Generator%nVersion %s%n", getVersion());

        return;
      }

      generate(args);
    } catch(ParameterException pe) {
      err.printf("dcc-generator: %s%n", pe.getMessage());
      err.printf("Try '%s --help' for more information.%n", getProgramName());
    }
  }

  public void generate(String[] args) throws JsonParseException, JsonMappingException, IOException {
    String pathToConfigFile = args[0];

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    GeneratorConfig config = mapper.readValue(new File(pathToConfigFile), GeneratorConfig.class);

    String outputDirectory = config.getOutputDirectory();
    Integer numberOfDonors = config.getNumberOfDonors();
    Integer numberOfSpecimensPerDonor = config.getNumberOfSpecimensPerDonor();
    Integer numberOfSamplesPerDonor = config.getNumberOfSamplesPerSpecimen();
    String leadJurisdiction = config.getLeadJurisdiction();
    Long tumourType = config.getTumourType();
    Long institution = config.getInstitution();
    Long platform = config.getPlatform();
    Long seed = config.getSeed();

    // ArrayList<OptionalFile> optionalFiles = config.getOptionalFiles();

    List<ExperimentalFile> experimentalFiles = config.getExperimentalFiles();

    DataGenerator test = new DataGenerator(outputDirectory, seed);
    test.createCoreFile("donor", numberOfDonors, leadJurisdiction, institution, tumourType, platform);
    test.createCoreFile("specimen", numberOfSamplesPerDonor, leadJurisdiction, institution, tumourType, platform);
    test.createCoreFile("sample", numberOfSpecimensPerDonor, leadJurisdiction, institution, tumourType, platform);

    // Create loop to create optionalFiles here

    for(ExperimentalFile experimentalFile : experimentalFiles) {
      String fileType = experimentalFile.getFileType();
      String schemaName = experimentalFile.getName() + "_" + fileType;
      Integer numberOfLines = experimentalFile.getNumberOfLinesPerForeignKey();

      test.determineUniqueFields(DataGenerator.getSchema(schemaName));
      if(fileType.equals("m")) {
        test.createMetaFile(schemaName, numberOfLines, leadJurisdiction, institution, tumourType, platform);
      } else if(fileType.equals("p")) {
        test.createPrimaryFile(schemaName, numberOfLines, leadJurisdiction, institution, tumourType, platform);
      } else if(fileType.equals("g")) {
        test.createPrimaryFile(schemaName, numberOfLines, leadJurisdiction, institution, tumourType, platform);
      } else if(fileType.equals("s")) {
        test.createSecondaryFile(schemaName, numberOfLines, leadJurisdiction, institution, tumourType, platform);
      }
    }
    /*
     * Here make variables of all the config fields, and make an arraylist of ExperimentalFile objects and an arraylist
     * of OptionalFile objects Make a method in DataGenerator for createCoreFile and createExpFile and createTempFile
     * call the above three methods here using loops With in the createCoreFile and createExpFile and createTempFile,
     * instantiate their respective objects (passing in UniqueID), and call the openFile method which will
     * generateFileName open a stream and call the populateCoreFile, populateExpFile, populateTempFile Move on from
     * there
     */
  }

  private String getProgramName() {
    return "java -jar " + getJarName();
  }

  private String getVersion() {
    String version = getClass().getPackage().getImplementationVersion();

    return version == null ? "" : version;
  }

  private String getJarName() {
    String jarPath = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
    File jarFile = new File(jarPath);

    return jarFile.getName();
  }

}
