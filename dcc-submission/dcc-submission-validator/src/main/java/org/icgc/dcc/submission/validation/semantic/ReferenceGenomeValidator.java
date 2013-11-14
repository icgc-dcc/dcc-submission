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

import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.Files.getFileExtension;
import static com.google.common.io.Files.getNameWithoutExtension;
import static com.google.common.primitives.Ints.tryParse;
import static java.util.Arrays.asList;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME_END;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME_START;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE;
import static org.icgc.dcc.submission.validation.core.ErrorType.REFERENCE_GENOME_ERROR;
import static org.icgc.dcc.submission.validation.core.Validators.checkInterrupted;
import static org.icgc.dcc.submission.validation.platform.PlatformStrategy.FIELD_SEPARATOR;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import net.sf.picard.reference.IndexedFastaSequenceFile;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.BZip2Codec;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;

import com.google.common.base.Optional;
import com.google.common.io.LineReader;

/**
 * Support querying a reference genome data file in the form for chromosome-start-end to validate submission input.
 * <p>
 * This uses the picard utilities to query an indexed FASTA file, as a bench mark reference we can check roughly
 * 3,000,000 reference genomes in 200 seconds.
 * 
 * @see https://wiki.oicr.on.ca/display/DCCSOFT/Unify+genome+assembly+build+throughout+the+system
 */
@Slf4j
public class ReferenceGenomeValidator implements Validator {

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
    this.assemblyVersion = getNameWithoutExtension(fastaFile.getName());
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

    // This validation is only applicable if ssm_p is available
    Optional<Path> optionalSsmPrimaryFile = context.getSsmPrimaryFile();
    val skip = !optionalSsmPrimaryFile.isPresent();
    if (skip) {
      log.info("No ssm_p file for '{}'", context.getProjectKey());

      return;
    }

    // Exists
    val ssmPrimaryFile = optionalSsmPrimaryFile.get();

    // Source input
    @Cleanup
    val inputStream = getInputStream(ssmPrimaryFile, context.getFileSystem());

    // Get to work
    log.info("Performing reference genome validation on file '{}' for '{}'", ssmPrimaryFile, context.getProjectKey());
    validate(context, ssmPrimaryFile.getName(), inputStream);
    log.info("Finished performing reference genome validation for '{}'", context.getProjectKey());
  }

  private void validate(final ValidationContext context, final String fileName, InputStream inputStream) {
    val reader = new LineReader(new InputStreamReader(inputStream));
    parse(reader, new LineProcessor() {

      @Override
      public void process(long lineNumber, String start, String end, String chromosomeCode, String referenceAllele) {
        val chromosome = getChromosome(context, chromosomeCode);
        val referenceSequence = getReferenceGenomeSequence(chromosome, start, end);

        val mismatch = !isMatch(referenceAllele, referenceSequence);
        if (mismatch) {
          context.reportError(
              fileName,
              lineNumber,
              SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE,
              formatValue(referenceSequence, referenceAllele),
              REFERENCE_GENOME_ERROR,

              // Params
              assemblyVersion);
        }

        // Cooperate
        checkInterrupted(getName());
      }

    });
  }

  @SneakyThrows
  public void parse(LineReader reader, LineProcessor callback) {
    // @formatter:off
    // Field indexes
    int      chromosomeIdx = -1;
    int           startIdx = -1;
    int             endIdx = -1;
    int referenceAlleleIdx = -1;

    // Line state (one-based)
    long lineNumber = 1;
    String line;

    // Read all lines
    while ((line = reader.readLine()) != null) {
      val fields = parseLine(line);

      if (lineNumber == 1) {
              chromosomeIdx = fields.indexOf(SUBMISSION_OBSERVATION_CHROMOSOME);
                   startIdx = fields.indexOf(SUBMISSION_OBSERVATION_CHROMOSOME_START);
                     endIdx = fields.indexOf(SUBMISSION_OBSERVATION_CHROMOSOME_END);
         referenceAlleleIdx = fields.indexOf(SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE);
      } else {
        // Delegate logic
        callback.process(lineNumber, fields.get(startIdx), fields.get(endIdx), fields.get(chromosomeIdx), fields.get(referenceAlleleIdx));
      }

      // Book-keeping
      lineNumber++;
    }
    // @formatter:on
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
  private static void download(String source, String target, boolean compressed) {
    log.info("Downloading '{}' to '{}'...", source, target);
    @Cleanup
    val inputStream = new BufferedInputStream(createUrlStream(source, compressed));
    @Cleanup
    val outputStream = new FileOutputStream(target);

    copy(inputStream, outputStream);
    log.info("Finished downloading '{}' to '{}'", source, target);
  }

  private static InputStream createUrlStream(String source, boolean compressed) throws IOException {
    val connection = new URL(source).openConnection();
    val inputStream = connection.getInputStream();

    return compressed ? new GZIPInputStream(inputStream) : inputStream;
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

  private static List<String> parseLine(String line) {
    return asList(line.split(FIELD_SEPARATOR));
  }

  private static boolean isMatch(String controlAllele, String refSequence) {
    return controlAllele.equalsIgnoreCase(refSequence);
  }

  private static String formatValue(String expected, String actual) {
    return String.format("Expected: %s, Actual: %s", expected, actual);
  }

  /**
   * Returns a {@InputStream} capable of reading {@code gz}, {@code bzip2} or plain text files
   */
  private static InputStream getInputStream(Path path, FileSystem fileSystem) throws IOException {
    val extension = getFileExtension(path.getName());
    val gzip = extension.equals("gz");
    val bzip2 = extension.equals("bz2");

    // @formatter:off
    val inputStream = fileSystem.open(path);
    return 
        gzip  ? new GZIPInputStream(inputStream)                : 
        bzip2 ? new BZip2Codec().createInputStream(inputStream) :
                inputStream;
    // @formatter:on
  }

  /**
   * Simple callback for line processing.
   */
  private interface LineProcessor {

    void process(long lineNumber, String start, String end, String chromosome, String referenceAllele);

  }

}
