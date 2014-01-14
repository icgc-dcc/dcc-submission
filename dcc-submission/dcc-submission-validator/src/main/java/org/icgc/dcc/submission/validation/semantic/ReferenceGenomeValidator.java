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
package org.icgc.dcc.submission.validation.semantic;

import static com.google.common.io.Files.getNameWithoutExtension;
import static com.google.common.primitives.Ints.tryParse;
import static java.lang.String.format;
import static org.codehaus.jackson.JsonGenerator.Feature.AUTO_CLOSE_TARGET;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.SSM_TYPE;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME_END;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME_START;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_MUTATION_TYPE;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE;
import static org.icgc.dcc.submission.core.parser.FileParsers.newMapFileParser;
import static org.icgc.dcc.submission.validation.core.ErrorType.REFERENCE_GENOME_INSERTION_ERROR;
import static org.icgc.dcc.submission.validation.core.ErrorType.REFERENCE_GENOME_MISMATCH_ERROR;
import static org.icgc.dcc.submission.validation.core.Validators.checkInterrupted;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import net.sf.picard.reference.IndexedFastaSequenceFile;

import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.icgc.dcc.submission.core.parser.FileParser;
import org.icgc.dcc.submission.core.parser.FileRecordProcessor;
import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;

import com.google.common.base.Optional;

/**
 * Support querying a reference genome data file in the form for chromosome-start-end to validate submission input.
 * <p>
 * This uses the picard utilities to query an indexed FASTA file, as a bench mark reference we can check roughly
 * 3,000,000 reference genomes in 200 seconds.
 * 
 * @see https://wiki.oicr.on.ca/display/DCCSOFT/Unify+genome+assembly+build+throughout+the+system
 * @see https://wiki.oicr.on.ca/display/DCCSOFT/SSM+data+model+supporting+controlled+fields+and+other+improvements#
 * SSMdatamodelsupportingcontrolledfieldsandotherimprovements-SSMvalidationinReferenceGenomesequenceValidationRGV
 */
@Slf4j
public class ReferenceGenomeValidator implements Validator {

  /**
   * The value of {@code ssm_p.0.mutation_type.v1} that corresponds to {@code insertion of <=200bp}.
   * 
   * @see http://legacy-portal.dcc.icgc.org/pages/docs/dictionaries/latest/#ssm_p.0.mutation_type.v1
   */
  private static final String INSERTION_MUTATION_TYPE = "2";

  /**
   * Value that is used to convey an insertion for the reference allele.
   */
  private static final String REFERENCE_INSERTION_VALUE = "-";

  /**
   * The reference assembly version that corresponds to the configured {@link #sequenceFile}.
   */
  @NonNull
  private final String assemblyVersion;

  /**
   * The FASTA file used for validation.
   */
  @NonNull
  private final IndexedFastaSequenceFile sequenceFile;

  /**
   * Creates a {@code ReferenceGenomeValidator} configured with the supplied {@code fastaFilePath}.
   * 
   * @param fastaFilePath the fully qualified path to the the {@code .fasta} file. Expected to be placed next to
   * {@code .fai} file with the same prefix.
   */
  @SneakyThrows
  public ReferenceGenomeValidator(@NonNull String fastaFilePath) {
    val fastaFile = new File(fastaFilePath).getAbsoluteFile();
    this.assemblyVersion = getAssemblyVersion(fastaFile);
    this.sequenceFile = new IndexedFastaSequenceFile(fastaFile);

    log.info("Using '{}' assembly versioned FASTA file: '{}'", assemblyVersion, fastaFile);
  }

  @Override
  public String getName() {
    return "Reference Genome Validator";
  }

  /**
   * Validate genome reference aligns with reference genome of submitted primary file. We assume at this stage the file
   * is well-formed, and that each individual field is sane.
   */
  @SneakyThrows
  @Override
  public void validate(ValidationContext context) {
    log.info("Starting...");

    // Selective validation filtering
    val requested = context.getDataTypes().contains(SSM_TYPE);
    if (!requested) {
      log.info("SSM validation not requested for '{}'. Skipping...", context.getProjectKey());

      return;
    }

    // This validation is only applicable if ssm_p is available
    Optional<Path> optionalSsmPrimaryFile = context.getSsmPrimaryFile();
    val skip = !optionalSsmPrimaryFile.isPresent();
    if (skip) {
      log.info("No ssm_p file for '{}'. Skipping...", context.getProjectKey());

      return;
    }

    // It exists
    val ssmPrimaryFile = optionalSsmPrimaryFile.get();

    val fileParser = newMapFileParser(context.getFileSystem(), context.getSsmPrimaryFileSchema());

    @Cleanup
    val outputStream = getOutputStream(context, ssmPrimaryFile);

    // Get to work
    log.info("Performing reference genome validation on file '{}' for '{}'", ssmPrimaryFile, context.getProjectKey());
    validate(context, ssmPrimaryFile, fileParser, outputStream);
    log.info("Finished performing reference genome validation for '{}'", context.getProjectKey());
  }

