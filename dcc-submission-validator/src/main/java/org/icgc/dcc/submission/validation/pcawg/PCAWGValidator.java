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

import java.util.Collection;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.model.Programs;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.pcawg.core.ClinicalProcessor;
import org.icgc.dcc.submission.validation.pcawg.parser.ClinicalParser;
import org.icgc.dcc.submission.validation.pcawg.util.PCAWGClient;
import org.icgc.dcc.submission.validation.pcawg.util.TCGAClient;

/**
 * Validator responsible for ensuring PCAWG validation rules are enforced.
 * <p>
 * This class assumes that prior {@code Validator}s have ensured that clinical data exists and that all files are
 * welformed.
 * 
 * @see https://jira.oicr.on.ca/browse/DCC-3012
 */
@Slf4j
@RequiredArgsConstructor
public class PCAWGValidator implements Validator {

  @NonNull
  private final PCAWGClient pcawgClient;
  @NonNull
  private final TCGAClient tcgaClient;

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

  private boolean isValidatable(ValidationContext context) {
    return isPCAWG(context.getProjectKey());
  }

  private void validateClinical(ValidationContext context) {
    val clinical = ClinicalParser.parse(context);
    val referenceSampleIds = getReferenceSampleIds(context);
    val tcga = Programs.isTCGA(context.getProjectKey());

    val processor = new ClinicalProcessor(clinical, tcga, referenceSampleIds, context);
    processor.process();
  }

  private Collection<String> getReferenceSampleIds(ValidationContext context) {
    val projectSamples = pcawgClient.getProjectSamples();

    return projectSamples.get(context.getProjectKey());
  }

  private boolean isPCAWG(String projectKey) {
    val projectNames = pcawgClient.getProjects();

    return projectNames.contains(projectKey);
  }

}
