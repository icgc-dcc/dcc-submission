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
package org.icgc.dcc.submission.validation.norm.steps;

import static cascading.tuple.Fields.ALL;
import static com.google.common.base.Joiner.on;
import static org.icgc.dcc.core.model.FieldNames.NormalizerFieldNames.NORMALIZER_MUTATION;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_MUTATED_FROM_ALLELE;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_MUTATED_TO_ALLELE;
import lombok.val;

import org.icgc.dcc.core.model.FieldNames.NormalizerFieldNames;
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
import com.google.common.base.Joiner;

/**
 * Step in charge of rebuilding {@link NormalizerFieldNames#NORMALIZER_MUTATION} field.
 */
public final class MutationRebuilding implements NormalizationStep {

  static final Fields MUTATED_FROM_ALLELE_FIELD = new Fields(SUBMISSION_OBSERVATION_MUTATED_FROM_ALLELE);
  static final Fields MUTATED_TO_ALLELE_FIELD = new Fields(SUBMISSION_OBSERVATION_MUTATED_TO_ALLELE);
  static final Fields MUTATION_FIELD = new Fields(NORMALIZER_MUTATION);

  /**
   * Short name for the step.
   */
  private static final String SHORT_NAME = "mutation";

  /**
   * Joiner to use to concatenate the "from" and "to" allele fields.
   */
  private static final Joiner MUTATION_JOINER = on(">");

  @Override
  public String shortName() {
    return SHORT_NAME;
  }

  @Override
  public Pipe extend(Pipe pipe, NormalizationContext context) {
    return new Each(
        pipe,
        ALL,
        new MutationRebuilder(),
        ALL);
  }

  /**
   * Rebuilds the mutation by concatenating the "from" and "to" allele fields.
   */
  @VisibleForTesting
  static final class MutationRebuilder extends BaseOperation<Void> implements Function<Void> {

    @VisibleForTesting
    MutationRebuilder() {
      super(MUTATION_FIELD);
    }

    /**
     * Rebuild 'mutation' from 'control_genotype' and 'tumour_genotype' and TODO
     */
    @Override
    public void operate(
        @SuppressWarnings("rawtypes") FlowProcess flowProcess,
        FunctionCall<Void> functionCall) {

      val entry = functionCall.getArguments();
      val mutatedFromAllele = entry.getString(MUTATED_FROM_ALLELE_FIELD);
      val mutatedToAllele = entry.getString(MUTATED_TO_ALLELE_FIELD);
      val mutation = MUTATION_JOINER.join(mutatedFromAllele, mutatedToAllele);
      functionCall
          .getOutputCollector()
          .add(new Tuple(mutation));
    }
  }

}
