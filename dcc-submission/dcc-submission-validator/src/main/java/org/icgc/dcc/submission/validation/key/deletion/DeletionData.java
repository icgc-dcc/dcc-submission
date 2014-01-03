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
package org.icgc.dcc.submission.validation.key.deletion;

import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.intersection;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newTreeSet;
import static com.google.common.collect.Sets.union;
import static org.icgc.dcc.submission.validation.key.KVUtils.TO_BE_REMOVED_FILE_NAME;
import static org.icgc.dcc.submission.validation.key.KVUtils.hasToBeRemovedFile;
import static org.icgc.dcc.submission.validation.key.enumeration.KeyValidationAdditionalType.ERROR;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.DeletionType;
import org.icgc.dcc.submission.validation.key.enumeration.KeyValidationAdditionalType;

import com.google.common.collect.Maps;

/**
 * Holds the map of donor ID to list of feature types to be deleted + performs validations on it.
 */
@Value
@Slf4j
public class DeletionData {

  private final Map<String, List<DeletionType>> deletionMap;

  public Set<String> getDonorIdKeys() {
    return deletionMap.keySet();
  }

  // TODO: more getters

  /**
   * 
   */
  public static DeletionData getInstance() {
    DeletionData deletionData;
    if (hasToBeRemovedFile()) {
      deletionData = DeletionFileParser.parseToBeDeletedFile();
    } else {
      deletionData = DeletionData.getEmptyInstance();
      log.info("No '{}' file provided", TO_BE_REMOVED_FILE_NAME);
    }
    log.info("{}", deletionData);
    return deletionData;
  }

  private static DeletionData getEmptyInstance() {
    return new DeletionData(Maps.<String, List<DeletionType>> newTreeMap());
  }

  /**
   * Only log.errors them now
   * <p>
   * Assumed to be well-formed (TSV, encoding, number of columns, ...)
   * <p>
   * TODO: consider validating while parsing to save time? Could this file get big?
   * <p>
   * TODO: pass line number as well
   */
  public boolean validateWellFormedness() {
    Set<String> encountered = newTreeSet();
    for (val entry : deletionMap.entrySet()) {
      val donorId = entry.getKey();
      val featureTypes = entry.getValue();

      if (encountered.contains(donorId)) {
        log.error("collision: '{}'", donorId); // TODO
        return false;
      } else {
        encountered.add(donorId);
      }

      boolean duplicateFeatureTypes = featureTypes.size() != newHashSet(featureTypes).size();
      if (duplicateFeatureTypes) {
        log.error("duplicate feature types: '{}'", featureTypes);
        return false;
      }

      if (featureTypes.contains(ERROR)) {
        log.error("invalid feature types: '{}'", featureTypes); // TODO: must be able to pass invalid onereturn false;
        return false;
      } else if (!isAllDeletionAlone(featureTypes) && featureTypes.contains(KeyValidationAdditionalType.ALL)) {
        log.error("invalid feature type set: '{}'", featureTypes);
        return false;
      }
    }
    return true;
  }

  /**
   * TODO: add tests
   */
  public boolean validateAgainstOldClinicalData(Set<String> donorOriginalPks) {
    val donorsToBeDeleted = getDonorIdKeys();

    // Checks if there are donors marked as to-be-deleted but that do not exist in the original data
    val difference = difference(donorsToBeDeleted, donorOriginalPks);
    if (!difference.isEmpty()) {
      log.error("'{}'", difference);
      return false;
    }

    return true;
  }

  /**
   * TODO: add tests
   */
  public boolean validateAgainstIncrementalClinicalData(Set<String> donorOriginalPks, Set<String> donorNewPks) {
    val donorsToBeDeleted = getDonorIdKeys();

    // Check if there are donors that are both included in the new data and marked as to-be-deleted
    val intersection = intersection(donorNewPks, donorsToBeDeleted);
    if (!intersection.isEmpty()) {
      log.error("intersection error: {}'", intersection);
      return false;
    }

    // Check if there are donors formerly in the data but not included in the new data yet not marked as to-be-deleted
    val union = union(donorNewPks, donorsToBeDeleted);
    val difference = difference(donorOriginalPks, union);
    if (!difference.isEmpty()) {
      log.error("difference error: '{}' ({})", difference, union);
      return false;
    }

    return true;
  }

  private boolean isAllDeletionAlone(List<DeletionType> featureTypes) {
    return featureTypes.size() == 1 && featureTypes.get(0).isAllDeletionType();
  }
}
