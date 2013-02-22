package org.icgc.dcc.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.Relation;
import org.icgc.dcc.dictionary.model.ValueType;

import com.google.common.io.Resources;

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

/**
 * 
 */

public class DataGenerator {
  private final ObjectMapper mapper;

  private final List<CodeList> codeList;

  private final Random random;

  private ArrayList<String> uniqueString;

  private ArrayList<Integer> uniqueInt;

  private ArrayList<Double> uniqueDecimal;

  private final ArrayList<ArrayList<String>> uniqueID;

  private final List<String> featureType;

  private final List<String> leadJurisdiction;

  private final List<String> tumourType;

  private final List<String> institution;

  private final List<String> platform;

  private static org.icgc.dcc.dictionary.model.Dictionary dictionary;

  public DataGenerator() throws JsonParseException, JsonMappingException, IOException {
    mapper = new ObjectMapper();
    dictionary =
        mapper.readValue(Resources.getResource("dictionary.json"), org.icgc.dcc.dictionary.model.Dictionary.class);
    codeList = mapper.readValue(Resources.getResource("codeList.json"), new TypeReference<List<CodeList>>() {
    });
    random = new Random();
    uniqueID = new ArrayList<ArrayList<String>>();

    uniqueString = new ArrayList<String>();
    uniqueInt = new ArrayList<Integer>();
    uniqueDecimal = new ArrayList<Double>();

    /*
     * donorID = new ArrayList<String>(); donorID.add("donor"); donorID.add("donor_id"); specimenID = new
     * ArrayList<String>(); specimenID.add("specimen"); specimenID.add("specimen_id"); sampleID = new
     * ArrayList<String>(); sampleID.add("sample"); sampleID.add("analyzed_sample_id");
     */

    featureType = Arrays.asList("ssm", "sgv", "cnsm", "cngv", "stsm", "stgv", "exp", "mirna", "jcn", "meth");
    leadJurisdiction =
        Arrays.asList("Australia", "Canada", "China", "France", "Germany", "India", "Japan", "Spain", "UK", "USA");
    institution =
        Arrays
            .asList(
                "Advanced Centre for Treatment, Research and Education in Cancer (Mumbai)",
                "AMC Medical Research BV (Netherlands)",
                "Applied Biosystems Inc.",
                "Australian Pancreatic Cancer Network",
                "Barcelona Supercomputer Center (BSC-Barcelona)",
                "Baylor College of Medicine (Houston, TX)",
                "British Columbia Cancer Agency (Vancouver, Canada)",
                "Beijing Cancer Hospital/Insititute",
                "Beijing Genome Institute/Shenzhen",
                "Bioquant (Heidelberg)",
                "Broad Institute (Cambridge, MA)",
                "Catalan Institute of Oncology",
                "Center for Cancer Research (CICSalamanca) and University Hospital",
                "Center for Genomic Regulation (CRG) and Pompeu Fabra University (UPF)",
                "Centre Leon Berard (Lyon, France)",
                "Centre National de Génotypage (France)",
                "Centre Val d'Aurelle (Montpellier, France)",
                "Commissariat à l'Energie Atomique",
                "CRUK (UK)",
                "Dana-Farber Cancer Institute",
                "DFCI (USA)",
                "EMBL-EBI (Hinxton)",
                "Erasmus (Netherlands)",
                "European Molecular Biology Laboratory (EMBL), Heidelberg",
                "Fondation Jean Dausset CEPH",
                "Fondation Synergie-Lyon-Cancer",
                "Garvan Institute of Medical Research",
                "Data Submission Manual",
                "German Cancer Research Center (DKFZ), Heidelberg Harvard Medical School and Brigham and Women's Hospital (Cambridge, MA)",
                "Hiroshima University, Faculty of Medicine",
                "Hospital Clinic, University of Barcelona",
                "Hospital-University : AP-HP Paris (Beaujon, H. Mondor, A. Béclère and P. Brousse hospitals), Bordeaux, Rennes, Toulouse Grenoble",
                "HudsonAlpha Institute for Biotechnology (Huntsville, AL)",
                "Human Genome Center, Institute of Medical Science, University of Tokyo", "ICR (UK)", "INCa (France)",
                "Institut Curie (France)", "Institut Génomique",
                "Institut National de la Santé et de la Recherche Médicale",
                "Institut National du Cancer (Boulogne-Billancourt, France)",
                "Institut Paoli-Calmettes (Marseille, France)", "Institute for Molecular Bioscience (Brisbane)",
                "Institute for System Biology (Seattle, WA)", "International Breast Cancer Genome Consortium (UK)",
                "International Genome Consortium (Phoenix, AZ)", "Johns Hopkins University (Baltimore, MD)",
                "Lawrence Berkeley National Laboratory (Berkeley, CA)", "Lund University (Sweden)",
                "Massachusetts General Hospital", "Max-Planck-Institut for Molecular Genetics (Berlin)", "Mayo Clinic",
                "Memorial Sloan-Kettering Cancer Center (New York, NY)", "Mount Sinai Hospital (Toronto)",
                "National Bioinformatics Institute", "National Cancer Center",
                "National Center for tumour Diseases (Heidelberg)", "National DNA and tumour Bank Networks",
                "National Institute of Biomedical Genomics (Kalyani)",
                "National Institutes of Health; National Cancer Institute, National Human Genome Research Institute",
                "National Sequencing Center (Barcelona)", "NCI Bari (Italy)", "Norwegian Radium Hospital (Norway)",
                "Ontario Institute for Cancer Research", "Osaka Medical Center for Cancer & Cardiovascular Diseases",
                "Peking University School of Oncology", "Data Submission Manual", "Peter MacCallum Cancer Centre",
                "Queensland Centre for Medical Genomics", "Queensland Institute of Medical Research",
                "Radboud University (Netherlands)",
                "Research Center for Advanced Science and Technology, University of Tokyo", "RIKEN",
                "Silicon Graphics Inc.", "Singapore General Hospital (Hong Kong)", "Spanish Cancer Research Network",
                "Spanish National Cancer Research Centre (CNIO-Madrid)", "UCSF", "University Health Network (Toronto)",
                "University of California (Santa Cruz, CA)", "University of Cambridge (UK)", "University of Deusto",
                "University of Düsseldorf", "University of Heidelberg",
                "University of North Carolina (Chapel Hill, NC)", "University of Oviedo",
                "University of Queensland (Australia)", "University of Southern California (Los Angeles, CA)",
                "University of Tromsø (Norway)", "University of Verona", "Wakayama Medical University",
                "Wellcome Trust Sanger Institute", "Westmead Institute for Cancer Research",
                "Washington University Genome Sequencing Center (St. Louis, MO)", "The Cancer Genome Atlas",
                "Nationwide Children's Hospital", "MD Anderson");
    platform =
        Arrays.asList("PCR", "qPCR", "capillary sequencing", "SOLiD sequencing", "Illumina GA sequencing",
            "sequencing", "Helicos sequencing", "Affymetrix Genome-Wide Human SNP Array .",
            "Affymetrix Genome-Wide Human SNP Array .", "Affymetrix Mapping K Array Set",
            "Affymetrix Mapping K Array Set", "Affymetrix Mapping K . Array Set", "Affymetrix EMET Plus Premier Pack",
            "Agilent Whole Human Genome Oligo Microarray Kit", "Agilent Human Genome A", "Agilent Human Genome A",
            "Agilent Human CNV Association xK", "", "Agilent Human Genome K", "Agilent Human CGH xM",
            "Agilent Human CGH xK", "Agilent Human CGH xK", "Agilent Human CGH xK", "Agilent Human CNV xK",
            "Agilent Human miRNA Microarray Kit (v)", "Agilent Human CpG Island Microarray Kit",
            "Agilent Human Promoter ChIP-on-chip Microarray Set", "Agilent Human SpliceArray", "Illumina humanm-duo",
            "Illumina humanw-quad", "Illumina humancytosnp-", "Illumina humans-duo", "Illumina humanmethylation",
            "Illumina goldengate methylation", "Illumina HumanHT- v. beadchip", "Illumina HumanWG- v. beadchip",
            "Illumina HumanRef- v. beadchip", "Illumina microRNA Expression Profiling Panel", "Illumina humanht-",
            "Illumina humanht-", "Nimblegen Human CGH x Whole-Genome v. Array",
            "Nimblegen Human CGH .M Whole-Genome v.D Array", "Nimblegen Gene Expression K",
            "Nimblegen Gene Expression xK", "Nimblegen Gene Expression xK",
            "Nimblegen Human Methylation .M Whole-Genome sets", "Nimblegen Human Methylation K Whole-Genome sets",
            "Nimblegen CGS", "Illumina HumanM OmniQuad chip", "PCR and capillary sequencing",
            "Custom-designed gene expression array", "Affymetrix HT Human Genome UA Array Plate Set",
            "Agilent K Custom Gene Expression GA--", "Agilent K Custom Gene Expression GA--",
            "Agilent K Custom Gene Expression GA--", "Agilent Human Genome CGH Custom Microaary xK",
            "Affymetrix Human U Plus PM", "", "Affymetrix Human U Plus .", "Affymetrix Human Exon . ST",
            "Almac Human CRC", "Illumina HiSeq", "Affymetrix Human MIP K", "Affymetrix Human Gene . ST",
            "Illumina Human Omni-Quad beadchip", "Sequenom MassARRAY", "Custom-designed cDNA array",
            "Illumina HumanHap", "Ion Torrent PGM", "Illumina GoldenGate Methylation Cancer Panel I",
            "Illumina Infinium HumanMethylation", "Agilent  x K Human miRNA-specific microarray",
            "M.D. Anderson Reverse Phase Protein Array Core", "Microsatellite Instability Analysis",
            "Agilent K Custom Gene Expression GA-", "Illumina HumanCNV-Duo v. BeadChip",
            "Illumina HumanOmniExpress BeadChip");
    tumourType =
        Arrays.asList("Pancreatic Cancer", "Breast Cancer", "Brain cancer", "Colorectal cancer", "Ovarian cancer",
            "Gastric cancer", "Liver cancer", "Pediatric brain tumours", "Oral cancer", "Chronic lymphocytic leukemia",
            "Lung cancer", "Melanoma", "Kidney renal clear cell cancinoma", "Kidney renal papillary cell carcinoma",
            "Acute Myeloid Leukemia", "Head and Neck squamous cell carcinoma", "Lung adenocaracinoma",
            "Lung squamous cell carcinoma", "Rectum adenocarcinoma", "Stomach adenocarcinoma",
            "Uterine Corpus Endometrioid Carcinoma", "Colon adenocarcinoma", "Malignant Lymphoma", "Myelodysplasia",
            "Brain Lower Grade Glioma", "Cervical squamous cell carcinoma and endocervical adenocarcinoma",
            "Esophageal carcinoma", "Prostate adenocarcinoma", "Sarcoma", "Thyroid carcinoma",
            "Bladder Urothelial Carcinoma");

  }

