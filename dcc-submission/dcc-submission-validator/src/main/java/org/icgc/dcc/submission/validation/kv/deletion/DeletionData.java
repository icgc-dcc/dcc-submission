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
package org.icgc.dcc.submission.validation.kv.deletion;

import static com.google.common.base.Optional.absent;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.DeletionType;
import org.icgc.dcc.submission.validation.kv.KeyValidatorData;
import org.icgc.dcc.submission.validation.kv.deletion.Deletion.KeyValidationAdditionalType;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * 
 */
@Value
@Slf4j
public class DeletionData {

  private final Map<String, List<DeletionType>> deletionMap;

  public static DeletionData getEmptyInstance() {
    return new DeletionData(Maps.<String, List<DeletionType>> newTreeMap());
  }

  public Set<String> getDonorIds() {
    return deletionMap.keySet();
  }

  /**
   * Only log.errors them now
   * <p>
   * TODO: consider validating while parsing to save time? Could this file get big?
   * <p>
   * TODO: pass line number as well
   */
  public Optional<DeletionError> validate() {
    Set<String> encountered = newTreeSet();
    for (val entry : deletionMap.entrySet()) {
      val donorId = entry.getKey();
      val featureTypes = entry.getValue();

      if (encountered.contains(donorId)) {
        log.error("collision: '{}'", donorId); // TODO
      } else {
        encountered.add(donorId);
      }

      boolean duplicateFeatureTypes = featureTypes.size() != newHashSet(featureTypes).size();
      if (duplicateFeatureTypes) {
        log.error("duplicate feature types: '{}'", featureTypes);
      }

      if (featureTypes.contains(KeyValidationAdditionalType.ERROR)) {
        log.error("invalid feature types: '{}'", featureTypes); // TODO: must be able to pass invalid one
      } else if (!isAllDeletionAlone(featureTypes) && featureTypes.contains(KeyValidationAdditionalType.ALL)) {
        log.error("invalid feature type set: '{}'", featureTypes);
      }
    }
    return absent();
  }

  private boolean isAllDeletionAlone(List<DeletionType> featureTypes) {
    return featureTypes.size() == 1 && featureTypes.get(0).isAllDeletionType();
  }

  /**
   * TODO: add tests
   */
  public boolean validateClinicalDataDeletion1(KeyValidatorData data) { // TODO: PLK
    val donorOriginalPks = data.getDonorOriginalDigest().getPks();
    val donorsToBeDeleted = getDonorIds();

    // Checks if there are donors marked as to-be-deleted but that do not exist in the original data
    val difference2 = Sets.difference(donorsToBeDeleted, donorOriginalPks);
    if (!difference2.isEmpty()) {
      log.error("'{}'", difference2);
      return false;
    }

    return true;
  }

  /**
   * TODO: add tests
   */
  public boolean validateClinicalDataDeletion2(KeyValidatorData data) { // TODO: PLK
    val donorOriginalPks = data.getDonorOriginalDigest().getPks();
    val donorNewPks = data.getDonorNewDigest().getPks();
    val donorsToBeDeleted = getDonorIds();

    // Check if there are donors that are both included in the new data and marked as to-be-deleted
    val intersection = Sets.intersection(donorNewPks, donorsToBeDeleted);
    if (!intersection.isEmpty()) {
      log.error("'{}'", intersection);
      return false;
    }

    val union = Sets.union(donorNewPks, donorsToBeDeleted);
    val difference1 = Sets.difference(donorOriginalPks, union);

    // Check if there are donors formerly in the data but not included in the new data yet not marked as to-be-deleted
    if (!difference1.isEmpty()) {
      log.error("'{}'", difference1);
      return false;
    }

    return true;
  }
}
