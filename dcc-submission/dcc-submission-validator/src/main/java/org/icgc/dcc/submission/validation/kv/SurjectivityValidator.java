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
package org.icgc.dcc.submission.validation.kv;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.newTreeSet;
import static org.icgc.dcc.submission.validation.kv.Helper.hasNewClinicalData;

import java.util.Set;

import lombok.val;

import org.icgc.dcc.submission.validation.kv.error.KVFileErrors;

/**
 * 
 */
public class SurjectivityValidator {

  /**
   * TODO: explain very special case
   */
  Set<Keys> sampleSurjectionEncountered = newTreeSet();

  /**
   * @param surjectionEncountered
   */
  void addEncounteredSamples(Set<Keys> surjectionEncountered) {
    sampleSurjectionEncountered.addAll(surjectionEncountered);
  }

  void validateSimpleSurjection(KVFileType fileType,
      KVFileDataDigest originalData, KVFileDataDigest newData, KVFileErrors surjectionFileErrors,
      Set<Keys> surjectionEncountered) {
    val dataDigest = !fileType.isDodo() || hasNewClinicalData() ? newData : originalData;
    val expectedSujectionKeys = newTreeSet(checkNotNull(dataDigest, "TODO: '%s'", fileType).getPks());
    if (hasSurjectionErrors(expectedSujectionKeys, surjectionEncountered)) {
      collectSurjectionErrors(
          expectedSujectionKeys,
          surjectionEncountered,
          surjectionFileErrors);
    }
  }

  void validateComplexSurjection(
      KVFileDataDigest sampleOriginalData, KVFileDataDigest sampleNewData,
      KVFileErrors surjectionSampleFileErrors) {
    val sampleDataDigest = hasNewClinicalData() ? sampleNewData : sampleOriginalData;
    val expectedSampleSujectionKeys = newTreeSet(checkNotNull(sampleDataDigest, "TODO: '%s'").getPks());
    if (hasSurjectionErrors(expectedSampleSujectionKeys, sampleSurjectionEncountered)) {
      collectSurjectionErrors(
          expectedSampleSujectionKeys,
          sampleSurjectionEncountered,
          surjectionSampleFileErrors);
    }
  }

  private boolean hasSurjectionErrors(Set<Keys> surjectionExpected, Set<Keys> surjectionEncountered) {
    return surjectionExpected.size() != surjectionEncountered.size();
  }

  private void collectSurjectionErrors(
      Set<Keys> surjectionExpected, Set<Keys> surjectionEncountered,
      KVFileErrors fileError) {
    for (Keys keys : surjectionExpected) {
      if (!surjectionEncountered.contains(keys)) {
        fileError.addSurjectionError(keys);
      }
    }
  }

}