  public String getReferenceGenomeSequence(String chromosome, String start, String end) {
    val startPosition = Long.valueOf(start);
    val endPosition = Long.valueOf(end);

    return getReferenceGenomeSequence(chromosome, startPosition, endPosition);
  }

  public String getReferenceGenomeSequence(String chromosome, long start, long end) {
    val sequence = sequenceFile.getSubsequenceAt(chromosome, start, end);
    val text = new String(sequence.getBases());

    return text;
  }

  @SneakyThrows
  private void validate(final ValidationContext context, final Path filePath,
      final FileParser<Map<String, String>> fileParser, final OutputStream outputStream) {
    val fileName = filePath.getName();
    val writer = createReportWriter();

    fileParser.parse(filePath, new FileRecordProcessor<Map<String, String>>() {

      @Override
      public void process(long lineNumber, Map<String, String> record) throws Exception {
        val mutationType = record.get(SUBMISSION_OBSERVATION_MUTATION_TYPE);
        val chromosomeCode = record.get(SUBMISSION_OBSERVATION_CHROMOSOME);
        val start = record.get(SUBMISSION_OBSERVATION_CHROMOSOME_START);
        val end = record.get(SUBMISSION_OBSERVATION_CHROMOSOME_END);
        val referenceAllele = record.get(SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE);

        if (isInsertionType(mutationType)) {
          // Insertion
          val mismatch = !referenceAllele.equals(REFERENCE_INSERTION_VALUE);
          if (mismatch) {
            val type = REFERENCE_GENOME_INSERTION_ERROR;
            val value = formatValue(REFERENCE_INSERTION_VALUE, referenceAllele);
            val columnName = SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE;
            val param = assemblyVersion;

            // Database
            context.reportError(fileName, lineNumber, columnName, value, type, param);

            // Report file
            val tupleState = new TupleState(lineNumber);
            tupleState.reportError(type, columnName, value, param);
            writer.writeValue(outputStream, tupleState);
          }
        } else {
          // Deletion or substitution
          val chromosome = getChromosome(context, chromosomeCode);
          val referenceSequence = getReferenceGenomeSequence(chromosome, start, end);

          val mismatch = !isMatch(referenceAllele, referenceSequence);
          if (mismatch) {
            val type = REFERENCE_GENOME_MISMATCH_ERROR;
            val value = formatValue(REFERENCE_INSERTION_VALUE, referenceAllele);
            val columnName = SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE;
            val param = assemblyVersion;

            // Database
            context.reportError(fileName, lineNumber, columnName, value, type, param);

            // Report file
            val tupleState = new TupleState(lineNumber);
            tupleState.reportError(type, columnName, value, param);
            writer.writeValue(outputStream, tupleState);
          }
        }

        // Cooperate
        checkInterrupted(getName());
      }

    });
  }

  /**
   * Extracts the assembly version from the file name.
   * 
   * @param fastaFile the FASTA file
   * @return the assembly version
   */
  private static String getAssemblyVersion(File fastaFile) {
    return getNameWithoutExtension(fastaFile.getName());
  }

  private static String getChromosome(ValidationContext context, String chromosomeCode) {
    val value = tryParse(chromosomeCode);

    val alpha = value == null;
    if (alpha) {
      // CodeList value was passed
      return chromosomeCode;
    }

    if (value >= 1 && value <= 22) {
      // CodeList value and terms are the same
      return chromosomeCode;
    }

    if (value == 23) {
      return "X";
    }
    if (value == 24) {
      return "Y";
    }
    if (value == 25) {
      return "MT";
    }

    throw new IllegalStateException("Could not convert term for code '" + chromosomeCode + "'");
  }

  private static boolean isInsertionType(String mutationType) {
    return mutationType.equals(INSERTION_MUTATION_TYPE);
  }

  private static boolean isMatch(String controlAllele, String refSequence) {
    return controlAllele.equalsIgnoreCase(refSequence);
  }

  private static String formatValue(String expected, String actual) {
    return String.format("Expected: %s, Actual: %s", expected, actual);
  }

  /**
   * Returns a {@code OutputStream} to capture all reported errors.
   */
  private static OutputStream getOutputStream(ValidationContext context, Path ssmPrimaryFile) throws IOException {
    val directory = context.getSubmissionDirectory().getValidationDirPath();
    val fileName = format("%s.rgv--errors.json", getNameWithoutExtension(ssmPrimaryFile.getName()));
    val path = new Path(directory, fileName);

    return context.getFileSystem().create(path);
  }

  /**
   * Returns a {@code TupleState} json report writer.
   */
  private ObjectWriter createReportWriter() {
    return new ObjectMapper()
        .configure(AUTO_CLOSE_TARGET, false)
        .writer()
        .withDefaultPrettyPrinter();
  }

}