  public void createCoreFile(FileSchema schema, String featureType, String[] args, boolean core) throws IOException {
    String fileName = generateFileName(schema, args, featureType, core);
    File outputFile = new File("target/" + fileName);
    outputFile.createNewFile();
    Writer writer = new BufferedWriter(new FileWriter(outputFile));

    for(String fieldName : schema.getFieldNames()) {
      writer.write(fieldName + '\t');
    }

    writer.write("\n");
    String output = null;

    if(schema.getName().equals("donor")) {
      for(int i = 0; i < Integer.parseInt(args[4]); i++) {
        for(Field currentField : schema.getFields()) {
          output = getFieldValue(schema, currentField);
          writer.write(output + "/t");
          for(String uniqueField: schema.getUniqueFields()){
            if(currentField.getName().equals(uniqueField)){
              for(ArrayList<String> primaryKeyArray: uniqueID){
                if(primaryKeyArray.get(0).equals("donor")&&primaryKeyArray.get(1).equals(currentField.getName())){
                  primaryKeyArray.add(output);
                }
              }
            }
          }
        }
        writer.write("/n");
        
        
      }
    } else if(schema.getName().equals("specimen")) {
      for(int i = 0; i < Integer.parseInt(args[5]); i++) {
        for(Field currentField : schema.getFields()) {
          output = null;
          for(Relation relation:schema.getRelations()){
            for(String foreignField:relation.getFields()){
              if(foreignField.equals(currentField.getName())){
                for(ArrayList<String> foreign)
              }
            }
          }
        }
      }
    }
  }

