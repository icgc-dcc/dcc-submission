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

import static org.icgc.dcc.common.core.util.Jackson.DEFAULT;

import java.util.List;
import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.model.Programs;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.pcawg.core.ClinicalRule;
import org.icgc.dcc.submission.validation.pcawg.core.ClinicalRuleEngine;
import org.icgc.dcc.submission.validation.pcawg.external.PanCancerClient;
import org.icgc.dcc.submission.validation.pcawg.external.TCGAClient;
import org.icgc.dcc.submission.validation.pcawg.parser.ClinicalParser;
import org.icgc.dcc.submission.validation.pcawg.util.ClinicalIndex;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

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
   * Constants.
   */
  private static final String RULES_FILE = "pcawg-clinical-rules.json";

  @NonNull
  private final PanCancerClient pcawgClient;
  @NonNull
  private final TCGAClient tcgaClient;

  @Override
  public String getName() {
    return "PCAWGFields Validator";
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

  private boolean isValidatable(ValidationContext context) {
    return isPCAWG(context.getProjectKey());
  }

  private void validateClinical(ValidationContext context) {
    val clinical = ClinicalParser.parse(context);
    val referenceSampleIds = getReferenceSampleIds(context);
    val tcga = Programs.isTCGA(context.getProjectKey());

    val ruleEngine = new ClinicalRuleEngine(
        readRules(),

        // Data
        clinical, new ClinicalIndex(clinical), tcga,

        // Reference
        tcgaClient, referenceSampleIds,

        // Reporting
        context);

    ruleEngine.execute();
  }

  private Set<String> getReferenceSampleIds(ValidationContext context) {
    val projectSamples = pcawgClient.getProjectSamples();
    val sampleIds = projectSamples.get(context.getProjectKey());

    return Sets.newTreeSet(sampleIds);
  }

  private boolean isPCAWG(String projectKey) {
    val projectNames = pcawgClient.getProjects();

    return projectNames.contains(projectKey);
  }

  @SneakyThrows
  public static List<ClinicalRule> readRules() {
    val node = DEFAULT.readTree(Resources.getResource(RULES_FILE));
    val values = node.get("rules");

    return DEFAULT.convertValue(values, new TypeReference<List<ClinicalRule>>() {});
  }

}
