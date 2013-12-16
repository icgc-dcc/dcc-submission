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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newTreeSet;
import static org.icgc.dcc.submission.validation.kv.FileType.CNSM_M;
import static org.icgc.dcc.submission.validation.kv.FileType.CNSM_P;
import static org.icgc.dcc.submission.validation.kv.FileType.CNSM_S;
import static org.icgc.dcc.submission.validation.kv.FileType.DONOR;
import static org.icgc.dcc.submission.validation.kv.FileType.SAMPLE;
import static org.icgc.dcc.submission.validation.kv.FileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.kv.FileType.SSM_M;
import static org.icgc.dcc.submission.validation.kv.FileType.SSM_P;

import java.util.List;
import java.util.Set;

/**
 * 
 */
public class Surjectivity {

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

  void validateSimpleSurjection(FileType fileType, //
      final KeyValidatorData data, // TODO: only pass relevant copy instead
      Set<Keys> surjectionEncountered) {

    Set<Keys> surjectionExpected = getSurjectionExpectedForType(fileType, data);
    if (hasSurjectionErrors(surjectionExpected, surjectionEncountered)) {
      collectSurjectionErrors(
          surjectionExpected,
          surjectionEncountered,
          getSurjectionErrorsForType(fileType, data));
    }
  }

  void validateComplexSurjection(KeyValidatorData data) { // TODO: only pass relevant part instead
    Set<Keys> sampleSurjectionExpected = newTreeSet(
        (Helper.hasNewClinical() ? data.sampleNewDigest : data.sampleOriginalDigest)
            .getPks()); // TODO: defensive copy instead
    if (hasSurjectionErrors(sampleSurjectionExpected, sampleSurjectionEncountered)) {
      collectSurjectionErrors(sampleSurjectionExpected, sampleSurjectionEncountered, data.sampleSurjectivityErrors);
    }
  }

  private boolean hasSurjectionErrors(Set<Keys> surjectionExpected, Set<Keys> surjectionEncountered) {
    return surjectionExpected.size() != surjectionEncountered.size();
  }

  private void collectSurjectionErrors(Set<Keys> surjectionExpected, Set<Keys> surjectionEncountered,
      List<Keys> surjectionErrors) {
    for (Keys keys : surjectionExpected) {
      if (!surjectionEncountered.contains(keys)) {
        surjectionErrors.add(keys);
      }
    }
  }

  private Set<Keys> getSurjectionExpectedForType(FileType fileType, //
      final KeyValidatorData data // TODO: only pass relevant copy instead
  ) {
    Set<Keys> surjectionExpected = null;

    // Clinical
    if (fileType == DONOR) {
      ; // N/A
    } else if (fileType == SPECIMEN) {
      surjectionExpected = newTreeSet(data.donorNewDigest.getPks());
    } else if (fileType == SAMPLE) {
      surjectionExpected = newTreeSet(data.specimenNewDigest.getPks());
    }

    // Ssm
    else if (fileType == SSM_M) {
      ; // Handled differently
    } else if (fileType == SSM_P) {
      surjectionExpected = newTreeSet(data.ssmMNewDigest.getPks());
    }

    // Cnsm
    else if (fileType == CNSM_M) {
      ; // Handled differently
    } else if (fileType == CNSM_P) {
      surjectionExpected = newTreeSet(data.cnsmMNewDigest.getPks());
    } else if (fileType == CNSM_S) {
      ; // N/A
    }
    return checkNotNull(surjectionExpected, "TODO: '%s'", fileType);
  }

  private List<Keys> getSurjectionErrorsForType(FileType fileType, //
      final KeyValidatorData data // TODO: only pass relevant copy instead
  ) {
    List<Keys> surjectionErrors = null;

    // Clinical
    if (fileType == DONOR) {
      ; // N/A
    } else if (fileType == SPECIMEN) {
      surjectionErrors = data.donorSurjectivityErrors;
    } else if (fileType == SAMPLE) {
      surjectionErrors = data.specimenSurjectivityErrors;
    }

    // Ssm
    else if (fileType == SSM_M) {
      ; // Handled elsewhere
    } else if (fileType == SSM_P) {
      surjectionErrors = data.ssmMSurjectivityErrors;
    }

    // Cnsm
    else if (fileType == CNSM_M) {
      ; // Handled elsewhere
    } else if (fileType == CNSM_P) {
      surjectionErrors = data.cnsmMSurjectivityErrors;
    } else if (fileType == CNSM_S) {
      ; // N/A
    } else {
      checkState(false, "TODO");
    }
    return checkNotNull(surjectionErrors, "TODO");
  }
}
