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

import static java.util.Arrays.asList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.zip.GZIPInputStream;

import lombok.Cleanup;
import net.sf.picard.reference.IndexedFastaSequenceFile;
import net.sf.picard.reference.ReferenceSequence;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.BZip2Codec;
import org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames;
import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.cascading.TupleState.TupleError;
import org.icgc.dcc.submission.validation.core.ValidationErrorCode;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.LineReader;

/**
 * Support querying a reference genome data file in the form for chromosome-start-end to validate submission input This
 * uses the picard utilities to query an indexed fasta file, as a bench mark reference we can check roughly 3,000,000
 * reference genomes in 200 seconds.
 */
public class ReferenceGenomeValidator {

  private IndexedFastaSequenceFile sequenceFile = null;

  private final String REFERENCE_GENOME_DATA_URL =
      "ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/technical/reference/human_g1k_v37.fasta.gz";
  private final String REFERENCE_GENOME_INDEX_URL =
      "ftp://ftp.1000genomes.ebi.ac.uk/vol1/ftp/technical/reference/human_g1k_v37.fasta.fai";

  public ReferenceGenomeValidator() {
  }

  public ReferenceGenomeValidator(File fastaFile) throws FileNotFoundException {
    sequenceFile = new IndexedFastaSequenceFile(fastaFile);
  }

  /**
   * Currently this is for testing only. For production it makes more sense to have the files setup in-place instead of
   * downloading them every time.
   * @throws Exception
   */
  public void downloadAndSetGenomeDataFiles() throws Exception {
    @Cleanup
    BufferedInputStream bufferedInputStream = null;

    @Cleanup
    GZIPInputStream gzipInputStream = null;

    @Cleanup
    FileOutputStream dataOutputStream = null;

    @Cleanup
    FileOutputStream indexOutputStream = null;

    URL url = null;
    URLConnection conn = null;

    String referenceFile = "/tmp/referenceGenome.fasta";
    String indexFile = "/tmp/referenceGenome.fasta.fai";

    // Exit if files already exist
    if (new File(referenceFile).exists() && new File(indexFile).exists()) {
      sequenceFile = new IndexedFastaSequenceFile(new File(referenceFile));
      return;
    }

    // Copy the data index file
    url = new URL(REFERENCE_GENOME_INDEX_URL);
    conn = url.openConnection();
    bufferedInputStream = new BufferedInputStream(conn.getInputStream());
    indexOutputStream = new FileOutputStream(indexFile);
    ByteStreams.copy(bufferedInputStream, indexOutputStream);

    // Copy the main data file (g-zipped)
    url = new URL(REFERENCE_GENOME_DATA_URL);
    conn = url.openConnection();
    gzipInputStream = new GZIPInputStream(new BufferedInputStream(conn.getInputStream()));
    dataOutputStream = new FileOutputStream(referenceFile);
    ByteStreams.copy(gzipInputStream, dataOutputStream);

    sequenceFile = new IndexedFastaSequenceFile(new File(referenceFile));
  }

  public List<TupleError> validate(Path ssmPrimaryFile, FileSystem fileSystem) throws IOException {
    // Validate genome reference aligns with reference genome of submitted primary file. We assume at this stage the
    // file is well-formed, and that each individual field is sane

    LineReader reader = null;

    // File extensions
    if (ssmPrimaryFile.getName().endsWith(".gz")) {
      reader = new LineReader(new InputStreamReader(new GZIPInputStream(fileSystem.open(ssmPrimaryFile))));
    } else if (ssmPrimaryFile.getName().endsWith(".bz2")) {
      BZip2Codec codec = new BZip2Codec();
      reader = new LineReader(new InputStreamReader(codec.createInputStream((fileSystem.open(ssmPrimaryFile)))));
    } else {
      reader = new LineReader(new InputStreamReader(fileSystem.open(ssmPrimaryFile)));
    }

    List<TupleError> errors = Lists.newArrayList();
    long lineNumber = 1;
    int chromosomeIdx = -1;
    int startIdx = -1;
    int endIdx = -1;
    int referenceAlleleIdx = -1;
    String line;

    while ((line = reader.readLine()) != null) {
      List<String> fields = asList(line.split("\t"));

      if (lineNumber == 1) {
        // Get column position
        chromosomeIdx = fields.indexOf(SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME);
        startIdx = fields.indexOf(SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME_START);
        endIdx = fields.indexOf(SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME_END);
        referenceAlleleIdx = fields.indexOf(SubmissionFieldNames.SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE);
      } else {
        String refSequence =
            getReferenceGenomeSequence(fields.get(chromosomeIdx), fields.get(startIdx), fields.get(endIdx));

        if (!fields.get(referenceAlleleIdx).equalsIgnoreCase(refSequence)) {
          errors.add(TupleState.createTupleError(ValidationErrorCode.REFERENCE_GENOME_VIOLATION,
              SubmissionFieldNames.SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE, fields.get(referenceAlleleIdx),
              lineNumber, refSequence));
        }
      }
      lineNumber++;
    }
    return errors;
  }

  public String getReferenceGenomeSequence(String chromosome, String start, String end) {
    long startPosition = Long.parseLong(start);
    long endPosition = Long.parseLong(end);
    return getReferenceGenomeSequence(chromosome, startPosition, endPosition);
  }

  public String getReferenceGenomeSequence(String chromosome, long start, long end) {
    ReferenceSequence refSequence = sequenceFile.getSubsequenceAt(chromosome, start, end);
    return new String(refSequence.getBases());
  }

}