  public void createExpFile(FileSchema schema, String featureType, String[] args, boolean core) throws IOException {
    String fileName = null;
    if(core) {
      fileName = generateFileName(schema, args, featureType, core);
    } else {
      fileName = generateFileName(schema, args, featureType, core);
    }
    File outputFile = new File("target/" + fileName);
    outputFile.createNewFile();
    Writer writer = new BufferedWriter(new FileWriter(outputFile));

    for(String fieldName : schema.getFieldNames()) {
      writer.write(fieldName + '\t');
    }

    writer.write("\n");
    String output = null;
    Integer iterate = null;
    for(ArrayList<String> primaryKeyArray : uniqueID) {
      try {
        if(primaryKeyArray.get(0).equals(schema.getRelations().get(0).getOther())
            && primaryKeyArray.get(1).equals(schema.getRelations().get(0).getOtherFields().get(0))) {
          iterate = primaryKeyArray.size() - 2;
        }
      } catch(IndexOutOfBoundsException e) {

      }
    }

    if(iterate == null) {
      iterate = Integer.parseInt(args[4]);
    }
    System.out.println(iterate);
    for(int i = 0; i < iterate; i++) {
      // BiDirectionality check must be here somewhere. I don't think its logically possibly that one relationship be
      // bidirectional and one not be of the same schema
      int randomInt;
      if(schema.getName().equals("donor")) {
        randomInt = 1;
      } else {
        randomInt = randomIntGenerator(1, 5);
      }
      for(int x = 0; x < randomInt; x++) {

        for(Field currentField : schema.getFields()) {
          output = null;

          // See if the current field being populated is suppose to have a foreign key
          for(int j = 0; j < schema.getRelations().size(); j++) {
            int k = 0; // holds index of 'fields' array that matches current field

            // Above Comment. Iterates through fields
            for(String foreignKeyField : schema.getRelations().get(j).getFields()) {
              if(currentField.getName().equals(foreignKeyField)) {

                // Find arraylist that carries primary keys of schema that relates to this fileschema
                for(ArrayList<String> primaryKeyArray : uniqueID) {
                  if(primaryKeyArray.get(0).equals(schema.getRelations().get(j).getOther())
                      && primaryKeyArray.get(1).equals(schema.getRelations().get(j).getOtherFields().get(k))) {

                    try {
                      output = primaryKeyArray.get(i + 2);
                    } catch(IndexOutOfBoundsException e) {

                    }
                  }
                }
              }
              k++;
            }
          }

          if(output == null) {
            output = getFieldValue(schema, currentField);
          }

          if(isUniqueField(schema.getUniqueFields(), currentField.getName())) {
            for(ArrayList<String> primaryKey : uniqueID) {
              if(primaryKey.get(0).equals(schema.getName()) && primaryKey.get(1).equals(currentField.getName())) {
                primaryKey.add(output);
              }
            }
          }

          writer.write(output + "\t");
        }
        writer.write("\n");
      }
    }
    writer.close();

    // Uniqueness is to each file
    uniqueString = new ArrayList<String>();
    uniqueInt = new ArrayList<Integer>();
    uniqueDecimal = new ArrayList<Double>();
  }

