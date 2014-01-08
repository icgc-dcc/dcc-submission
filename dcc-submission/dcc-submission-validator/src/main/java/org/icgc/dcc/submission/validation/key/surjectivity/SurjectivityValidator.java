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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newTreeSet;

import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.core.KVFileSystem;
import org.icgc.dcc.submission.validation.key.data.KVFileDataDigest;
import org.icgc.dcc.submission.validation.key.data.KVKeyValues;
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
  private final Set<KVKeyValues> encounteredSampleKeys = newTreeSet();

  public Set<KVKeyValues> getSurjectionExpectedKeys(@NonNull KVFileDataDigest referencedData) {
    return newTreeSet(referencedData.getPks());
  }

  public void addEncounteredSampleKeys(Set<KVKeyValues> surjectionEncountered) {
    encounteredSampleKeys.addAll(surjectionEncountered);
  }

  public void validateSimpleSurjection(
      Set<KVKeyValues> expectedKeys,
      Set<KVKeyValues> encounteredKeys, // From one file only (unlike for complex check)
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
      Set<KVKeyValues> expectedSampleKeys,
      KVFileErrors sampleFileErrors) {

    if (encounteredSampleKeys.isEmpty()) {
      log.warn("No sample encountered..."); // Could indicate an issue
    }
    if (hasSurjectionErrors(expectedSampleKeys, encounteredSampleKeys)) {
      log.error("Some complex surjection errors detected");
      collectSurjectionErrors(
          true,
          expectedSampleKeys,
          encounteredSampleKeys,
          sampleFileErrors);
    } else {
      log.error("No complex surjection errors detected");
    }
  }

  private boolean hasSurjectionErrors(Set<KVKeyValues> surjectionExpected, Set<KVKeyValues> surjectionEncountered) {
    int expectedSize = surjectionExpected.size();
    int encounteredSize = surjectionEncountered.size();
    checkState(encounteredSize <= expectedSize, "TODO");
    return expectedSize != encounteredSize;
  }

  private void collectSurjectionErrors(
      boolean complex,
      Set<KVKeyValues> expectedKeys,
      Set<KVKeyValues> encounteredKeys,
      KVFileErrors referencedFileError) {
    for (KVKeyValues keys : expectedKeys) {
      if (!encounteredKeys.contains(keys)) {
        if (complex) {
          referencedFileError.addComplexSurjectionError(keys);
        } else {
          referencedFileError.addSimpleSurjectionError(keys);
        }
      }
    }
  }
}
