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
import static org.icgc.dcc.core.model.FieldNames.NormalizerFieldNames.NORMALIZER_MARKING;
import lombok.RequiredArgsConstructor;

import org.icgc.dcc.core.model.Marking;
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
 * Step in charge of pre-emptively marking all observations as {@link Marking#OPEN} for the {@link SensitiveRowMarking}
 * step.
 */
@RequiredArgsConstructor
public final class PreMarking implements NormalizationStep {

  static final Fields MARKING_FIELD = new Fields(NORMALIZER_MARKING);

  @Override
  public String shortName() {
    return "pre-marking";
  }

  @Override
  public Pipe extend(Pipe pipe, NormalizationContext context) {
    return new Each(
        pipe,
        ALL,
        new PreMarker(),
        ALL);
  }

  /**
   * Marks all observations as {@link Marking#OPEN}.
   */
  @VisibleForTesting
  static final class PreMarker extends BaseOperation<Void> implements Function<Void> {

    @VisibleForTesting
    PreMarker() {
      super(MARKING_FIELD);
    }

    @Override
    public void operate(
        @SuppressWarnings("rawtypes") FlowProcess flowProcess,
        FunctionCall<Void> functionCall) {

      functionCall
          .getOutputCollector()
          .add(
              // Until specified otherwise (if applicable as it can be turned off)
              new Tuple(Marking.OPEN.name())
          );
    }
  }
}
