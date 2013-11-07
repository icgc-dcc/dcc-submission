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
package org.icgc.dcc.submission.normalization.steps.hacks;

import static cascading.tuple.Fields.ALL;

import org.icgc.dcc.submission.normalization.NormalizationContext;
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
 * Temporary hack
 */
public class HackNewFieldsSynthesis implements NormalizationStep {

  private final String field1;
  private final String field2;

  public HackNewFieldsSynthesis(String field1, String field2) {
    this.field1 = field1;
    this.field2 = field2;
  }

  @Override
  public String shortName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Pipe extend(Pipe pipe, NormalizationContext context) {

    final class NewFieldsFakerFunction extends BaseOperation<Void> implements Function<Void> {

      private NewFieldsFakerFunction(String field1, String field2) {
        super(new Fields(field1, field2));
      }

      @Override
      public void operate(
          @SuppressWarnings("rawtypes") FlowProcess flowProcess,
          FunctionCall<Void> functionCall) {

        TupleEntry entry = functionCall.getArguments();
        String controlGenotype = entry.getString("control_genotype");
        String tumourGenotype = entry.getString("tumour_genotype");
        String from = controlGenotype.split("/")[1];
        String to = tumourGenotype.split("/")[1];
        functionCall
            .getOutputCollector()
            .add(new Tuple(from, to));
      }
    }

    return new Each(
        pipe,
        ALL,
        new NewFieldsFakerFunction(field1, field2),
        ALL);
  }
}
