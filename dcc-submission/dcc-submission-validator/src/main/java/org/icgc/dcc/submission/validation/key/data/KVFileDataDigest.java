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
package org.icgc.dcc.submission.validation.key.data;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newTreeSet;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_M_FKS1;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_M_FKS2;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_P_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_S_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.DONOR_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.EXP_G_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.EXP_M_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.EXP_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.JCN_M_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.JCN_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.JCN_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MAPPER;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_M_FKS1;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_M_FKS2;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_P_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_S_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MIRNA_M_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MIRNA_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MIRNA_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MIRNA_S_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MIRNA_S_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.PEXP_M_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.PEXP_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.PEXP_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SAMPLE_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SAMPLE_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SGV_M_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SGV_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SGV_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SPECIMEN_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SPECIMEN_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_M_FKS1;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_M_FKS2;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_M_FKS1;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_M_FKS2;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_P_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_S_FKS;
import static org.icgc.dcc.submission.validation.key.data.KVKeyValues.NOT_APPLICABLE;
import static org.icgc.dcc.submission.validation.key.data.KVKeyValues.from;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.PRIMARY_RELATION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.SECONDARY_RELATION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.UNIQUENESS;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_G;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.JCN_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.JCN_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.PEXP_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.PEXP_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SGV_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SGV_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.STSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.STSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.STSM_S;

import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.parser.FileRecordProcessor;
import org.icgc.dcc.submission.validation.key.core.KVFileDescription;
import org.icgc.dcc.submission.validation.key.core.KVFileParser;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.key.error.KVFileErrors;
import org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator;

import com.google.common.base.Optional;

@Slf4j
public class KVFileDataDigest {

  /**
   * TODO: temporarily...
   */
  public static final boolean TUPLE_CHECKS_ENABLED = true;

  protected final KVFileParser kvFileParser;
  @Getter
  protected final KVFileDescription kvFileDescription;
  private final long logThreshold;

  @Getter
  protected final Set<KVKeyValues> pks = newTreeSet(); // TODO: change to arrays?
  private Optional<KVFileDataDigest> optionalReferencedData;

  private KVFileErrors fileErrors; // To collect all but surjection errors
  private Optional<KVFileErrors> optionalReferencedFileErrors; // To collect simple surjection errors (complex ones are
                                                               // collected later)

  private SurjectivityValidator surjectivityValidator; // TODO: instantiate here?

  private final Set<KVKeyValues> encounteredKeys = newTreeSet();

  private KVFileDataDigest(
      @NonNull KVFileParser kvFileParser,
      @NonNull KVFileDescription kvFileDescription,
      long logThreshold) {
    this.kvFileParser = kvFileParser;
    this.kvFileDescription = kvFileDescription;
    this.logThreshold = logThreshold;
  }

  public static KVFileDataDigest getEmptyInstance(
      @NonNull KVFileParser kvFileParser,
      @NonNull KVFileDescription kvFileDescription) {
    return new KVFileDataDigest(kvFileParser, kvFileDescription, -1); // -1: no need for a threshold
  }

  public KVFileDataDigest(
      @NonNull KVFileParser kvFileParser,
      @NonNull KVFileDescription kvFileDescription,
      long logThreshold,

      @NonNull Optional<KVFileDataDigest> optionalReferencedData, // Not for DONOR for instance
      @NonNull KVFileErrors fileErrors,
      @NonNull Optional<KVFileErrors> optionalReferencedFileErrors,
      @NonNull SurjectivityValidator surjectivityValidator) {
    this.kvFileParser = kvFileParser;
    this.kvFileDescription = kvFileDescription;
    this.logThreshold = logThreshold;

    this.optionalReferencedData = checkNotNull(optionalReferencedData);
    this.fileErrors = fileErrors;
    this.optionalReferencedFileErrors = optionalReferencedFileErrors;
    this.surjectivityValidator = surjectivityValidator;
  }

  @SneakyThrows
  public KVFileDataDigest processFile() {
    log.info("{}", kvFileDescription);

    checkState(!kvFileDescription.isPlaceholder(), "TODO");
    kvFileParser.parse(kvFileDescription.getDataFilePath().get(), new FileRecordProcessor<List<String>>() {

      @Override
      public void process(long lineNumber, List<String> record) {
        val tuple = getTuple(kvFileDescription.getFileType(), record);
        log.debug("tuple: '{}'", tuple);

        processTuple(tuple, lineNumber);

        if ((lineNumber % logThreshold) == 0) {
          logProcessedLine(lineNumber, false);
        }
      }

    });

    postProcessing();

    return this;
  }

