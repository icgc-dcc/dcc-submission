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
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_ANALYZED_SAMPLE_ID;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_DONOR_ID;

import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

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

/**
 * Adds the corresponding donor ID for sample IDs.
 */
@RequiredArgsConstructor
public final class DonorIdAddition implements NormalizationStep {

  public static final Fields SAMPLE_ID_FIELD = new Fields(SUBMISSION_ANALYZED_SAMPLE_ID);
  public static final Fields DONOR_ID_FIELD = new Fields(SUBMISSION_DONOR_ID);

  @Override
  public String shortName() {
    return "donor-id-addition";
  }

  @Override
  public Pipe extend(Pipe pipe, NormalizationContext context) {
    return new Each(pipe, new DonorIdAdder(context.getSampleToDonorMap()), ALL);
  }

  /**
   * See {@link DonorIdAddition}.
   */
  @VisibleForTesting
  final class DonorIdAdder extends BaseOperation<Void> implements Function<Void> {

    @NonNull
    private final Map<String, String> sampleToDonorMap;

    @VisibleForTesting
    DonorIdAdder(Map<String, String> sampleToDonorMap) {
      super(DONOR_ID_FIELD);
      this.sampleToDonorMap = sampleToDonorMap;
    }

    @Override
    public void operate(
        @SuppressWarnings("rawtypes") FlowProcess flowProcess,
        FunctionCall<Void> functionCall) {
      val donorId = sampleToDonorMap.get(
          functionCall
              .getArguments()
              .getString(SAMPLE_ID_FIELD));
      functionCall
          .getOutputCollector()
          .add(new Tuple(donorId));
    }
  }
}
