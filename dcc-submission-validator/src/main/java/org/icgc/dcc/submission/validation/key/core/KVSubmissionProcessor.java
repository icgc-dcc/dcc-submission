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
package org.icgc.dcc.submission.validation.key.core;

import static com.google.common.collect.Maps.newHashMap;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.icgc.dcc.common.core.util.Formats.formatBytes;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableMap;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.BIOMARKER;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.EXPOSURE;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.FAMILY;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.SURGERY;
import static org.icgc.dcc.submission.validation.key.core.KVFileType.THERAPY;

import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.util.stream.Collectors;
import org.icgc.dcc.submission.validation.key.data.KVEncounteredForeignKeys;
import org.icgc.dcc.submission.validation.key.data.KVFileProcessor;
import org.icgc.dcc.submission.validation.key.data.KVPrimaryKeys;
import org.icgc.dcc.submission.validation.key.data.KVReferencedPrimaryKeys;
import org.icgc.dcc.submission.validation.key.report.KVReporter;
import org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator;

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

    // TODO: Add metadata to the dictionary which indicates which file type is this clinical or experimantal(feature)?
    // val fileTypes = dictionary.getTopologicallyOrderedFileTypes();

    // Process clinical data
    log.info("Processing clinical data");
    processFileType(DONOR);
    processFileType(SPECIMEN);
    processFileType(SAMPLE);

    log.info("Processing clinicla supplemental data");
    processFileType(BIOMARKER);
    processFileType(FAMILY);
    processFileType(EXPOSURE);
    processFileType(SURGERY);
    processFileType(THERAPY);

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

    val referencedPrimaryKeys = getReferencedPrimaryKeys(fileType);
    if (!referencedPrimaryKeys.isEmpty()) {
      log.info("Created collectors for referenced file types: {}", referencedPrimaryKeys.keySet());
    }

    // Encountered foreign keys in the case where we need to check for surjection
    val encounteredForeignKeys = createEncounteredForeignKeys(fileType);
    log.info("Processing file type: '{}'; has referencing is '{}'; will be collecting FKs for '{}'",
        new Object[] { fileType, !referencedPrimaryKeys.isEmpty(), encounteredForeignKeys.keySet() });

    // Process files matching the current file type
    val dataFilePaths = kvFileSystem.getDataFilePaths(fileType);
    if (dataFilePaths.isPresent()) {
      for (val dataFilePath : dataFilePaths.get()) {
        val watch = createStopwatch();
        log.info("{}", banner("-"));
        log.info("Processing '{}' file: '{}'; has referencing is '{}'",
            new Object[] { fileType, !referencedPrimaryKeys.isEmpty(), dataFilePath });

        // TODO: subclass for referencing/non-referencing?
        val fileProcessor = new KVFileProcessor(fileType, dataFilePath);
        fileProcessor.processFile(
            dictionary,
            fileParser,
            reporter,
            primaryKeys,
            referencedPrimaryKeys,
            encounteredForeignKeys);

        log.info("Finished processing file '{}' in {} with {} of JVM free memory remaining",
            new Object[] { dataFilePath, watch, formatFreeMemory() });
      }
    } else {
      log.info("Skipping '{}', there are no matching files", fileType);
    }
    fileTypeToPrimaryKeys.put(fileType, primaryKeys);

    encounteredForeignKeys.entrySet()
        .forEach(entry -> checkSurjection(fileType, entry.getKey(), entry.getValue()));
  }

  private void checkSurjection(
      KVFileType fileType,
      KVFileType referencedType,
      KVEncounteredForeignKeys encounteredForeignKeys) {

    log.info("{}", banner("-"));
    log.info("Post-processing: surjectivity check for type '{}'", fileType);

    surjectivityValidator
        .validateSurjection(
            fileType,
            fileTypeToPrimaryKeys.get(referencedType),
            encounteredForeignKeys,
            reporter,
            referencedType);
  }

  private Map<KVFileType, KVEncounteredForeignKeys> createEncounteredForeignKeys(KVFileType fileType) {
    return dictionary.getSurjectiveReferencedTypes(fileType).stream()
        .collect(Collectors.toImmutableMap(rft -> rft, rft -> new KVEncounteredForeignKeys()));
  }

  private Map<KVFileType, KVReferencedPrimaryKeys> getReferencedPrimaryKeys(KVFileType fileType) {
    log.info("{} - {}", fileType, dictionary.getParents(fileType));

    return dictionary.getParents(fileType).stream()
        .collect(toImmutableMap(
            parent -> parent,
            // fileTypeToPrimaryKeys.get(parent) is not null as populated by parent's check
            parent -> new KVReferencedPrimaryKeys(parent, fileTypeToPrimaryKeys.get(parent))));
  }

  private static String banner(String symbol) {
    return repeat(symbol, 75);
  }

  private static String formatFreeMemory() {
    return formatBytes(Runtime.getRuntime().freeMemory());
  }

  @SuppressWarnings("deprecation")
  private static Stopwatch createStopwatch() {
    // Can't use the new API here because Hadoop doesn't know about it cluster side. Trying to use it will result in
    // errors.
    return new Stopwatch().start();
  }

}