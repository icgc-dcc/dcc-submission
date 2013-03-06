package org.icgc.dcc.generator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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
  /**
   * 
   */
  public ObjectMapper mapper;

  /**
   * 
   */
  public static List<CodeList> codeList;

  /**
   * 
   */
  public static String DONOR_SCHEMA;

  /**
   * 
   */
  public static String tab;

  /**
   * 
   */
  public static String newLine;

  /**
   * 
   */
  public Random random;

  /**
   * 
   */
  public ArrayList<String> donorID;

  /**
   * 
   */
  public ArrayList<String> specimenID;

  /**
   * 
   */
  public ArrayList<String> sampleID;

  /**
   * 
   */
  public ArrayList<String> controlOne;

  /**
   * 
   */
  public ArrayList<String> controlTwo;

  /**
   * 
   */
  public ArrayList<String> uniqueString;

  /**
   * 
   */
  public Integer uniqueInt;

  /**
   * 
   */
  public Double uniqueDecimal;

  /**
   * 
   */
  public ArrayList<ArrayList<String>> uniqueID;

  /**
   * 
   */
  public static List<String> featureType;

  /**
   * 
   */
  public static List<String> leadJurisdiction;

  /**
   * 
   */
  public static List<String> tumourType;

  /**
   * 
   */
  public static List<String> institution;

  /**
   * 
   */
  public static List<String> platform;

  // private final org.icgc.dcc.dictionary.model.CodeList codeList;

  private static org.icgc.dcc.dictionary.model.Dictionary dictionary;

  public DataGenerator(Long seed) throws JsonParseException, JsonMappingException, IOException {
    mapper = new ObjectMapper();
    dictionary =
        mapper.readValue(Resources.getResource("dictionary.json"), org.icgc.dcc.dictionary.model.Dictionary.class);

    codeList = mapper.readValue(Resources.getResource("codeList.json"), new TypeReference<List<CodeList>>() {
    });
    random = new Random(seed);

    uniqueID = new ArrayList<ArrayList<String>>();

    uniqueString = new ArrayList<String>();
    uniqueInt = 0;
    uniqueDecimal = 0.0;

    donorID = new ArrayList<String>();
    donorID.add("donor");
    donorID.add("donor_id");

    specimenID = new ArrayList<String>();
    specimenID.add("specimen");
    specimenID.add("specimen_id");

    sampleID = new ArrayList<String>();
    sampleID.add("sample");
    sampleID.add("analyzed_sample_id");

    // ArrayList to hold whether a sample is tumour or controlled
    controlOne = new ArrayList<String>();
    controlOne.add("sample");
    controlOne.add("controlVariable");

    controlTwo = new ArrayList<String>();
    controlTwo.add("sample");
    controlTwo.add("controlVariable");

    uniqueID.add(donorID);
    uniqueID.add(specimenID);
    uniqueID.add(sampleID);

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

  public void createFile(FileSchema schema, String[] args, String featureType) throws IOException {
    boolean isCore = featureType.equals("core") ? true : false;
    String fileName = generateFileName(schema, args, featureType, isCore);
    File outputFile = new File("target/" + fileName);
    outputFile.createNewFile();
    Writer writer = new BufferedWriter(new FileWriter(outputFile));

    for(String fieldName : schema.getFieldNames()) {
      writer.write(fieldName + tab);
    }

    writer.write(newLine);

    if(isCore) {
      createCoreFile(schema, args, writer);
    } else if(featureType.substring(featureType.length() - 2).equals("m")) {
      createMetaFile(schema, args, writer);
    } else if(featureType.substring(featureType.length() - 2).equals("p")) {
      createPrimaryFile(schema, args, writer);
    } else if(featureType.substring(featureType.length() - 2).equals("g")) {
      createPrimaryFile(schema, args, writer);
    } else if(featureType.substring(featureType.length() - 2).equals("s")) {
      if(schema.getName().equals("mirna_s")) {
        createMirnaFile(schema, args, writer);
      } else {
        createSecondaryFile(schema, args, writer);
      }
    }

    writer.close();
    // Uniqueness is to each file
    uniqueString.removeAll(uniqueString);
    uniqueInt = 0;
    uniqueDecimal = 0.0;

  }

  public void createCoreFile(FileSchema schema, String[] args, Writer writer) throws IOException {

    int numberOfLines;
    // check bidirectionality
    if(schema.getName().equals(DONOR_SCHEMA)) {
      numberOfLines = 1;
    } else if(schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) {
      numberOfLines = randomIntGenerator(1, 5);// Get 5 from arguments
    } else {
      numberOfLines = randomIntGenerator(0, 4);// Get 4 from arguments
    }

    int numberOfIterations =
        schema.getName().equals(DONOR_SCHEMA) ? numberOfIterations = Integer.parseInt(args[4]) : getForeignKey(schema,
            schema.getRelations().get(0).getFields().get(0)).size() - 2;

    for(int i = 0; i < numberOfIterations; i++) {
      for(int j = 0; j < numberOfLines; j++) {
        for(Field currentField : schema.getFields()) {
          String output = null;
          ArrayList<String> foreignKeyArray = getForeignKey(schema, currentField.getName());
          if(foreignKeyArray != null) {
            output = foreignKeyArray.get(i + 2);
          } else {
            output = getFieldValue(schema, currentField);
          }

          if(isUniqueField(schema.getUniqueFields(), currentField.getName())) {
            getPrimaryKey(schema, currentField.getName()).add(output);
          }

          // Special case for sample, to add whether sample type is controlled or tumour
          if(schema.getName().equals("sample") && currentField.getName().equals("analyzed_sample_type")) {
            int x = randomIntGenerator(0, 1);
            // Instead here you could check if output(which will be the value of analyzed_sample_type) = 'c' then
            // control one or, if output = 't' then go to control two
            if(x == 0) {
              controlOne.add(getPrimaryKey(schema, "analyzed_sample_id").get(
                  getPrimaryKey(schema, "analyzed_sample_id").size() - 1));
            } else {
              controlTwo.add(getPrimaryKey(schema, "analyzed_sample_id").get(
                  getPrimaryKey(schema, "analyzed_sample_id").size() - 1));
            }
          }
          writer.write(output + tab);
        }
        writer.write(newLine);
      }
      if(schema.getName().equals(DONOR_SCHEMA)) {
        numberOfLines = 1;
      } else if(schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) {
        numberOfLines = randomIntGenerator(1, 5);// Get 5 from arguments
      } else {
        numberOfLines = randomIntGenerator(0, 4);// Get 4 from arguments
      }
    }
  }

  public void createMetaFile(FileSchema schema, String[] args, Writer writer) throws IOException {
    // check bidirectionality
    int numberOfLines =
        (schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) ? randomIntGenerator(1, 5) : randomIntGenerator(
            0, 4);

    int numberOfIterations = getForeignKey(schema, schema.getRelations().get(0).getFields().get(0)).size() - 2;

    for(int i = 0; i < numberOfIterations; i++) {
      for(int j = 0; j < numberOfLines; j++) {
        for(Field currentField : schema.getFields()) {
          String output = null;

          if(getForeignKey(schema, currentField.getName()) != null) {
            if(!schema.getName().equals("exp_m") && !schema.getName().equals("jcn_m")
                && !schema.getName().equals("mirna_m")) {
              // Assuming that the relationship is bidirectional false in all m files,hence the generator
              output =
                  currentField.getName().equals("matched_sample_id") ? controlOne.get(randomIntGenerator(0,
                      controlOne.size() - 1)) : controlTwo.get(randomIntGenerator(0, controlTwo.size() - 1));
            } else {
              output = getForeignKey(schema, currentField.getName()).get(i + 2);
            }
          } else {
            output = getFieldValue(schema, currentField);
          }

          if(isUniqueField(schema.getUniqueFields(), currentField.getName())) {
            for(ArrayList<String> primaryKey : uniqueID) {
              if(primaryKey.get(0).equals(schema.getName()) && primaryKey.get(1).equals(currentField.getName())) {
                primaryKey.add(output);
              }
            }
          }

          writer.write(output + tab);
        }
        writer.write(newLine);
      }
      numberOfLines =
          (schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) ? randomIntGenerator(1,
              5) : randomIntGenerator(0, 4);
    }
  }

  public void createPrimaryFile(FileSchema schema, String[] args, Writer writer) throws IOException {
    int numberOfLines =
        (schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) ? randomIntGenerator(1, 5) : randomIntGenerator(
            0, 4);

    int numberOfIterations = getForeignKey(schema, schema.getRelations().get(0).getFields().get(0)).size() - 2;

    for(int i = 0; i < numberOfIterations; i++) {
      for(int j = 0; j < numberOfLines; j++) {
        for(Field currentField : schema.getFields()) {
          String output = null;

          ArrayList<String> foreignKeyArray = getForeignKey(schema, currentField.getName());

          output = foreignKeyArray != null ? foreignKeyArray.get(i + 2) : getFieldValue(schema, currentField);

          if(isUniqueField(schema.getUniqueFields(), currentField.getName())) {
            getPrimaryKey(schema, currentField.getName()).add(output);
            if(currentField.getName().equals("placement")) {
              System.out.println(output);
            }
          }

          writer.write(output + tab);
        }
        writer.write(newLine);
      }
      numberOfLines =
          (schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) ? randomIntGenerator(1,
              5) : randomIntGenerator(0, 4);
    }
  }

  public void createMirnaFile(FileSchema schema, String[] args, Writer writer) throws IOException {
    FileInputStream fis = null;
    BufferedReader br = null;

    fis = new FileInputStream("src/main/resources/mirna_mirbase.txt");
    br = new BufferedReader(new InputStreamReader(fis));
    int fieldIndexOne = getMirbaseIdIndex(schema, br);
    fis.getChannel().position(0);
    br = new BufferedReader(new InputStreamReader(fis));
    int fieldIndexTwo = getMirnaSequenceIndex(schema, br);

    int numberOfLines =
        (schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) ? randomIntGenerator(1, 5) : randomIntGenerator(
            0, 4);

    // 18000 is a random number i just picked cause mirna has no relation to other files
    for(int i = 0; i < 18000; i++) {
      for(int j = 0; j < numberOfLines; j++) {

        for(Field currentField : schema.getFields()) {
          String output = null;

          // Add system file fields
          String line = br.readLine();

          if(line != null) {
            output = getSystemFileOutput(fieldIndexOne, fieldIndexTwo, currentField, output, line.split(tab));
          } else {
            fis.getChannel().position(0);
            br = new BufferedReader(new InputStreamReader(fis));
            output = getSystemFileOutput(fieldIndexOne, fieldIndexTwo, currentField, output, br.readLine().split(tab));
          }

          if(output == null) {
            ArrayList<String> foreignKeyArray = getForeignKey(schema, currentField.getName());
            output = foreignKeyArray != null ? foreignKeyArray.get(i + 2) : getFieldValue(schema, currentField);
          }

          writer.write(output + tab);
        }
        writer.write(newLine);
      }
      numberOfLines =
          (schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) ? randomIntGenerator(1,
              5) : randomIntGenerator(0, 4);
    }
    br.close();
  }

  public void createSecondaryFile(FileSchema schema, String[] args, Writer writer) throws IOException {
    FileInputStream fis = null;
    BufferedReader br = null;
    fis = new FileInputStream("src/main/resources/hsapiens_gene_ensembl__transcript__main.txt");
    br = new BufferedReader(new InputStreamReader(fis));
    int fieldIndexOne = getGeneIndex(schema, br);
    fis.getChannel().position(0);
    br = new BufferedReader(new InputStreamReader(fis));
    int fieldIndexTwo = getTranscriptIndex(schema, br);

    int numberOfLines =
        (schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) ? randomIntGenerator(1, 5) : randomIntGenerator(
            0, 4);

    int numberOfIterations = getForeignKey(schema, schema.getRelations().get(0).getFields().get(0)).size() - 2;

    for(int i = 0; i < numberOfIterations; i++) {
      for(int j = 0; j < numberOfLines; j++) {

        for(Field currentField : schema.getFields()) {
          String output = null;

          // Add system file fields
          String line = br.readLine();

          if(line != null) {
            output = getSystemFileOutput(fieldIndexOne, fieldIndexTwo, currentField, output, line.split(tab));
          } else {
            fis.getChannel().position(0);
            br = new BufferedReader(new InputStreamReader(fis));
            output = getSystemFileOutput(fieldIndexOne, fieldIndexTwo, currentField, output, br.readLine().split(tab));
          }
          if(output == null) {
            ArrayList<String> foreignKeyArray = getForeignKey(schema, currentField.getName());
            output = foreignKeyArray != null ? foreignKeyArray.get(i + 2) : getFieldValue(schema, currentField);
          }

          writer.write(output + tab);
        }
        writer.write(newLine);
      }
      numberOfLines =
          (schema.getRelations().size() > 0 && schema.getRelations().get(0).isBidirectional()) ? randomIntGenerator(1,
              5) : randomIntGenerator(0, 4);
    }
    br.close();

  }

  /*
   * We can make this static
   */
  public String getSystemFileOutput(int fieldIndexOne, int fieldIndexTwo, Field currentField, String output,
      String[] fields) {
    if(currentField.getName().equals("xref_mirbase_id")) {
      output = fields[fieldIndexOne];
    } else if(currentField.getName().equals("mirna_seq")) {
      output = fields[fieldIndexTwo];
    } else if(currentField.getName().equals("gene_affected")) {
      output = fields[fieldIndexOne];
    } else if(currentField.getName().equals("transcript_affected")) {
      output = fields[fieldIndexTwo];
    }
    return output;
  }

  /*
   * We can make this static
   */
  public static int getGeneIndex(FileSchema schema, BufferedReader bf) throws IOException {
    int indexOfGeneID;
    String[] fields = bf.readLine().split(tab);

    for(indexOfGeneID = 0; indexOfGeneID < fields.length; indexOfGeneID++) {
      if(fields[indexOfGeneID].equals("gene_id_1020_key")) {
        return indexOfGeneID;
      }
    }

    return -1;
  }

  /*
   * We can make this static
   */
  public static int getTranscriptIndex(FileSchema schema, BufferedReader br) throws IOException {
    int indexOfTranscriptID;
    String[] fields = br.readLine().split(tab);

    for(indexOfTranscriptID = 0; indexOfTranscriptID < fields.length; indexOfTranscriptID++) {
      if(fields[indexOfTranscriptID].equals("transcript_id_1064_key")) {
        return indexOfTranscriptID;
      }
    }

    return -1;
  }

  /*
   * We can make this static
   */
  public static int getMirbaseIdIndex(FileSchema schema, BufferedReader br) throws IOException {
    int indexOfMirbaseIdID;
    String[] fields = br.readLine().split(tab);

    for(indexOfMirbaseIdID = 0; indexOfMirbaseIdID < fields.length; indexOfMirbaseIdID++) {
      if(fields[indexOfMirbaseIdID].equals("xref_mirbase_id")) {
        return indexOfMirbaseIdID;
      }
    }

    return -1;
  }

  /*
   * We can make this static
   */
  public static int getMirnaSequenceIndex(FileSchema schema, BufferedReader br) throws IOException {
    int indexOfMirnaSequence;
    String[] fields = br.readLine().split(tab);

    for(indexOfMirnaSequence = 0; indexOfMirnaSequence < fields.length; indexOfMirnaSequence++) {
      if(fields[indexOfMirnaSequence].equals("mirna_seq")) {
        return indexOfMirnaSequence;
      }
    }

    return -1;

  }

  public ArrayList<String> getPrimaryKey(FileSchema schema, String currentField) {
    for(ArrayList<String> primaryKeyArray : uniqueID) {
      if(primaryKeyArray.get(0).equals(schema.getName()) && primaryKeyArray.get(1).equals(currentField)) {
        return primaryKeyArray;
      }
    }
    return null;
  }

  public ArrayList<String> getForeignKey(FileSchema schema, String currentField) {

    for(int j = 0; j < schema.getRelations().size(); j++) {
      int k = 0; // holds index of 'fields' array that matches current field

      // Above Comment. Iterates through fields
      for(String foreignKeyField : schema.getRelations().get(j).getFields()) {
        if(currentField.equals(foreignKeyField)) {

          // Find arraylist that carries primary keys of schema that relates to this fileschema
          for(ArrayList<String> primaryKeyArray : uniqueID) {
            if(primaryKeyArray.get(0).equals(schema.getRelations().get(j).getOther())
                && primaryKeyArray.get(1).equals(schema.getRelations().get(j).getOtherFields().get(k))) {
              return primaryKeyArray;
            }
          }
        }
        k++;
      }
    }
    return null;
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
    StringBuffer filename = null;
    if(core) {
      filename =
          new StringBuffer(args[0] + "__" + args[1] + "__" + args[2] + "__" + schema.getName() + "__" + dateStamp
              + ".txt");
    } else {
      filename =
          new StringBuffer(featureType + "__" + args[0] + "__" + args[1] + "__" + args[2] + "__"
              + schema.getName().substring(schema.getName().length() - 1) + "__" + args[3] + "__" + dateStamp + ".txt");
    }
    return filename.toString();
  }

  public void determineUniqueFields(FileSchema schema) {
    for(String uniqueField : schema.getUniqueFields()) {
      ArrayList<String> uniqueFieldArray = new ArrayList<String>();
      System.out.println(schema.getName());
      System.out.println(uniqueField);
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
        output = codeListData(field, output);
        if(output == null || output.trim().length() == 0) {
          output = randomStringGenerator(10);
        }
      }
    } else if(field.getValueType() == ValueType.INTEGER) {
      if(isUniqueField(schema.getUniqueFields(), field.getName())) {
        output = Integer.toString(uniqueInt++);
      } else {
        output = codeListData(field, output);
        if(output == null || output.trim().length() == 0) {
          output = Integer.toString(randomIntGenerator(0, 200));
        }
      }
    } else if(field.getValueType() == ValueType.DECIMAL) {
      if(isUniqueField(schema.getUniqueFields(), field.getName())) {
        output = Double.toString(uniqueDecimal + 0.1);
      } else {
        output = codeListData(field, output);
        if(output == null || output.trim().length() == 0) {
          output = Double.toString(randomDecimalGenerator(50));
        }
      }
    } else if(field.getValueType() == ValueType.DATETIME) {
      return Calendar.DAY_OF_MONTH + "/" + Calendar.MONTH + "/" + Calendar.YEAR;
    }
    return output;
  }

  private String codeListData(Field field, String output) {
    if(field.getRestriction("codelist").isPresent()) {
      String codeListName = field.getRestriction("codelist").get().getConfig().getString("name");

      for(CodeList codelist : codeList) {
        if(codelist.getName().equals(codeListName)) {
          output = codelist.getTerms().get(randomIntGenerator(0, codelist.getTerms().size() - 1)).getCode();
        }
      }
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
      char c = (char) (random.nextDouble() * (123 - 97) + 97);
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
    DataGenerator test = new DataGenerator(Long.parseLong(args[6]));

    List<FileSchema> files = dictionary.getFiles();

    test.determineUniqueFields(test.getSchema("donor", files));
    test.createFile(test.getSchema("donor", files), args, "core");

    test.determineUniqueFields(test.getSchema("specimen", files));
    test.createFile(test.getSchema("specimen", files), args, "core");

    test.determineUniqueFields(test.getSchema("sample", files));
    test.createFile(test.getSchema("sample", files), args, "core");

    test.determineUniqueFields(test.getSchema(args[5] + "_m", files));
    test.createFile(test.getSchema(args[5] + "_m", files), args, "ssm");

    test.determineUniqueFields(test.getSchema(args[5] + "_p", files));
    test.createFile(test.getSchema(args[5] + "_p", files), args, "ssm");

    test.determineUniqueFields(test.getSchema(args[5] + "_s", files));
    test.createFile(test.getSchema(args[5] + "_s", files), args, "ssm");
  }
}