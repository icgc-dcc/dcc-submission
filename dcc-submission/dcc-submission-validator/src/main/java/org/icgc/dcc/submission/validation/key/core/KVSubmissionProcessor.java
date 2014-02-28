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
import static org.icgc.dcc.core.util.FormatUtils.formatBytes;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SPECIMEN;

import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.data.KVEncounteredForeignKeys;
import org.icgc.dcc.submission.validation.key.data.KVFileProcessor;
import org.icgc.dcc.submission.validation.key.data.KVPrimaryKeys;
import org.icgc.dcc.submission.validation.key.data.KVReferencedPrimaryKeys;
import org.icgc.dcc.submission.validation.key.report.KVReporter;
import org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;

/**
 * Main processor for the key validation.
 */
@Slf4j
@RequiredArgsConstructor
public class KVSubmissionProcessor {

  /**
   * Enables/disables checks at the row level (for performance); TODO: make this configurable.
   */
  public static final boolean ROW_CHECKS_ENABLED = true;

  @NonNull
  private final KVDictionary dictionary;
  @NonNull
  private final KVFileParser fileParser;
  @NonNull
  private final KVFileSystem kvFileSystem;
  @NonNull
  private final KVReporter reporter;

  private final Map<KVFileType, KVPrimaryKeys> fileTypeToPrimaryKeys = newHashMap();
  private final SurjectivityValidator surjectivityValidator = new SurjectivityValidator();

  public void processSubmission() {
    log.info("Loading data");

    // Process clinical data
    log.info("Processing clinical data");
    processFileType(DONOR);
    processFileType(SPECIMEN);
    processFileType(SAMPLE);

    // Process experimental data
    for (val dataType : dictionary.getExperimentalDataTypes()) {
      if (kvFileSystem.hasDataType(dataType)) {
        log.info("Processing '{}' data", dataType);
        for (val fileType : dictionary.getExperimentalFileTypes(dataType)) { // Order matters!
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
    log.info("done.");
  }

  public void processFileType(KVFileType fileType) {
    log.info("{}", banner("="));

    // Primary keys for the type under consideration (each file will augment it)
    val primaryKeys = new KVPrimaryKeys();

    val optionallyReferencedPrimaryKeys1 = getOptionallyReferencedPrimaryKeys1(fileType);
    val optionallyReferencedPrimaryKeys2 = getOptionallyReferencedPrimaryKeys2(fileType);

    // Encountered foreign keys in the case where we need to check for surjection
    val optionalEncounteredForeignKeys = dictionary.hasOutgoingSurjectiveRelation(fileType) ?
        of(new KVEncounteredForeignKeys()) :
        Optional.<KVEncounteredForeignKeys> absent();

    log.info(
        "Processing file type: '{}'; has referencing is '{} and {}' (FK1 and FK2); will be collecting FKs: '{}'",
        new Object[] { fileType,
            optionallyReferencedPrimaryKeys1.isPresent(), optionallyReferencedPrimaryKeys2.isPresent(),
            optionalEncounteredForeignKeys.isPresent() });

    // Process files matching the current file type
    val dataFilePaths = kvFileSystem.getDataFilePaths(fileType);
    if (dataFilePaths.isPresent()) {
      for (val dataFilePath : dataFilePaths.get()) {
        val watch = createStopwatch();
        log.info("{}", banner("-"));
        log.info(
            "Processing '{}' file: '{}'; has referencing is '{}' and '{}' (FK1 and FK2)",
            new Object[] { fileType, optionallyReferencedPrimaryKeys1.isPresent(), optionallyReferencedPrimaryKeys2
                .isPresent(), dataFilePath });

        // TODO: subclass for referencing/non-referencing?
        val fileProcessor = new KVFileProcessor(fileType, dataFilePath);
        fileProcessor.processFile(
            dictionary,
            fileParser,
            reporter,
            primaryKeys,
            optionallyReferencedPrimaryKeys1,
            optionallyReferencedPrimaryKeys2,
            optionalEncounteredForeignKeys
            );

        log.info("Finished processing file '{}' in {} with {} of JVM free memory remaining",
            new Object[] { dataFilePath, watch, formatFreeMemory() });
      }
    } else {
      log.info("Skipping '{}', there are no matching files", fileType);
    }
    fileTypeToPrimaryKeys.put(fileType, primaryKeys);

    // Check surjection (N/A for FK2 or optional FK at the moment)
    if (optionallyReferencedPrimaryKeys1.isPresent()) {
      checkSurjection(
          fileType,
          optionallyReferencedPrimaryKeys1.get()
              .getReferencedFileType(),
          optionalEncounteredForeignKeys);
    }
  }

  private void checkSurjection(
      KVFileType fileType,
      KVFileType referencedType,
      Optional<KVEncounteredForeignKeys> optionalEncounteredForeignKeys) {

    log.info("{}", banner("-"));
    if (dictionary.hasOutgoingSurjectiveRelation(fileType)) {
      log.info("Post-processing: surjectivity check for type '{}'", fileType);

      checkState(optionalEncounteredForeignKeys.isPresent());
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

  private Optional<KVReferencedPrimaryKeys> getOptionallyReferencedPrimaryKeys1(KVFileType fileType) {
    return getOptionallyReferencedPrimaryKeys(fileType, false);
  }

  private Optional<KVReferencedPrimaryKeys> getOptionallyReferencedPrimaryKeys2(KVFileType fileType) {
    return getOptionallyReferencedPrimaryKeys(fileType, true);
  }

  private Optional<KVReferencedPrimaryKeys> getOptionallyReferencedPrimaryKeys(KVFileType fileType, boolean secondary) {
    // Obtain referenced file type (if applicable, for instance DONOR has none)
    val optionalReferencedFileType = secondary ?
        dictionary.getOptionalReferencedFileType2(fileType) :
        dictionary.getOptionalReferencedFileType1(fileType);
    log.info("'{}' references '{}'", fileType, optionalReferencedFileType);

    if (optionalReferencedFileType.isPresent()) {
      // Obtain corresponding PKs for the referenced file type (also if applicable)
      val referencedFileType = optionalReferencedFileType.get();
      return Optional.<KVReferencedPrimaryKeys> of(
          new KVReferencedPrimaryKeys(
              referencedFileType,
              fileTypeToPrimaryKeys.get(referencedFileType)));
    } else {
      return Optional.absent();
    }
  }

  @SuppressWarnings("deprecation")
  private static Stopwatch createStopwatch() {
    // Can't use the new API here because Hadoop doesn't know about it.
    return new Stopwatch().start();
  }

  private static String banner(String symbol) {
    return repeat(symbol, 75);
  }

  private static String formatFreeMemory() {
    return formatBytes(Runtime.getRuntime().freeMemory());
  }

}