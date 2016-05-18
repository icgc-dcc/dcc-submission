/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
import static com.google.common.collect.Sets.newHashSet;
import static org.icgc.dcc.submission.validation.core.ClinicalFields.getSampleSampleId;

import java.util.Set;

import org.icgc.dcc.common.core.model.Programs;
import org.icgc.dcc.submission.validation.core.Clinical;
import org.icgc.dcc.submission.validation.core.ClinicalIndex;
import org.icgc.dcc.submission.validation.core.ClinicalParser;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.pcawg.core.PCAWGClinicalValidator;
import org.icgc.dcc.submission.validation.pcawg.core.PCAWGDictionary;
import org.icgc.dcc.submission.validation.pcawg.core.PCAWGSampleSheet;

import com.google.common.collect.Sets;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Validator responsible for ensuring PCAWG clinical validation rules are enforced.
 * <p>
 * This class assumes that prior {@code Validator}s have ensured that clinical data exists and that all files are
 * welformed.
 * 
 * @see https://jira.oicr.on.ca/browse/DCC-3012
 */
@Slf4j
@RequiredArgsConstructor
public class PCAWGValidator implements Validator {

  /**
   * Dependencies.
   */
  @NonNull
  private final PCAWGDictionary pcawgDictionary;
  @NonNull
  private final PCAWGSampleSheet pcawgSampleSheet;

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
    // Parse raw clinical data
    val clinical = ClinicalParser.parse(context);

    // Filter clinical entities returning the excluded sample ids
    val excludedSampleIds = filterClinical(context, clinical);

    // Filter to remove excluded donor sample ids
    val filteredReferenceSampleIds = filterSampleIds(excludedSampleIds, context.getProjectKey());

    val clinicalValidator = new PCAWGClinicalValidator(clinical, filteredReferenceSampleIds, context);
    clinicalValidator.execute();
  }

  private Set<String> filterClinical(ValidationContext context, Clinical clinical) {
    // Filter excluded donors
    val excludedDonorIds = pcawgDictionary.getExcludedDonorIds(context.getProjectKey());
    val excludedSampleIds = Sets.<String> newHashSet(pcawgDictionary.getExcludedSampleIds(context.getProjectKey()));
  
    val index = new ClinicalIndex(clinical);
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
    }
  
    return excludedSampleIds;
  }

  private Set<String> filterSampleIds(Set<String> excludedSampleIds, String projectKey) {
    val referenceSampleIds = newHashSet(pcawgSampleSheet.getProjectSampleIds().get(projectKey));
    return difference(referenceSampleIds, excludedSampleIds);
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
    val projectNames = pcawgSampleSheet.getProjects();

    return projectNames.contains(projectKey);
  }

  private static boolean isTCGA(String projectKey) {
    // For DCC testing of PCAWG TCGA projects
    val testTCGA = projectKey.startsWith("TEST") && projectKey.contains("TCGA");

    return testTCGA || Programs.isTCGA(projectKey);
  }

}