  public String checkParameters(String[] args) {
    boolean found = false;
    for(String jurisdiction : leadJurisdiction) {
      for(String tumour : tumourType) {
        for(String institute : institution) {
          for(String pf : platform) {
            for(String fT : featureType) {
              for(int i = 5; i < args.length; i++) {
                if(args[i].equals(fT)) {
                  found = true;
                }
              }
            }
            if(!found) {
              return "Incorrect feature type";
            } else if(args[3].equals(pf)) {
              found = true;
            }
          }
          if(!found) {
            return "Incorrect platform";
          } else if(args[2].equals(institute)) {
            found = true;
          }
        }
        if(!found) {
          return "Incorrect institute";
        } else if(args[1].equals(tumour)) {
          found = true;
        }
      }
      if(!found) {
        return "Incorrect tumour type";
      } else if(args[0].equals(jurisdiction)) {
        found = true;
      }
    }
    if(!found) {
      return "Incorrect jurisdiction";
    } else
      return null;
  }

  public String generateFileName(FileSchema schema, String[] args, String featureType, boolean core) {
    String dateStamp = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
    if(core) {
      return args[0] + "__" + args[1] + "__" + args[2] + "__" + schema.getName() + "__" + dateStamp + ".txt";
    } else {
      return featureType + "__" + args[0] + "__" + args[1] + "__" + args[2] + "__"
          + schema.getName().substring(schema.getName().length() - 2, schema.getName().length()) + "__" + args[3]
          + "__" + dateStamp + ".txt";
    }
  }

