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

import java.util.List;

import org.icgc.dcc.common.core.model.Programs;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.util.CodeLists;
import org.icgc.dcc.submission.validation.core.ClinicalCore;
import org.icgc.dcc.submission.validation.core.ClinicalParser;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.pcawg.core.PCAWGClinicalFilter;
import org.icgc.dcc.submission.validation.pcawg.core.PCAWGDictionary;
import org.icgc.dcc.submission.validation.pcawg.core.PCAWGSample;
import org.icgc.dcc.submission.validation.pcawg.core.PCAWGSampleFilter;
import org.icgc.dcc.submission.validation.pcawg.core.PCAWGSampleSheet;
import org.icgc.dcc.submission.validation.pcawg.core.PCAWGSampleValidator;

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
 * @see https://jira.oicr.on.ca/browse/DCC-4615
 * @see https://jira.oicr.on.ca/browse/DCC-4564
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
  public void validate(@NonNull ValidationContext context) throws InterruptedException {
    log.info("Starting...");

    // Selective validation filtering
    if (!isValidatable(context)) {
      log.info("Validation not required for '{}'. Skipping...", context.getProjectKey());

      return;
    }

    validateClinical(context);
  }

  private void validateClinical(ValidationContext context) {
    val projectKey = context.getProjectKey();

    // Filter actual clinical core entities returning the excluded entity ids
    val clinicalCore = filterClinicalCore(projectKey, context);

    // Filter expected PCAWG samples to remove excluded entity ids
    val pcawgSamples = filterPCAWGSamples(projectKey);

    // Validate with filtered actual and expected values
    new PCAWGSampleValidator(getSpecimenTypes(context), clinicalCore, pcawgSamples, context).execute();
  }

  private ClinicalCore filterClinicalCore(String projectKey, ValidationContext context) {
    val clinical = ClinicalParser.parse(context);

    val filter = new PCAWGClinicalFilter(projectKey, pcawgDictionary);
    filter.filter(clinical);

    return clinical.getCore();
  }

  private List<PCAWGSample> filterPCAWGSamples(String projectKey) {
    val pcawgSamples = pcawgSampleSheet.getProjectSamples(projectKey);
    val filter = new PCAWGSampleFilter(projectKey, pcawgDictionary);

    return filter.filter(pcawgSamples);
  }

  private boolean isValidatable(ValidationContext context) {
    val projectKey = context.getProjectKey();

    if (isProjectExcluded(projectKey)) {
      return false;
    }

    // For DCC testing of PCAWG projects
    val testPCAWG = projectKey.startsWith("TESTP-");

    val pcawg = testPCAWG || pcawgSampleSheet.hasProject(projectKey);
    val tcga = isTCGA(projectKey);

    return pcawg && !tcga;
  }

  private boolean isProjectExcluded(String projectKey) {
    return pcawgDictionary.getExcludedProjectKeys().contains(projectKey);
  }

  private static boolean isTCGA(String projectKey) {
    // For DCC testing of PCAWG TCGA projects
    val testTCGA = projectKey.startsWith("TEST") && projectKey.contains("TCGA");

    return testTCGA || Programs.isTCGA(projectKey);
  }

  private static CodeList getSpecimenTypes(ValidationContext context) {
    return CodeLists.getSpecimenTypes(context.getCodeLists());
  }

}
