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
package org.icgc.dcc.submission.validation.key.core;

import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.icgc.dcc.submission.validation.key.core.KVDictionary.getOptionalReferencedFileType;
import static org.icgc.dcc.submission.validation.key.core.KVDictionary.hasOutgoingSurjectiveRelation;
import static org.icgc.dcc.submission.validation.key.core.KVFileDescription.getFileDescription;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SPECIMEN;

import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.validation.key.data.KVEncounteredForeignKeys;
import org.icgc.dcc.submission.validation.key.data.KVFileProcessor;
import org.icgc.dcc.submission.validation.key.data.KVPrimaryKeys;
import org.icgc.dcc.submission.validation.key.enumeration.KVExperimentalDataType;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.key.report.KVReport;
import org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator;

import com.google.common.base.Optional;

@Slf4j
@RequiredArgsConstructor
public class KVValidator {

  /**
   * TODO: temporarily...
   */
  public static final boolean TUPLE_CHECKS_ENABLED = true;

  @NonNull
  private final KVFileParser fileParser;
  @NonNull
  private final KVFileSystem kvFileSystem;
  @NonNull
  private final KVReport reporter;

  private final Map<KVFileType, KVPrimaryKeys> fileTypeToPrimaryKeys = newHashMap(); // TODO: wrapper?
  private final SurjectivityValidator surjectivityValidator = new SurjectivityValidator();

  public void processSubmission() {
    log.info("Loading data");

    // Process clinical data
    log.info("Processing clinical data");
    processFileType(DONOR);
    processFileType(SPECIMEN);
    processFileType(SAMPLE);

    // Process experimental data
    for (val dataType : KVExperimentalDataType.values()) {
      if (kvFileSystem.hasDataType(dataType)) {
        log.info("Processing '{}' data", dataType);
        for (val fileType : dataType.getFileTypes()) { // Order matters!
          processFileType(fileType);
        }
      } else {
        log.info("No '{}' data", dataType);
      }
    }

    log.info("{}", banner("="));
    for (val fileType : fileTypeToPrimaryKeys.keySet()) {
      log.debug("{}: {}", fileType, fileTypeToPrimaryKeys.get(fileType));
    }
    log.debug("{}", banner("="));

    // Report
    boolean valid = false;// submissionErrors.reportSubmissionErrors(kvReport);
    log.info("{}", valid);
    log.info("done.");
  }

  /**
   * TODO: create abstraction for file type
   */
  public void processFileType(KVFileType fileType) {
    log.info("{}", banner("="));

    // Primary keys for the type under consideration (each file will augment it)
    val primaryKeys = new KVPrimaryKeys();

    // Obtain referenced file type (if applicable, for instance DONOR has none)
    val optionalReferencedFileType = getOptionalReferencedFileType(fileType);

    // Obtain corresponding PKs for the referenced file type (also if applicable)
    val optionalReferencedPrimaryKeys = optionalReferencedFileType.isPresent() ?
        of(fileTypeToPrimaryKeys.get(optionalReferencedFileType.get())) :
        Optional.<KVPrimaryKeys> absent();

    // Encountered foreign keys in the case where we need to check for surjection
    val optionalEncounteredForeignKeys = hasOutgoingSurjectiveRelation(fileType) ?
        of(new KVEncounteredForeignKeys()) :
        Optional.<KVEncounteredForeignKeys> absent();

    log.info("Processing file type: '{}'; Referencing '{}' (will be collecting FKs: '{}')",
        new Object[] { fileType, optionalReferencedFileType, optionalEncounteredForeignKeys.isPresent() });

    val dataFilePaths = kvFileSystem.getDataFilePaths(fileType);
    checkState(dataFilePaths.isPresent(),
        "Expecting to find at least one matching file at this point for: '%s'", fileType);
    for (val dataFilePath : dataFilePaths.get()) {
      processFile(
          getFileDescription(fileType, dataFilePath),
          dataFilePath,
          primaryKeys,
          optionalEncounteredForeignKeys,
          optionalReferencedFileType,
          optionalReferencedPrimaryKeys);
    }
    fileTypeToPrimaryKeys.put(fileType, primaryKeys);

    postProcessing(fileType, optionalReferencedFileType, optionalEncounteredForeignKeys);
  }

  private void processFile(
      KVFileDescription fileDescription,
      Path filePath,
      KVPrimaryKeys primaryKeys,
      Optional<KVEncounteredForeignKeys> optionalEncounteredForeignKeys,
      Optional<KVFileType> optionalReferencedType,
      Optional<KVPrimaryKeys> optionalReferencedPrimaryKeys) {
    log.info("{}", banner("-"));
    log.info("Processing file: '{}'; Referencing '{}': '{}'",
        new Object[] { filePath, optionalReferencedType, fileDescription });

    // TODO: subclass for referencing/non-referencing?
    new KVFileProcessor(fileDescription)

        // Process file
        .processFile(
            fileParser,
            reporter,
            primaryKeys,
            optionalReferencedPrimaryKeys,
            optionalEncounteredForeignKeys
        );
  }

  /**
   * For surjection checks.
   */
  private void postProcessing(
      KVFileType fileType,
      Optional<KVFileType> optionalReferencedType,
      Optional<KVEncounteredForeignKeys> optionalEncounteredForeignKeys) {

    log.info("{}", banner("-"));
    if (hasOutgoingSurjectiveRelation(fileType)) {
      log.info("Post-processing: surjectivity check for type '{}'", fileType);

      checkState(optionalReferencedType.isPresent());
      val referencedType = optionalReferencedType.get();

      checkState(optionalEncounteredForeignKeys.isPresent(), "TODO");
      surjectivityValidator
          .validateSurjection(
              fileType,
              fileTypeToPrimaryKeys.get(referencedType),
              optionalEncounteredForeignKeys.get(),
              reporter,
              referencedType);
    } else {
      log.info("No outgoing surjection relation for file type: '{}'", fileType);
    }
  }

  private String banner(String symbol) {
    return repeat(symbol, 75);
  }
}
