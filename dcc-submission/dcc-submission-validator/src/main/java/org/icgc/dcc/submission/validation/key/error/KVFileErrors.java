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
package org.icgc.dcc.submission.validation.key.error;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newTreeMap;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.RELATIONS;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.COMPLEX_SURJECTION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.PRIMARY_RELATION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.SECONDARY_RELATION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.SIMPLE_SURJECTION;
import static org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator.COMPLEX_SURJECTION_ERROR_LINE_NUMBER;
import static org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator.SIMPLE_SURJECTION_ERROR_LINE_NUMBER;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.core.KVConstants;
import org.icgc.dcc.submission.validation.key.core.KVFileDescription;
import org.icgc.dcc.submission.validation.key.data.KVKeyValues;
import org.icgc.dcc.submission.validation.key.enumeration.KVErrorType;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.key.enumeration.KeysType;
import org.icgc.dcc.submission.validation.key.report.KVReport;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

/**
 * 
 */
@Slf4j
public class KVFileErrors {

  private final KVFileType fileType;
  private final Map<KVErrorType, List<String>> fieldNamesPerErrorType;
  private final Map<Long, List<KVRowError>> lineToRowErrors = newTreeMap();

  // TODO: factory instead of constructor
  public KVFileErrors(
      @NonNull KVFileType fileType,
      List<String> pkNames,
      List<String> fkNames,
      List<String> secondaryFkNames) {
    this.fileType = fileType;
    checkState(pkNames != null || fkNames != null || secondaryFkNames != null, "TODO");
    fieldNamesPerErrorType = getFieldNamesPerErrorType(pkNames, fkNames, secondaryFkNames);
    checkState(!fieldNamesPerErrorType.isEmpty(), "TODO");
    log.info("fieldNamesPerErrorType: '{}'", fieldNamesPerErrorType);
  }

  private final Map<KVErrorType, List<String>> getFieldNamesPerErrorType(
      List<String> pkNames, List<String> fkNames, List<String> secondaryFkNames) {
    val builder = new ImmutableMap.Builder<KVErrorType, List<String>>();
    for (val errorType : KVErrorType.values()) {
      val keysType = errorType.getKeysType();
      val optionalNames = getOptionalNames(keysType,
          pkNames, fkNames, secondaryFkNames);
      log.info("keysType, optionalIndices: {}, '({}, {})'", new Object[] { fileType, keysType, optionalNames });
      if (optionalNames.isPresent()) {
        builder.put(errorType, optionalNames.get());
      }
    }
    return builder.build();
  }

  private final Optional<List<String>> getOptionalNames(KeysType keysType,
      List<String> pkNames, List<String> fkNames, List<String> secondaryFkNames) {
    List<String> names = null;
    switch (keysType) {
    case PK:
      names = pkNames == null ? null : newArrayList(pkNames);
      break;
    case FK:
      names = fkNames == null ? null : newArrayList(fkNames);
      break;
    case SECONDARY_FK:
      names = secondaryFkNames == null ? null : newArrayList(secondaryFkNames);
      break;
    default:
      checkState(false, "%s", keysType);
    }
    return names == null ? Optional.<List<String>> absent() : Optional.of(names);
  }

  public boolean hasError(long lineNumber) {
    return lineToRowErrors.containsKey(lineNumber);
  }

  /**
   * TODO: create other wrappers like the surjection one
   */
  public void addError(long lineNumber, KVErrorType type, KVKeyValues keys) {
    List<KVRowError> rowErrors = lineToRowErrors.get(lineNumber);
    if (rowErrors == null) {
      rowErrors = newArrayList();
      lineToRowErrors.put(lineNumber, rowErrors);
    }
    rowErrors.add(new KVRowError(type, keys));
  }

  public void addSimpleSurjectionError(KVKeyValues keys) {
    addError(SIMPLE_SURJECTION_ERROR_LINE_NUMBER, SIMPLE_SURJECTION, keys);
  }

  public void addComplexSurjectionError(KVKeyValues keys) {
    addError(COMPLEX_SURJECTION_ERROR_LINE_NUMBER, COMPLEX_SURJECTION, keys);
  }

  public boolean describe(KVReport report, KVFileDescription kvFileDescription) { // TODO: remove full description?
    if (lineToRowErrors.isEmpty()) {
      return true;
    } else {
      for (val entry : lineToRowErrors.entrySet()) {
        val lineNumber = entry.getKey();
        val rowErrors = entry.getValue();
        for (val rowError : rowErrors) {
          val errorType = rowError.getType();
          val fieldNames = checkNotNull(
              fieldNamesPerErrorType
                  .get(rowError.getType()),
              "TODO: %s, %s, %s, %s", fileType, fieldNamesPerErrorType, errorType, kvFileDescription);
          val dataFileName = kvFileDescription.getDataFileName();
          rowError.describe(report, dataFileName, lineNumber, fieldNames, getErrorParams(errorType), kvFileDescription);
        }
      }
      return false;
    }
  }

  private Object[] getErrorParams(KVErrorType errorType) {
    Object[] errorParams = null;
    if (errorType == PRIMARY_RELATION || errorType == SECONDARY_RELATION) {
      val referencedFileType = RELATIONS.get(fileType);
      val referencedFields = KVConstants.PKS.get(referencedFileType);
      errorParams = new Object[] { referencedFileType, referencedFields };
    } else if (errorType == SIMPLE_SURJECTION) {
      val referencingFileType = getReferencingFileType(fileType);
      val referencingFields = KVConstants.PKS.get(referencingFileType);
      errorParams = new Object[] { referencingFileType, referencingFields };
    }
    return errorParams;
  }

  /**
   * Should NOT be called in the context of complex surjection.
   */
  private KVFileType getReferencingFileType(KVFileType fileType) {
    KVFileType referencingFileType = null;
    for (val entry : RELATIONS.entrySet()) {
      if (entry.getValue() == fileType) {
        checkState(referencingFileType == null, "TODO");
        return entry.getKey();
      }
    }
    return checkNotNull(referencingFileType, "TODO");
  }

}
