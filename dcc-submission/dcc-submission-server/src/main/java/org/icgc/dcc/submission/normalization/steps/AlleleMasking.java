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
package org.icgc.dcc.submission.normalization.steps;

import static cascading.tuple.Fields.ALL;
import static cascading.tuple.Fields.ARGS;
import static cascading.tuple.Fields.REPLACE;
import static org.icgc.dcc.core.model.FieldNames.NormalizerFieldNames.NORMALIZER_MASKING;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CONTROL_GENOTYPE;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_MUTATED_FROM_ALLELE;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_MUTATED_TO_ALLELE;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_TUMOUR_GENOTYPE;
import static org.icgc.dcc.submission.normalization.NormalizationCounter.MARKED_AS_CONTROLLED;
import static org.icgc.dcc.submission.normalization.NormalizationCounter.MASKED;
import static org.icgc.dcc.submission.normalization.steps.AlleleMasking.Masking.CONTROLLED;
import static org.icgc.dcc.submission.normalization.steps.AlleleMasking.Masking.OPEN;
import static org.icgc.dcc.submission.normalization.steps.AlleleMasking.Masking.valueOf;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.hadoop.cascading.TupleEntries;
import org.icgc.dcc.submission.normalization.NormalizationStep;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

/**
 * TODO
 */
@Slf4j
public class AlleleMasking implements NormalizationStep {

  /**
   * 
   */
  enum Masking {
    CONTROLLED, OPEN, MASKED;
  }

  /**
   * Short name for the step.
   */
  private static final String SHORT_NAME = "masking";

  /**
   * TODO + move to core?
   */
  private static final Object NO_VALUE = null;

  @Override
  public String shortName() {
    return SHORT_NAME;
  }

  @Override
  public Pipe extend(Pipe pipe) {

    /**
     * TODO
     */
    final class SensitiveRowMarker extends BaseOperation<Void> implements Function<Void> {

      private SensitiveRowMarker() {
        super(new Fields(NORMALIZER_MASKING));
      }

      @Override
      public void operate(
          @SuppressWarnings("rawtypes")
          FlowProcess flowProcess,
          FunctionCall<Void> functionCall) {

        val entry = functionCall.getArguments();
        val referenceGenomeAllele = entry.getString(SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE);
        val mutatedFromAllele = entry.getString(SUBMISSION_OBSERVATION_MUTATED_FROM_ALLELE);

        final Masking masking;
        if (isSensitive(
            referenceGenomeAllele,
            mutatedFromAllele)) {
          log.info("Marking sensitive row: '{}'", entry); // Should be rare enough
          masking = CONTROLLED;

          flowProcess.increment(MARKED_AS_CONTROLLED, COUNT_INCREMENT);
        } else {
          log.debug("Marking open-access row: '{}'", entry);
          masking = OPEN;
        }

        functionCall
            .getOutputCollector()
            .add(new Tuple(masking));
      }

      private boolean isSensitive(String referenceGenomeAllele, String mutatedFromAllele) {
        return !referenceGenomeAllele.equals(mutatedFromAllele);
      }

    }

    /**
     * TODO
     */
    final class MaskedRowGenerator extends BaseOperation<Void> implements Function<Void> {

      private MaskedRowGenerator() {
        super(ARGS);
      }

      @Override
      public void operate(
          @SuppressWarnings("rawtypes")
          FlowProcess flowProcess,
          FunctionCall<Void> functionCall) {

        val entry = functionCall.getArguments();

        functionCall
            .getOutputCollector()
            .add(entry.getTupleCopy());

        // Create masked counterpart if sensitive and mask is non trivial (see
        // https://wiki.oicr.on.ca/display/DCCSOFT/Data+Normalizer+Component?focusedCommentId=53182773#comment-53182773)
        if (getMaskingState(entry) == CONTROLLED) {
          val referenceGenomeAllele = entry.getString(SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE);
          val mutatedToAllele = entry.getString(SUBMISSION_OBSERVATION_MUTATED_TO_ALLELE);
          if (!isTrivialMaskedMutation(
              referenceGenomeAllele,
              mutatedToAllele)) {
            log.info("Creating mask for '{}'", entry); // Rare enough that we can log
            val mask = mask(
                TupleEntries.clone(entry),
                referenceGenomeAllele);

            log.info("Resulting mask for '{}': '{}'", entry, mask); // Rare enough that we can log
            functionCall
                .getOutputCollector()
                .add(mask);

            flowProcess.increment(MASKED, COUNT_INCREMENT);
          } else {
            log.info("Skipping trivial mask for '{}'", entry); // Rare enough that we can log
          }
        }
      }

      /**
       * 
       */
      private Tuple mask(TupleEntry copy, String referenceGenomeAllele) {
        copy.set(NORMALIZER_MASKING, MASKED);
        copy.set(SUBMISSION_OBSERVATION_CONTROL_GENOTYPE, NO_VALUE);
        copy.set(SUBMISSION_OBSERVATION_TUMOUR_GENOTYPE, NO_VALUE);
        copy.setString(SUBMISSION_OBSERVATION_MUTATED_FROM_ALLELE, referenceGenomeAllele);
        return copy.getTuple();
      }

      /**
       * Returns the value for masking as set by the previous step.
       */
      private Masking getMaskingState(TupleEntry entry) {
        return valueOf(entry.getString(NORMALIZER_MASKING));
      }

      /**
       * We don't want to create a masked copy that would be result in a mutation like 'A>A' (useless).
       */
      private boolean isTrivialMaskedMutation(String referenceGenomeAllele, String mutatedToAllele) {
        return referenceGenomeAllele.equals(mutatedToAllele);
      }
    }

    pipe = new Each(
        pipe,
        ALL,
        new SensitiveRowMarker(),
        ALL);

    return new Each(
        pipe,
        ALL,
        new MaskedRowGenerator(),
        REPLACE);
  }
}
