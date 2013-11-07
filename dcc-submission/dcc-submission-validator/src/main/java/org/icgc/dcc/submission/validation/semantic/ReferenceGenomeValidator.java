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
import static java.util.Arrays.asList;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE;
import static org.icgc.dcc.submission.validation.core.ErrorType.REFERENCE_GENOME_ERROR;
import static org.icgc.dcc.submission.validation.core.Validators.checkState;

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
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import net.sf.picard.reference.IndexedFastaSequenceFile;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.BZip2Codec;
import org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames;
import org.icgc.dcc.submission.validation.core.ReportContext;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;

import com.google.common.base.Optional;
import com.google.common.io.LineReader;

/**
 * Support querying a reference genome data file in the form for chromosome-start-end to validate submission input.
 * <p>
 * This uses the picard utilities to query an indexed FASTA file, as a bench mark reference we can check roughly
 * 3,000,000 reference genomes in 200 seconds.
 */
@Slf4j
@NoArgsConstructor
public class ReferenceGenomeValidator implements Validator {

  public static final String REFERENCE_GENOME_VERSION = "GrCh37";
  public static final String REFERENCE_GENOME_BASE_URL = "ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/technical/reference";
  public static final String REFERENCE_GENOME_DATA_URL = REFERENCE_GENOME_BASE_URL + "/" + "human_g1k_v37.fasta.gz";
  public static final String REFERENCE_GENOME_INDEX_URL = REFERENCE_GENOME_BASE_URL + "/" + "human_g1k_v37.fasta.fai";

  public static final String LOCAL_DIR = "/tmp";
  public static final String LOCAL_DATA_FILE = LOCAL_DIR + "/" + "referenceGenome.fasta";
  public static final String LOCAL_INDEX_FILE = LOCAL_DIR + "/" + "referenceGenome.fasta.fai";

  private static final String FIELD_SEPARATOR = "\t";

  private IndexedFastaSequenceFile sequenceFile;

  @Override
  public String getName() {
    return "Reference Genome Validator";
  }

  /**
   * Currently this is for testing only. For production it makes more sense to have the files setup in-place instead of
   * downloading them every time.
   */
  @SneakyThrows
  public void ensureDownload() {
    log.info("Checking if '{}' and '{}' exist", LOCAL_DATA_FILE, LOCAL_INDEX_FILE);
    val downloaded = new File(LOCAL_DATA_FILE).exists() && new File(LOCAL_INDEX_FILE).exists();
    if (downloaded) {
      log.info("Files exist");
      sequenceFile = new IndexedFastaSequenceFile(new File(LOCAL_DATA_FILE));

      return;
    }

    log.warn("Missing '{}' and / or '{}'", LOCAL_INDEX_FILE, LOCAL_DATA_FILE);
    download(REFERENCE_GENOME_INDEX_URL, LOCAL_INDEX_FILE, false);
    download(REFERENCE_GENOME_DATA_URL, LOCAL_DATA_FILE, true);

    log.info("Download successful");
    sequenceFile = new IndexedFastaSequenceFile(new File(LOCAL_DATA_FILE));
  }

  /**
   * Validate genome reference aligns with reference genome of submitted primary file. We assume at this stage the file
   * is well-formed, and that each individual field is sane.
   */
  @SneakyThrows
  @Override
  public void validate(ValidationContext context) {
    log.info("Starting...");
    ensureDownload();

    // This validation is only applicable if ssm_p is available
    Optional<Path> optionalSsmPrimaryFile = context.getSsmPrimaryFile();
    if (!optionalSsmPrimaryFile.isPresent()) {
      log.info("No ssm_p file for '{}'", context.getProjectKey());
    }

    // Create a streaming reader
    val ssmPrimaryFile = optionalSsmPrimaryFile.get();
    val reader = getLineReader(ssmPrimaryFile, context.getFileSystem());

    // Get to work
    log.info("Performing reference genome validation on file '{}' for '{}'", ssmPrimaryFile, context.getProjectKey());
    validate(context, ssmPrimaryFile.getName(), reader);
    log.info("Finished performing reference genome validation for '{}'", context.getProjectKey());
    log.info("Finished");
  }

  private void validate(ReportContext context, String fileName, LineReader reader) throws IOException {
    // Field indexes
    int chromosomeIdx = -1;
    int startIdx = -1;
    int endIdx = -1;
    int referenceAlleleIdx = -1;

    // Line state (one-based)
    long lineNumber = 1;
    String line;

    // Read all lines
    while ((line = reader.readLine()) != null) {
      val fields = parseLine(line);

      if (lineNumber == 1) {
        // Get column position
        chromosomeIdx = fields.indexOf(SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME);
        startIdx = fields.indexOf(SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME_START);
        endIdx = fields.indexOf(SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME_END);
        referenceAlleleIdx = fields.indexOf(SubmissionFieldNames.SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE);
      } else {
        val start = fields.get(startIdx);
        val end = fields.get(endIdx);

        // FIXME: This needs to be converted from CodeList "code" to "value", which means this class needs at least the
        // CodeList to be injected, by using the context
        val chromosome = fields.get(chromosomeIdx);

        val referenceAllele = fields.get(referenceAlleleIdx);
        val referenceSequence = getReferenceGenomeSequence(chromosome, start, end);

        boolean mismatch = !isMatch(referenceAllele, referenceSequence);
        if (mismatch) {
          context.reportError(
              fileName,
              lineNumber,
              SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE,
              formatValue(referenceSequence, referenceAllele),
              REFERENCE_GENOME_ERROR,

              // Params
              REFERENCE_GENOME_VERSION);
        }
      }

      // Cooperate
      checkState(getName());

      // Book-keeping
      lineNumber++;
    }
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
   * Returns a LineReader capable of reading gz, bzip2 or plain text files
   */
  private static LineReader getLineReader(Path path, FileSystem fileSystem) throws IOException {
    val extension = getFileExtension(path.getName());
    val gzip = extension.equals("gz");
    val bzip2 = extension.equals("bz2");

    // @formatter:off
    val inputStream = fileSystem.open(path);
    return new LineReader(new InputStreamReader(
        gzip  ? new GZIPInputStream(inputStream)                : 
        bzip2 ? new BZip2Codec().createInputStream(inputStream) :
                inputStream
        ));
    // @formatter:on
  }

}
