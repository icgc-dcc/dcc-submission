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
package org.icgc.dcc.submission.validation.key.surjectivity;

import static org.icgc.dcc.submission.validation.key.data.KVKeyValuesWrapper.sameSize;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.COMPLEX_SURJECTION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.SIMPLE_SURJECTION;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.core.KVFileSystem;
import org.icgc.dcc.submission.validation.key.data.KVEncounteredForeignKeys;
import org.icgc.dcc.submission.validation.key.data.KVKeys;
import org.icgc.dcc.submission.validation.key.data.KVPrimaryKeys;
import org.icgc.dcc.submission.validation.key.error.KVFileErrors;

/**
 * Validates surjective relations.
 */
@RequiredArgsConstructor
@Slf4j
public class SurjectivityValidator {

  public static final long SIMPLE_SURJECTION_ERROR_LINE_NUMBER = -1;
  public static final long COMPLEX_SURJECTION_ERROR_LINE_NUMBER = -2;

  @NonNull
  private final KVFileSystem fileSystem;

  /**
   * TODO: explain very special case.
   * <p>
   * From potentially more than one file (hence "complex").
   */
  private final KVEncounteredForeignKeys encounteredSampleForeignKeys = new KVEncounteredForeignKeys();

  public void addEncounteredSampleKeys(KVEncounteredForeignKeys encounteredSampleForeignKeys) {
    this.encounteredSampleForeignKeys.addEncounteredForeignKeys(encounteredSampleForeignKeys);
  }

  public void validateSimpleSurjection(
      KVPrimaryKeys expectedKeys,
      KVEncounteredForeignKeys encounteredKeys, // From one file only (unlike for complex check)
      KVFileErrors referencedFileErrors) {
    if (hasSurjectionErrors(expectedKeys, encounteredKeys)) {
      collectSurjectionErrors(
          false,
          expectedKeys,
          encounteredKeys,
          referencedFileErrors);
    }
  }

  public void validateComplexSurjection(
      KVPrimaryKeys expectedSampleKeys,
      KVFileErrors sampleFileErrors) {

    if (encounteredSampleForeignKeys.noneEncountered()) {
      log.warn("No sample encountered..."); // Could indicate an issue
    }
    if (hasSurjectionErrors(expectedSampleKeys, encounteredSampleForeignKeys)) {
      log.error("Some complex surjection errors detected");
      collectSurjectionErrors(
          true,
          expectedSampleKeys,
          encounteredSampleForeignKeys,
          sampleFileErrors);
    } else {
      log.error("No complex surjection errors detected");
    }
  }

  private boolean hasSurjectionErrors(
      KVPrimaryKeys surjectionExpected,
      KVEncounteredForeignKeys surjectionEncountered) {
    return !sameSize(surjectionExpected, surjectionEncountered);
  }

  private void collectSurjectionErrors(
      boolean complex,
      KVPrimaryKeys expectedKeys,
      KVEncounteredForeignKeys encounteredKeys,
      KVFileErrors referencedFileError) {
    log.info("Collecting '{}' surjectivity errors", complex ? COMPLEX_SURJECTION : SIMPLE_SURJECTION);
    for (KVKeys expected : expectedKeys) {
      if (!encounteredKeys.encountered(expected)) {
        if (complex) {
          referencedFileError.addComplexSurjectionError(expected);
        } else {
          referencedFileError.addSimpleSurjectionError(expected);
        }
      }
    }
  }
}
