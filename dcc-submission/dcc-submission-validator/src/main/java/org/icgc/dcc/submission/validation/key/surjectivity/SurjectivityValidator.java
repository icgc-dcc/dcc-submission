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

import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.COMPLEX_SURJECTION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVErrorType.SIMPLE_SURJECTION;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SAMPLE;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.data.KVEncounteredForeignKeys;
import org.icgc.dcc.submission.validation.key.data.KVPrimaryKeys;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.key.error.KVSubmissionErrors;

/**
 * Validates surjective relations.
 */
@RequiredArgsConstructor
@Slf4j
public class SurjectivityValidator {

  public static final long SIMPLE_SURJECTION_ERROR_LINE_NUMBER = -1;
  public static final long COMPLEX_SURJECTION_ERROR_LINE_NUMBER = -2;

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
      KVSubmissionErrors errors,
      KVFileType offendedFileType) {
    if (hasSurjectionErrors(expectedKeys, encounteredKeys)) {
      collectSurjectionErrors(
          false,
          expectedKeys,
          encounteredKeys,
          errors,
          offendedFileType);
    }
  }

  public void validateComplexSurjection(KVPrimaryKeys expectedSampleKeys, KVSubmissionErrors errors) {
    if (encounteredSampleForeignKeys.noneEncountered()) {
      log.warn("No sample encountered..."); // Could indicate an issue
    }
    if (hasSurjectionErrors(expectedSampleKeys, encounteredSampleForeignKeys)) {
      log.error("Some complex surjection errors detected");
      collectSurjectionErrors(
          true,
          expectedSampleKeys,
          encounteredSampleForeignKeys,
          errors,
          SAMPLE);
    } else {
      log.error("No complex surjection errors detected");
    }
  }

  private void collectSurjectionErrors(
      boolean complex,
      KVPrimaryKeys expectedKeys,
      KVEncounteredForeignKeys encounteredKeys,
      KVSubmissionErrors errors,
      KVFileType offendedFileType) {
    log.info("Collecting '{}' surjectivity errors", complex ? COMPLEX_SURJECTION : SIMPLE_SURJECTION);
    for (val fileName : expectedKeys.getFilePaths()) {
      val expectedIterator = expectedKeys.getPrimaryKeys(fileName);
      while (expectedIterator.hasNext()) {
        val expected = expectedIterator.next();
        if (encounteredKeys.encountered(expected)) {
          if (complex) {
            errors.addComplexSurjectionError(offendedFileType, fileName, expected);
          } else {
            errors.addSimpleSurjectionError(offendedFileType, fileName, expected);
          }
        }

      }
    }
  }

  private boolean hasSurjectionErrors(
      KVPrimaryKeys surjectionExpected,
      KVEncounteredForeignKeys surjectionEncountered) {
    return surjectionExpected.getSize() != surjectionEncountered.getSize();
  }
}