  public void determineUniqueFields(FileSchema schema) {
    for(String uniqueField : schema.getUniqueFields()) {
      ArrayList<String> uniqueFieldArray = new ArrayList<String>();
      uniqueFieldArray.add(schema.getName());
      uniqueFieldArray.add(uniqueField);
      uniqueID.add(uniqueFieldArray);
    }
  }

  public String getFieldValue(FileSchema schema, Field field) {
    String output = "";
    if(field.getValueType() == ValueType.TEXT) {
      if(isUniqueField(schema.getUniqueFields(), field.getName())) {
        output = uniqueStringGenerator(10);
      } else {
        output = randomStringGenerator(10);
      }
    } else if(field.getValueType() == ValueType.INTEGER) {
      if(isUniqueField(schema.getUniqueFields(), field.getName())) {
        output = Integer.toString(uniqueIntGenerator(0, 200));
      } else {
        output = Integer.toString(randomIntGenerator(0, 200));
      }
    } else if(field.getValueType() == ValueType.DECIMAL) {
      if(isUniqueField(schema.getUniqueFields(), field.getName())) {
        output = Double.toString(uniqueDecimalGenerator(50));
      } else {
        output = Double.toString(randomDecimalGenerator(50));
      }
    } else if(field.getValueType() == ValueType.DATETIME) {
      output = Calendar.DAY_OF_MONTH + "/" + Calendar.MONTH + "/" + Calendar.YEAR;
    }
    return output;
  }

  public FileSchema getSchema(String schemaName, List<FileSchema> files) {
    for(FileSchema schema : files) {
      if(schema.getName().equals(schemaName)) {
        return schema;
      }
    }
    return null;
  }

  public boolean isUniqueField(List<String> uniqueField, String fieldName) {
    for(String field : uniqueField) {
      if(field.equals(fieldName)) {
        return true;
      }
    }
    return false;
  }