  /**
   * Also validates; TODO: include lineCount in tuple?
   */
  protected void processTuple(KVTuple tuple, long lineCount) {

    // Clinical
    val fileType = kvFileDescription.getFileType();
    if (fileType == DONOR) { // TODO: split per file type (subclass or compose)

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // No foreign key check for DONOR

      updatePksIfApplicable(tuple);
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasFk()); // Hence no surjection
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    } else if (fileType == SPECIMEN) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    } else if (fileType == SAMPLE) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());
      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // SSM
    else if (fileType == SSM_M) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK
        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == SSM_P) {
      ; // No uniqueness check for SSM_P

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // CNSM
    else if (fileType == CNSM_M) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK
        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == CNSM_P) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    } else if (fileType == CNSM_S) {
      ; // No uniqueness check for CNSM

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      ; // No surjection between secondary and primary
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // STSM
    else if (fileType == STSM_M) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK
        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == STSM_P) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    } else if (fileType == STSM_S) {
      ; // No uniqueness check for STSM_s

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      ; // No surjection between secondary and primary
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // MIRNA
    else if (fileType == MIRNA_M) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK
        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == MIRNA_P) {
      ; // No uniqueness check for MIRNA_P (unlike for other types, the PK is on the secondary file for MIRNA)

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    } else if (fileType == MIRNA_S) {

      // Uniqueness check (unlike for other types, the PK is on the secondary file for MIRNA)
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      ; // No surjection between secondary and primary
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // METH
    else if (fileType == METH_M) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK
        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == METH_P) {

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    } else if (fileType == METH_S) {
      ; // No uniqueness check for METH_s

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      ; // No surjection between secondary and primary
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // EXP
    else if (fileType == EXP_M) {
      // TODO: later on, report on diff using: oldData.pksContains(tuple.getPk())

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK

        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == EXP_G) {
      ; // No uniqueness check for EXP_P

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // PEXP
    else if (fileType == PEXP_M) {
      // TODO: later on, report on diff using: oldData.pksContains(tuple.getPk())

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());

      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK
        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());
      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == PEXP_P) {
      ; // No uniqueness check for PEXP_P

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // JCN
    else if (fileType == JCN_M) {
      // TODO: later on, report on diff using: oldData.pksContains(tuple.getPk())

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());
      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK
        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());

      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == JCN_P) {
      ; // No uniqueness check for JCN_P

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());

      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }

    // SGV
    else if (fileType == SGV_M) {
      // TODO: later on, report on diff using: oldData.pksContains(tuple.getPk())

      // Uniqueness check
      if (pks.contains(tuple.getPk())) {
        fileErrors.addError(lineCount, UNIQUENESS, tuple.getPk());
      }

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
      }

      // Secondary foreign key check
      if (tuple.hasSecondaryFk() && !hasMatchingReference(tuple.getSecondaryFk())) { // May not have a secondary FK

        fileErrors.addError(lineCount, SECONDARY_RELATION, tuple.getSecondaryFk());
      }

      updatePksIfApplicable(tuple);
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (tuple.hasSecondaryFk()) { // matched_sample_id may be null or a missing code
        encounteredKeys.add(tuple.getSecondaryFk());
      }
    } else if (fileType == SGV_P) {
      ; // No uniqueness check for SGV_P

      // Foreign key check
      if (!hasMatchingReference(tuple.getFk())) {
        fileErrors.addError(lineCount, PRIMARY_RELATION, tuple.getFk());
      }

      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasPk(), "TODO");
      encounteredKeys.add(checkNotNull(tuple.getFk()));
      if (TUPLE_CHECKS_ENABLED) checkState(!tuple.hasSecondaryFk());
    }
  }

  /**
   * TODO: encode in dictionary data structure rather (hardcoded elsewhere, at least the PKs)
   */
  protected KVTuple getTuple(KVFileType fileType, List<String> row) {
    KVKeyValues pk = null, fk1 = null, fk2 = null;

    // Clinical
    if (fileType == DONOR) {
      pk = from(row, DONOR_PKS);
      fk1 = NOT_APPLICABLE;
      fk2 = NOT_APPLICABLE;
    } else if (fileType == SPECIMEN) {
      pk = from(row, SPECIMEN_PKS);
      fk1 = from(row, SPECIMEN_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == SAMPLE) {
      pk = from(row, SAMPLE_PKS);
      fk1 = from(row, SAMPLE_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Ssm
    else if (fileType == SSM_M) {
      pk = from(row, SSM_M_PKS);
      fk1 = from(row, SSM_M_FKS1);
      fk2 = from(row, SSM_M_FKS2); // TODO: handle case where value is null or a missing code
    } else if (fileType == SSM_P) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, SSM_P_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Cnsm
    else if (fileType == CNSM_M) {
      pk = from(row, CNSM_M_PKS);
      fk1 = from(row, CNSM_M_FKS1);
      fk2 = from(row, CNSM_M_FKS2);
    } else if (fileType == CNSM_P) {
      pk = from(row, CNSM_P_PKS);
      fk1 = from(row, CNSM_P_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == CNSM_S) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, CNSM_S_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Stsm
    else if (fileType == STSM_M) {
      pk = from(row, STSM_M_PKS);
      fk1 = from(row, STSM_M_FKS1);
      fk2 = from(row, STSM_M_FKS2);
    } else if (fileType == STSM_P) {
      pk = from(row, STSM_P_PKS);
      fk1 = from(row, STSM_P_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == STSM_S) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, STSM_S_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Mirna
    else if (fileType == MIRNA_M) {
      pk = from(row, MIRNA_M_PKS);
      fk1 = from(row, MIRNA_M_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == MIRNA_P) {
      pk = NOT_APPLICABLE; // Special case: uniqueness is not enforced
      fk1 = from(row, MIRNA_P_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == MIRNA_S) {
      pk = from(row, MIRNA_S_PKS);
      fk1 = from(row, MIRNA_S_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Meth
    else if (fileType == METH_M) {
      pk = from(row, METH_M_PKS);
      fk1 = from(row, METH_M_FKS1);
      fk1 = from(row, METH_M_FKS2);
    } else if (fileType == METH_P) {
      pk = from(row, METH_P_PKS);
      fk1 = from(row, METH_P_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == METH_S) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, METH_S_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Exp
    else if (fileType == EXP_M) {
      pk = from(row, EXP_M_PKS);
      fk1 = from(row, EXP_M_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == EXP_G) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, EXP_G_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Pexp
    else if (fileType == SSM_M) {
      pk = from(row, PEXP_M_PKS);
      fk1 = from(row, PEXP_M_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == PEXP_P) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, PEXP_P_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Jcn
    else if (fileType == JCN_M) {
      pk = from(row, JCN_M_PKS);
      fk1 = from(row, JCN_M_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == JCN_P) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, JCN_P_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Sgv
    else if (fileType == SGV_M) {
      pk = from(row, SGV_M_PKS);
      fk1 = from(row, SGV_M_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == SGV_P) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, SGV_P_FKS);
      fk2 = NOT_APPLICABLE;
    }

    if (TUPLE_CHECKS_ENABLED) checkState(pk != null || fk1 != null, "TODO: '%s'", row);
    return new KVTuple(pk, fk1, fk2);
  }

  /**
   * For surjection checks.
   */
  protected void postProcessing() {
    // Surjectivity; TODO: externalize?

    val fileType = kvFileDescription.getFileType();
    if (fileType.hasOutgoingSimpleSurjectiveRelation()) {
      log.info("Post-processing: simply surjectivity check");
      checkState(optionalReferencedData.isPresent(), "TODO");
      checkState(optionalReferencedFileErrors.isPresent(), "TODO");
      surjectivityValidator
          .validateSimpleSurjection(
              surjectivityValidator.getSurjectionExpectedKeys(optionalReferencedData.get()),
              encounteredKeys,
              optionalReferencedFileErrors.get());
    }

    if (fileType.hasOutgoingComplexSurjectiveRelation()) {
      log.info("Post-processing: complex surjectivity addition");
      // Simply adding them all for now, actual validation will have to take place after all meta files have been read
      // (unlike for "simple" surjection check).
      surjectivityValidator.addEncounteredSampleKeys(encounteredKeys);
    }
  }

  /**
   * @param fk May be primary or secondary FK.
   */
  private boolean hasMatchingReference(KVKeyValues fk) {
    if (TUPLE_CHECKS_ENABLED) {
      checkState(optionalReferencedData.isPresent(), "TODO");
    }
    return optionalReferencedData.get().pksContains(fk);
  }

  protected void updatePksIfApplicable(KVTuple tuple) {
    if (tuple.hasPk()) {
      pks.add(tuple.getPk());
    } else {
      if (TUPLE_CHECKS_ENABLED) checkState(!kvFileDescription.getFileType().hasPk(), "TODO");
    }
  }

  public boolean pksContains(
      @NonNull// TODO: consider removing such time consuming checks?
      KVKeyValues keys) {
    return pks.contains(keys);
  }

  protected void logProcessedLine(long lineCount, boolean finished) {
    log.info("'{}' lines processed" + (finished ? " (finished)" : ""), lineCount);
  }

  @Override
  public String toString() {
    return toJsonSummaryString();
  }

  @SneakyThrows
  public String toJsonSummaryString() {
    return "\n" + MAPPER
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(this); // TODO: show sample only (first and last 10 for instance) + excluding nulls
  }
}