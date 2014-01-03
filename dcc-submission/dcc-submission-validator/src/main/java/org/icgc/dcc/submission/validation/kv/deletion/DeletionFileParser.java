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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newTreeMap;
import static org.icgc.dcc.core.model.FeatureTypes.FeatureType.from;
import static org.icgc.dcc.submission.validation.kv.KVConstants.TAB_SPLITTER;
import static org.icgc.dcc.submission.validation.kv.KVUtils.getDataFilePath;
import static org.icgc.dcc.submission.validation.kv.KVUtils.getToBeRemovedFile;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVSubmissionType.EXISTING_FILE;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVSubmissionType.INCREMENTAL_FILE;
import static org.icgc.dcc.submission.validation.kv.enumeration.KeyValidationAdditionalType.ALL;
import static org.icgc.dcc.submission.validation.kv.enumeration.KeyValidationAdditionalType.ERROR;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.DeletionType;
import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.submission.validation.kv.KVConstants;
import org.icgc.dcc.submission.validation.kv.enumeration.KeyValidationAdditionalType;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

/**
 * Utils class to parse the deletion file (which is assumed to be small in comparison with real data).
 * <p>
 * TODO: consider having the validation be separated from the key validator?
 */
@Slf4j
public class DeletionFileParser {

  private static final Splitter FEATURE_TYPE_SPLITTER = Splitter.on(',');

  /**
   * Key-value pair format is expected to have been checked for already in the first-pass validation.
   * <p>
   * Does not perform any validation per se, simply parsing.
   */
  @SneakyThrows
  public static DeletionData parseToBeDeletedFile() {
    val toBeDetetedFile = getToBeRemovedFile();
    log.info("{}", toBeDetetedFile);

    // TODO: use builder
    Map<String, List<DeletionType>> deletionMap = newTreeMap();

    // TODO: "with" construct
    @Cleanup
    val reader = new BufferedReader(new FileReader(new File(toBeDetetedFile)));
    long lineCount = 0;
    for (String line; (line = reader.readLine()) != null;) {
      if (lineCount != 0 && !line.trim().isEmpty()) {
        val row = newArrayList(KVConstants.TAB_SPLITTER.split(line));
        log.debug("\t" + row);

        checkState(row.size() == 2, "TODO");
        String donorId = row.get(0);
        String featureTypesString = row.get(1);

        deletionMap.put(
            donorId,
            getDeletionType(
            getFeatureTypeStringList(featureTypesString)));
      }
      lineCount++;
    }
    return new DeletionData(deletionMap);
  }

  // TODO: use abstraction rather
  @SneakyThrows
  private static Set<String> getDonorIds(String donorFile) {
    val donorIds = Sets.<String> newTreeSet();
    @Cleanup
    val reader = new BufferedReader(new FileReader(new File(donorFile)));
    long lineCount = 0;
    for (String line; (line = reader.readLine()) != null;) {
      if (lineCount != 0 && !line.trim().isEmpty()) {
        val row = newArrayList(TAB_SPLITTER.split(line));
        log.debug("\t" + row);
        String donorId = row.get(0);
        donorIds.add(donorId);
      }
      lineCount++;
    }
    return donorIds;
  }

  public static Set<String> getExistingDonorIds() {
    val existingDonorFile = getDataFilePath(EXISTING_FILE, DONOR);
    log.info("{}", existingDonorFile);
    return getDonorIds(existingDonorFile);
  }

  public static Set<String> getIncrementalDonorIds() {
    val incrementalDonorFile = getDataFilePath(INCREMENTAL_FILE, DONOR);
    log.info("{}", incrementalDonorFile);
    return getDonorIds(incrementalDonorFile);
  }

  private static List<String> getFeatureTypeStringList(String featureTypesString) {
    return newArrayList(FEATURE_TYPE_SPLITTER.split(
        featureTypesString
            .toLowerCase()
            .replace(" ", "")));
  }

  private static List<DeletionType> getDeletionType(List<String> featureTypeStringList) {
    List<DeletionType> deletionTypes = newArrayList();
    for (val featureTypeString : featureTypeStringList) {
      boolean isAll = KeyValidationAdditionalType.matchesAllDeletionType(featureTypeString);
      if (!isAll) {
        if (FeatureType.hasMatch(featureTypeString)) {
          deletionTypes.add(from(featureTypeString));
        } else {
          deletionTypes.add(ERROR);
        }
      } else {
        deletionTypes.add(ALL);
      }
    }
    return deletionTypes;
  }
}