  public String uniqueStringGenerator(int length) {
    StringBuilder sb = new StringBuilder();
    for(int i = 0; i < length; i++) {
      char c = (char) ((Math.random() * (123 - 97)) + 97);
      sb.append(c);
    }
    for(String genString : uniqueString) {
      if(genString.equals(sb)) {
        return uniqueStringGenerator(length);
      }
    }
    uniqueString.add(sb.toString());
    return sb.toString();
  }

  public int uniqueIntGenerator(int start, int end) {
    int randomInt = random.nextInt(end + 1) + start;
    for(Integer genInt : uniqueInt) {
      if(genInt.equals(randomInt)) {
        return uniqueIntGenerator(start, end);
      }
    }
    uniqueInt.add(randomInt);
    return randomInt;

  }

  public double uniqueDecimalGenerator(double end) {
    double randomDecimal = random.nextDouble() * end;
    for(Double genDecimal : uniqueDecimal) {
      if(genDecimal.equals(randomDecimal)) {
        return uniqueDecimalGenerator(end);
      }
    }
    uniqueDecimal.add(randomDecimal);
    return randomDecimal;
  }

  public String randomStringGenerator(int length) {
    StringBuilder sb = new StringBuilder();
    for(int i = 0; i < length; i++) {
      char c = (char) ((Math.random() * (123 - 97)) + 97);
      sb.append(c);
    }
    return sb.toString();
  }

  public int randomIntGenerator(int start, int end) {
    return random.nextInt(end + 1) + start;
  }

  public double randomDecimalGenerator(double end) {
    return random.nextDouble() * end;
  }

  public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException,
      IllegalArgumentException {
    DataGenerator testDataGenerator = new DataGenerator();

    List<FileSchema> files = dictionary.getFiles();

    String error = testDataGenerator.checkParameters(args);
    if(error != null) {
      throw new IllegalArgumentException(error);
    }

    testDataGenerator.determineUniqueFields(testDataGenerator.getSchema("donor", files));
    testDataGenerator.createCoreFile(testDataGenerator.getSchema("donor", files), "", args, true);

    testDataGenerator.determineUniqueFields(testDataGenerator.getSchema("specimen", files));
    testDataGenerator.createCoreFile(testDataGenerator.getSchema("specimen", files), "", args, true);

    testDataGenerator.determineUniqueFields(testDataGenerator.getSchema("sample", files));
    testDataGenerator.createCoreFile(testDataGenerator.getSchema("sample", files), "", args, true);

    for(int i = 6; i < 6 + Integer.parseInt(args[5]); i++) {
      testDataGenerator.determineUniqueFields(testDataGenerator.getSchema(args[i], files));
      testDataGenerator.createExpFile(testDataGenerator.getSchema(args[i], files), "", args, true);
    }

    for(int i = 6 + Integer.parseInt(args[5]); i < args.length; i++) {
      testDataGenerator.determineUniqueFields(testDataGenerator.getSchema(args[i] + "_m", files));
      testDataGenerator.createExpFile(testDataGenerator.getSchema(args[i] + "_m", files), args[i], args, false);
      if((testDataGenerator.getSchema(args[i] + "_p", files)) != null) {
        testDataGenerator.determineUniqueFields(testDataGenerator.getSchema(args[i] + "_p", files));
        testDataGenerator.createExpFile(testDataGenerator.getSchema(args[i] + "_p", files), args[i], args, false);
      } else if((testDataGenerator.getSchema(args[i] + "_g", files)) != null) {
        testDataGenerator.determineUniqueFields(testDataGenerator.getSchema(args[i] + "_g", files));
        testDataGenerator.createExpFile(testDataGenerator.getSchema(args[i] + "_g", files), args[i], args, false);
      }
      if((testDataGenerator.getSchema(args[i] + "_s", files)) != null) {
        testDataGenerator.determineUniqueFields(testDataGenerator.getSchema(args[i] + "_s", files));
        testDataGenerator.createExpFile(testDataGenerator.getSchema(args[i] + "_s", files), args[i], args, false);
      }
    }
  }
}