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
package org.icgc.dcc.submission.validation.pcawg;

import static com.google.common.collect.Sets.difference;
import static org.icgc.dcc.submission.validation.pcawg.util.ClinicalFields.getDonorId;
import static org.icgc.dcc.submission.validation.pcawg.util.ClinicalFields.getSampleSampleId;

import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.model.Programs;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.pcawg.core.ClinicalRuleEngine;
import org.icgc.dcc.submission.validation.pcawg.core.PCAWGDictionary;
import org.icgc.dcc.submission.validation.pcawg.external.PanCancerClient;
import org.icgc.dcc.submission.validation.pcawg.parser.ClinicalParser;
import org.icgc.dcc.submission.validation.pcawg.util.ClinicalIndex;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Validator responsible for ensuring PCAWGFields validation rules are enforced.
 * <p>
 * This class assumes that prior {@code Validator}s have ensured that clinical data exists and that all files are
 * welformed.
 * 
 * @see https://jira.oicr.on.ca/browse/DCC-3012
 */
@Slf4j
@RequiredArgsConstructor
public class PCAWGValidator implements Validator {

  // TODO:
  // 1) Decompose PCAWGValidator into 2 sub validators for Core and Clinical
  // 2) Move remaining control logic into PCAWGValidator
  // 3) Consider externalizing all rules by making generic RuleValidator

  /**
   * Dependencies.
   */
  @NonNull
  private final PanCancerClient pcawgClient;
  @NonNull
  private final PCAWGDictionary pcawgDictionary;

  @Override
  public String getName() {
    return "PCAWG Validator";
  }

  @Override
  public void validate(ValidationContext context) throws InterruptedException {
    log.info("Starting...");

    // Selective validation filtering
    if (!isValidatable(context)) {
      log.info("Validation not required for '{}'. Skipping...", context.getProjectKey());

      return;
    }

    validateClinical(context);
  }

  private void validateClinical(ValidationContext context) {
    // Parse and index raw data
    val clinical = ClinicalParser.parse(context);
    val index = new ClinicalIndex(clinical);

    // Filter excluded donors
    val excludedDonorIds = pcawgDictionary.getExcludedDonorIds(context.getProjectKey());
    val excludedSampleIds = Sets.<String> newHashSet(pcawgDictionary.getExcludedSampleIds(context.getProjectKey()));

    for (val excludedDonorId : excludedDonorIds) {
      log.info("Excluding donor '{}'...", excludedDonorId);

      // Identify donor records
      val donor = index.getDonor(excludedDonorId);
      val donorSpecimens = index.getDonorSpecimen(excludedDonorId);
      val donorSamples = index.getDonorSamples(excludedDonorId);

      for (val donorSample : donorSamples) {
        excludedSampleIds.add(getSampleSampleId(donorSample));
      }

      // Remove core records
      clinical.getCore().getDonors().remove(donor);
      clinical.getCore().getSpecimens().removeAll(donorSpecimens);
      clinical.getCore().getSamples().removeAll(donorSamples);

      // Remove optional records
      for (val records : clinical.getOptional()) {
        records.removeIf(record -> excludedDonorId.equals(getDonorId(record)));
      }
    }

    // Re-index to remove excluded donor information
    val filteredIndex = new ClinicalIndex(clinical);

    // Filter to remove excluded donor sample ids
    val referenceSampleIds = getReferenceSampleIds(context);
    val filteredReferenceSampleIds = difference(referenceSampleIds, excludedSampleIds);

    val rules = pcawgDictionary.getClinicalRules();
    val ruleEngine = new ClinicalRuleEngine(rules, clinical, filteredIndex, filteredReferenceSampleIds, context);

    ruleEngine.execute();
  }

  private Set<String> getReferenceSampleIds(ValidationContext context) {
    val projectKey = context.getProjectKey();
    val projectSamples = pcawgClient.getProjectSampleIds();

    // Get 2 sources of samples
    val externalSampleIds = projectSamples.get(projectKey);
    val whitelistSampleIds = pcawgDictionary.getWhitelistSampleIds(projectKey);

    // Union
    return ImmutableSet.<String> builder().addAll(externalSampleIds).addAll(whitelistSampleIds).build();
  }

  private boolean isValidatable(ValidationContext context) {
    val projectKey = context.getProjectKey();

    if (isProjectExcluded(projectKey)) {
      return false;
    }

    // For DCC testing of PCAWG projects
    val testPCAWG = projectKey.startsWith("TESTP-");

    val pcawg = testPCAWG || isPCAWG(projectKey);
    val tcga = isTCGA(projectKey);

    return pcawg && !tcga;
  }

  private boolean isProjectExcluded(String projectKey) {
    return pcawgDictionary.getExcludedProjectKeys().contains(projectKey);
  }

  private boolean isPCAWG(String projectKey) {
    val projectNames = pcawgClient.getProjects();

    return projectNames.contains(projectKey);
  }

  private static boolean isTCGA(String projectKey) {
    // For DCC testing of PCAWG TCGA projects
    val testTCGA = projectKey.startsWith("TEST") && projectKey.contains("TCGA");

    return testTCGA || Programs.isTCGA(projectKey);
  }

}
