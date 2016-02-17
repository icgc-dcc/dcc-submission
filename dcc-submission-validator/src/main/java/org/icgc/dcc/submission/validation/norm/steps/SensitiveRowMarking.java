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
package org.icgc.dcc.submission.validation.norm.steps;

import static cascading.tuple.Fields.ARGS;
import static cascading.tuple.Fields.REPLACE;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CONTROL_GENOTYPE;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_MUTATED_FROM_ALLELE;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_MUTATED_TO_ALLELE;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_TUMOUR_GENOTYPE;
import static org.icgc.dcc.common.core.model.Marking.CONTROLLED;
import static org.icgc.dcc.common.core.model.Marking.OPEN;
import static org.icgc.dcc.submission.validation.norm.core.NormalizationReport.NormalizationCounter.COUNT_INCREMENT;
import static org.icgc.dcc.submission.validation.norm.core.NormalizationReport.NormalizationCounter.MARKED_AS_CONTROLLED;
import static org.icgc.dcc.submission.validation.norm.steps.PreMarking.MARKING_FIELD;

import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.model.Marking;
import org.icgc.dcc.submission.validation.norm.core.NormalizationContext;
import org.icgc.dcc.submission.validation.norm.core.NormalizationStep;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;

/**
 * Steps in charge of marking sensitive observations.
 * <p>
 * A sensitive observation is one for which the original allele in the mutation does not match that of the reference
 * genome allele at the same position.
 */
@Slf4j
@RequiredArgsConstructor
public final class SensitiveRowMarking implements NormalizationStep {

  public static final String STEP_NAME = "mark";

  static final Fields REFERENCE_GENOME_ALLELE_FIELD = new Fields(SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE);
  static final Fields CONTROL_GENOTYPE_FIELD = new Fields(SUBMISSION_OBSERVATION_CONTROL_GENOTYPE);
  static final Fields TUMOUR_GENOTYPE_FIELD = new Fields(SUBMISSION_OBSERVATION_TUMOUR_GENOTYPE);
  static final Fields MUTATED_FROM_ALLELE_FIELD = new Fields(SUBMISSION_OBSERVATION_MUTATED_FROM_ALLELE);
  static final Fields MUTATED_TO_ALLELE_FIELD = new Fields(SUBMISSION_OBSERVATION_MUTATED_TO_ALLELE);

  @Override
  public String shortName() {
    return STEP_NAME;
  }

  @Override
  public Pipe extend(Pipe pipe, NormalizationContext context) {
    // Mark rows that are sensitive
    return new Each(
        pipe,

        // Argument selector
        REFERENCE_GENOME_ALLELE_FIELD
            .append(CONTROL_GENOTYPE_FIELD)
            .append(TUMOUR_GENOTYPE_FIELD)
            .append(MUTATED_FROM_ALLELE_FIELD)
            .append(MUTATED_TO_ALLELE_FIELD)
            .append(MARKING_FIELD),

        new SensitiveRowMarker(),
        REPLACE);
  }

  /**
   * Marks tuples that are sensitives.
   * <p>
   * This expects the {@link PreMarking#MARKING_FIELD} to be present already (as {@link Marking#OPEN} for all
   * observations).
   */
  @VisibleForTesting
  static final class SensitiveRowMarker extends BaseOperation<Void> implements Function<Void> {

    private static final Splitter ALLELES_SPLITTER = Splitter.on("/");

    @VisibleForTesting
    SensitiveRowMarker() {
      super(ARGS);
    }

    @Override
    public void operate(@SuppressWarnings("rawtypes") FlowProcess flowProcess, FunctionCall<Void> functionCall) {

      val entry = functionCall.getArguments();

      // Ensure expected state
      {
        val existingMasking = Marking.from(entry.getString(MARKING_FIELD));
        checkState(existingMasking.isPresent() && existingMasking.get() == Marking.OPEN,
            "Masking flag is expected to have been set to '%s' already", OPEN);
      }

      val referenceGenomeAllele = entry.getString(REFERENCE_GENOME_ALLELE_FIELD);
      val controlGenotype = entry.getString(CONTROL_GENOTYPE_FIELD);
      val tumourGenotype = entry.getString(TUMOUR_GENOTYPE_FIELD);
      val mutatedFromAllele = entry.getString(MUTATED_FROM_ALLELE_FIELD);
      val mutatedToAllele = entry.getString(MUTATED_TO_ALLELE_FIELD);

      // Mark if applicable
      final Marking masking;
      if (!matchesAllControlAlleles(referenceGenomeAllele, controlGenotype)
          || !matchesAllTumourAllelesButTo(referenceGenomeAllele, tumourGenotype, mutatedToAllele)) {

        log.debug("Marking sensitive row: '{}'", entry); // Should be rare enough
        masking = CONTROLLED;

        // Increment counter
        flowProcess.increment(MARKED_AS_CONTROLLED, COUNT_INCREMENT);
      } else {
        log.debug("Marking open-access row: '{}'", entry);
        masking = OPEN;
      }

      functionCall.getOutputCollector().add(new Tuple(
          referenceGenomeAllele,
          controlGenotype,
          tumourGenotype,
          mutatedFromAllele,
          mutatedToAllele,
          masking.getTupleValue()));
    }

    private boolean matchesAllControlAlleles(String referenceGenomeAllele, String controlGenotype) {
      val controlAlleles = getUniqueAlleles(controlGenotype);
      for (val controlAllele : controlAlleles) {
        if (!referenceGenomeAllele.equals(controlAllele)) {
          return false;
        }
      }

      return true;
    }

    private boolean matchesAllTumourAllelesButTo(String referenceGenomeAllele, String tumourGenotype,
        String mutatedToAllele) {
      for (val tumourAllele : getTumourAllelesMinusToAllele(tumourGenotype, mutatedToAllele)) {
        if (!referenceGenomeAllele.equals(tumourAllele)) {
          return false;
        }
      }
      return true;
    }

    private Set<String> getTumourAllelesMinusToAllele(String tumourGenotype, String mutatedToAllele) {
      val alleles = getUniqueAlleles(tumourGenotype);
      val removed = alleles.remove(mutatedToAllele);
      checkState(
          removed,
          "'%s' ('%s') is expected to be in '%s' ('%s') as per primary validation rules",
          mutatedToAllele, MUTATED_TO_ALLELE_FIELD, tumourGenotype, TUMOUR_GENOTYPE_FIELD);
      return alleles;
    }

    private static Set<String> getUniqueAlleles(String controlGenotype) {
      return newLinkedHashSet(ALLELES_SPLITTER.split(controlGenotype));
    }

  }
}
