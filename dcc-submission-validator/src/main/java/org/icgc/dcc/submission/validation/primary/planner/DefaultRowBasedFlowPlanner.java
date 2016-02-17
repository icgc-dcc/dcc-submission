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
package org.icgc.dcc.submission.validation.primary.planner;

import static cascading.tuple.Fields.ALL;
import static cascading.tuple.Fields.REPLACE;
import static cascading.tuple.Fields.SWAP;
import static com.google.common.base.Preconditions.checkArgument;
import static org.icgc.dcc.submission.validation.cascading.StructuralCheckFunction.LINE_FIELD_NAME;
import static org.icgc.dcc.submission.validation.cascading.ValidationFields.OFFSET_FIELD_NAME;
import static org.icgc.dcc.submission.validation.primary.core.FlowType.ROW_BASED;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.cascading.ForbiddenValuesFunction;
import org.icgc.dcc.submission.validation.cascading.RemoveEmptyValidationLineFilter;
import org.icgc.dcc.submission.validation.cascading.RemoveHeaderFilter;
import org.icgc.dcc.submission.validation.cascading.StructuralCheckFunction;
import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.cascading.TupleStates;
import org.icgc.dcc.submission.validation.cascading.ValidationFields;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy;
import org.icgc.dcc.submission.validation.primary.core.RowBasedPlanElement;

import cascading.flow.FlowDef;
import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

@Slf4j
class DefaultRowBasedFlowPlanner extends BaseFileFlowPlanner implements RowBasedFlowPlanner {

  private final Pipe headPipe;
  private Pipe structurallyValidTail;
  private Pipe structurallyInvalidTail;
  private StructuralCheckFunction structuralCheckFunction;

  DefaultRowBasedFlowPlanner(FileSchema fileSchema, String fileName) {
    super(fileSchema, fileName, ROW_BASED);
    this.headPipe = new Pipe(getSourcePipeName());

    // apply system pipe
    applySystemPipes(this.headPipe);
  }

  @Override
  public void applyRowBasedPlanElement(RowBasedPlanElement element) {
    checkArgument(element != null);
    log.info("[{}] applying element [{}]", getFlowName(), element.describe());
    structurallyValidTail = element.extend(structurallyValidTail);
  }

  @Override
  protected Pipe getReportTailPipe(String basename) {
    Pipe valid = new Pipe(basename + "_valid", structurallyValidTail);
    Pipe invalid = new Pipe(basename + "_invalid", structurallyInvalidTail);
    return new Merge(valid, invalid);
  }

  @Override
  protected Pipe getStructurallyValidTail() {
    return structurallyValidTail;
  }

  @Override
  protected Pipe getStructurallyInvalidTail() {
    return structurallyInvalidTail;
  }

  private Pipe applyStructuralCheck(Pipe pipe) {
    structuralCheckFunction = new StructuralCheckFunction(getFieldNames()); // TODO: due for a splitting
    return new Each( // parse "line" into the actual expected fields
        pipe, new Fields(OFFSET_FIELD_NAME, LINE_FIELD_NAME), structuralCheckFunction, SWAP);
  }

  @Override
  protected FlowDef onConnect(FlowDef flowDef, SubmissionPlatformStrategy platformStrategy) {
    return flowDef.addSource(
        headPipe,
        platformStrategy.getSourceTap(fileName));
  }

  private void applySystemPipes(Pipe pipe) {
    pipe = new Each(pipe, new RemoveEmptyValidationLineFilter());
    pipe = new Each(pipe, new RemoveHeaderFilter());
    pipe = applyStructuralCheck(pipe);

    // TODO: DCC-1076 - Would be better done from within {@link RequiredRestriction}.
    pipe = new Each(pipe, ALL, new ForbiddenValuesFunction(getRequiredFieldNames()), REPLACE);

    this.structurallyValidTail = new Each(pipe, TupleStates.keepStructurallyValidTuplesFilter());
    this.structurallyInvalidTail = new Each(pipe, TupleStates.keepStructurallyInvalidTuplesFilter());
  }

  @SuppressWarnings("rawtypes")
  static class OffsetFunction extends BaseOperation implements Function {

    public OffsetFunction() {
      super(1, new Fields(ValidationFields.OFFSET_FIELD_NAME));
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      TupleEntry entry = functionCall.getArguments();
      TupleState state = ValidationFields.state(entry);
      functionCall.getOutputCollector().add(new Tuple(state.getOffset()));
    }
  }

}
