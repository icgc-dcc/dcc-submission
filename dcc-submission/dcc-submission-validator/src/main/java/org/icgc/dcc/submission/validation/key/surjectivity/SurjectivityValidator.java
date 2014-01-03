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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newTreeSet;
import static org.icgc.dcc.submission.validation.key.KVUtils.hasIncrementalClinicalData;

import java.util.Set;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.data.KVFileDataDigest;
import org.icgc.dcc.submission.validation.key.data.KVKeyValues;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.key.error.KVFileErrors;

/**
 * Validates surjective relations.
 */
@Slf4j
public class SurjectivityValidator {

  public static final long SURJECTION_ERROR_LINE_NUMBER = -1;

  /**
   * TODO: explain very special case
   */
  private final Set<KVKeyValues> sampleSurjectionEncountered = newTreeSet();

  public void addEncounteredSamples(Set<KVKeyValues> surjectionEncountered) {
    sampleSurjectionEncountered.addAll(surjectionEncountered);
  }

  public void validateSimpleSurjection(
      KVFileType fileType,
      KVFileDataDigest dataDigest,
      KVFileErrors surjectionFileErrors,
      Set<KVKeyValues> surjectionEncountered) {
    val expectedSujectionKeys = newTreeSet(checkNotNull(dataDigest, "TODO: '%s'", fileType).getPks());
    if (hasSurjectionErrors(expectedSujectionKeys, surjectionEncountered)) {
      collectSurjectionErrors(
          expectedSujectionKeys,
          surjectionEncountered,
          surjectionFileErrors);
    }
  }

  public void validateComplexSurjection(
      KVFileDataDigest sampleExistingData,
      KVFileDataDigest sampleNewData,
      KVFileErrors surjectionSampleFileErrors) {
    val sampleDataDigest = hasIncrementalClinicalData() ? sampleNewData : sampleExistingData;
    val expectedSampleSujectionKeys = newTreeSet(checkNotNull(sampleDataDigest, "TODO: '%s'").getPks()); // FIXME
    if (hasSurjectionErrors(expectedSampleSujectionKeys, sampleSurjectionEncountered)) {
      log.error("Some complex surjection errors detected");
      collectSurjectionErrors(
          expectedSampleSujectionKeys,
          sampleSurjectionEncountered,
          surjectionSampleFileErrors);
    } else {
      log.error("No complex surjection errors detected");
    }
  }

  private void collectSurjectionErrors(
      Set<KVKeyValues> surjectionExpected,
      Set<KVKeyValues> surjectionEncountered,
      KVFileErrors fileError) {
    for (KVKeyValues keys : surjectionExpected) {
      if (!surjectionEncountered.contains(keys)) {
        fileError.addSurjectionError(keys);
      }
    }
  }

  private boolean hasSurjectionErrors(Set<KVKeyValues> surjectionExpected, Set<KVKeyValues> surjectionEncountered) {
    return surjectionExpected.size() != surjectionEncountered.size();
  }
}
