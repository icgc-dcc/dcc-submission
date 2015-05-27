/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.pcawg.core;

import static org.icgc.dcc.common.core.util.Jackson.DEFAULT;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

@RequiredArgsConstructor
public class PCAWGDictionary {

  /**
   * Constants.
   */
  public static final URL DEFAULT_PCAWG_DICTIONARY_URL = Resources.getResource("pcawg-dictionary.json");

  @NonNull
  private final URL url;

  public PCAWGDictionary() {
    this(DEFAULT_PCAWG_DICTIONARY_URL);
  }

  public Set<String> getExcludedProjectKeys() {
    val excludedProjectKeys = readField("excludedProjectKeys");
    if (excludedProjectKeys.isMissingNode()) {
      return ImmutableSet.of();
    }

    return DEFAULT.convertValue(excludedProjectKeys, new TypeReference<Set<String>>() {});
  }

  public Set<String> getExcludedDonorIds(@NonNull String projectKey) {
    val excludedDonorIds = readField("excludedDonorIds");
    if (excludedDonorIds.isMissingNode()) {
      return ImmutableSet.of();
    }

    Map<String, Set<String>> map =
        DEFAULT.convertValue(excludedDonorIds, new TypeReference<Map<String, Set<String>>>() {});
    val donorIds = map.get(projectKey);
    if (donorIds == null) {
      return ImmutableSet.of();
    }

    return donorIds;
  }

  public Set<String> getExcludedSampleIds(@NonNull String projectKey) {
    val excludedSampleIds = readField("excludedSampleIds");
    if (excludedSampleIds.isMissingNode()) {
      return ImmutableSet.of();
    }

    Map<String, Set<String>> map =
        DEFAULT.convertValue(excludedSampleIds, new TypeReference<Map<String, Set<String>>>() {});
    val donorIds = map.get(projectKey);
    if (donorIds == null) {
      return ImmutableSet.of();
    }

    return donorIds;
  }

  public Set<String> getWhitelistSampleIds(@NonNull String projectKey) {
    val samples = readField("samples");
    if (samples.isMissingNode()) {
      return ImmutableSet.of();
    }

    Map<String, Set<String>> map = DEFAULT.convertValue(samples, new TypeReference<Map<String, Set<String>>>() {});
    val sampleIds = map.get(projectKey);
    if (sampleIds == null) {
      return ImmutableSet.of();
    }

    return sampleIds;
  }

  public List<ClinicalRule> getClinicalRules() {
    val rules = readField("rules");
    if (rules.isMissingNode()) {
      ImmutableList.of();
    }

    return DEFAULT.convertValue(rules, new TypeReference<List<ClinicalRule>>() {});
  }

  private JsonNode readField(String fieldName) {
    val dictionary = readDictionary();
    return dictionary.path(fieldName);
  }

  @SneakyThrows
  private JsonNode readDictionary() {
    return DEFAULT.readTree(url);
  }

}
