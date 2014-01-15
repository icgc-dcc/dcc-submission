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
import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Maps.transformValues;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.RELATIONS;
import static org.icgc.dcc.submission.validation.key.core.KVFileDescription.getFileDescription;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SPECIMEN;

import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.data.KVEncounteredForeignKeys;
import org.icgc.dcc.submission.validation.key.data.KVFileDataDigest;
import org.icgc.dcc.submission.validation.key.data.KVPrimaryKeys;
import org.icgc.dcc.submission.validation.key.enumeration.KVExperimentalDataType;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.key.error.KVSubmissionErrors;
import org.icgc.dcc.submission.validation.key.report.KVReport;
import org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator;

import com.google.common.base.Function;
import com.google.common.base.Optional;

@Slf4j
public class KVValidator {

  /**
   * TODO: temporarily...
   */
  public static final boolean TUPLE_CHECKS_ENABLED = true;
  private static final Optional<KVPrimaryKeys> REFERENCED_PRIMARY_KEY_NOT_APPLICABLE =
      Optional.<KVPrimaryKeys> absent();
  private static final Optional<KVEncounteredForeignKeys> NA =
      Optional.<KVEncounteredForeignKeys> absent();

  @NonNull
  private final KVFileParser kvFileParser;
  @NonNull
  private final KVFileSystem kvFileSystem;
  @NonNull
  private final KVReport kvReport;
  @NonNull
  private final SurjectivityValidator surjectivityValidator;

  private final Map<KVFileType, KVFileDataDigest> data = newLinkedHashMap(); // TODO: still needed?

  // TODO: create new file type abstraction to hold those
  private final Map<KVFileType, KVPrimaryKeys> fileTypeToPrimaryKeys = newHashMap();

  private final KVSubmissionErrors errors = new KVSubmissionErrors();

  public KVValidator(KVFileParser kvFileParser, KVFileSystem kvFileSystem, KVReport kvReport) {
    this.kvFileParser = kvFileParser;
    this.kvFileSystem = kvFileSystem;
    this.kvReport = kvReport;
    this.surjectivityValidator = new SurjectivityValidator(kvFileSystem);
  }

  public void validate() {
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

    log.info("{}", repeat("=", 75));
    for (val fileType : data.keySet()) {
      log.debug("{}: {}", fileType, data.get(fileType));
      log.debug("{}", repeat("-", 75));
      log.debug("{}: {}", fileType, fileTypeToPrimaryKeys.get(fileType));
    }
    log.debug("{}", repeat("=", 75));

    // Surjection validation (can only be done at the very end)
    // validateComplexSurjection(); // TODO: to be re-enabled later

    // Report
    boolean valid = errors.describe(
        kvReport,
        copyOf(transformValues(
            data,
            new Function<KVFileDataDigest, KVFileDescription>() {

              @Override
              public KVFileDescription apply(KVFileDataDigest datum) {
                return datum.getKvFileDescription();
              }
            })));
    log.info("{}", valid);
    log.info("done.");
  }

  /**
   * TODO: create abstraction for file type
   */
  private void processFileType(KVFileType fileType) {

    // Primary keys for the type under consideration (each file will augment it)
    val primaryKeys = new KVPrimaryKeys(fileType);

    // Encountered foreign keys in the case where we need to check for surjection
    val optionalEncounteredForeignKeys = fileType.hasOutgoingSurjectiveRelation() ?
        of(new KVEncounteredForeignKeys()) :
        NA;

    val optionalReferencedType = RELATIONS.containsKey(fileType) ?
        Optional.of(RELATIONS.get(fileType)) : Optional.<KVFileType> absent();
    log.info("{}", repeat("=", 75));
    log.info("Processing file type: '{}' ('{}'); Referencing '{}'", fileType, optionalReferencedType);

    val dataFilePaths = kvFileSystem.getDataFilePaths(fileType);
    checkState(dataFilePaths.isPresent(), "TODO");
    for (val dataFilePath : dataFilePaths.get()) {
      log.info("{}", repeat("-", 75));
      log.info("Processing file: '{}' ('{}'); Referencing '{}'",
          new Object[] { dataFilePath, fileType, optionalReferencedType });
      data.put( // TODO: map still needed?
          fileType,

          // TODO: subclass for referencing/non-referencing?
          new KVFileDataDigest(getFileDescription(fileType, dataFilePath))

              // Process file
              .processFile(
                  kvFileParser,
                  errors.getFileErrors(fileType),
                  primaryKeys,
                  optionalReferencedType.isPresent() ?
                      of(fileTypeToPrimaryKeys.get(optionalReferencedType.get())) :
                      REFERENCED_PRIMARY_KEY_NOT_APPLICABLE,
                  optionalEncounteredForeignKeys
              ));
    }

    postProcessing(fileType, optionalReferencedType, optionalEncounteredForeignKeys);

    fileTypeToPrimaryKeys.put(fileType, primaryKeys);
  }

  /**
   * For surjection checks.
   */
  private void postProcessing(
      KVFileType fileType,
      Optional<KVFileType> optionalReferencedType,
      Optional<KVEncounteredForeignKeys> optionalEncounteredForeignKeys) {

    if (fileType.hasOutgoingSimpleSurjectiveRelation()) {
      log.info("Post-processing: simply surjectivity check");

      checkState(optionalReferencedType.isPresent());
      val referencedType = optionalReferencedType.get();

      checkState(optionalEncounteredForeignKeys.isPresent(), "TODO");
      surjectivityValidator
          .validateSimpleSurjection(
              fileTypeToPrimaryKeys.get(referencedType),
              optionalEncounteredForeignKeys.get(),
              errors.getFileErrors(referencedType));
    }

    if (fileType.hasOutgoingComplexSurjectiveRelation()) {
      log.info("Post-processing: complex surjectivity addition");
      checkState(optionalEncounteredForeignKeys.isPresent(), "TODO");
      // Simply adding them all for now, actual validation will have to take place after all meta files have been read
      // (unlike for "simple" surjection check).
      surjectivityValidator.addEncounteredSampleKeys(optionalEncounteredForeignKeys.get());
    }
  }

  @SuppressWarnings("unused")
  private void validateComplexSurjection() {
    log.info("{}", repeat("=", 75));
    log.info("Validating complex surjection");
    surjectivityValidator.validateComplexSurjection(
        fileTypeToPrimaryKeys.get(SAMPLE),
        errors.getFileErrors(SAMPLE));
    log.info("{}", repeat("=", 75));
  }
}
