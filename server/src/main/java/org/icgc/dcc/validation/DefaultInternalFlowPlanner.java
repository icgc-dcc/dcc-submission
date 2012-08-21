/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.validation.cascading.RemoveEmptyLineFilter;
import org.icgc.dcc.validation.cascading.RemoveHeaderFilter;
import org.icgc.dcc.validation.cascading.StructralCheckFunction;
import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.cascading.TupleStates;
import org.icgc.dcc.validation.cascading.ValidationFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cascading.flow.FlowDef;
import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.assembly.Retain;
import cascading.tap.Tap;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

import com.google.common.collect.Maps;
import com.google.common.collect.ObjectArrays;

class DefaultInternalFlowPlanner extends BaseFileSchemaFlowPlanner implements InternalFlowPlanner {

  private static final Logger log = LoggerFactory.getLogger(DefaultInternalFlowPlanner.class);

  private final Pipe head;

  private Pipe structurallyValidTail;

  private Pipe structurallyInvalidTail;

  private final Map<Trim, Pipe> trimmedTails = Maps.newHashMap();

  private StructralCheckFunction structralCheck;

  DefaultInternalFlowPlanner(FileSchema fileSchema) {
    super(fileSchema, FlowType.INTERNAL);
    this.head = new Pipe(fileSchema.getName());

    // apply system pipe
    applySystemPipes(this.head);
  }

  @Override
  public void apply(InternalPlanElement element) {
    checkArgument(element != null);
    log.info("[{}] applying element [{}]", getName(), element.describe());
    structurallyValidTail = element.extend(structurallyValidTail);
  }

  @Override
  public Trim addTrimmedOutput(String... fields) {
    checkArgument(fields != null);
    checkArgument(fields.length > 0);

    String[] trimFields = // in order to obtain offset of referencing side
        ObjectArrays.concat(fields, ValidationFields.OFFSET_FIELD_NAME);

    Trim trim = new Trim(getSchema(), trimFields);
    if(trimmedTails.containsKey(trim) == false) {
      String[] preTrimFields = ObjectArrays.concat(fields, ValidationFields.STATE_FIELD_NAME);

      Pipe newHead = new Pipe(trim.getName(), structurallyValidTail);
      Pipe tail = new Retain(newHead, new Fields(preTrimFields));
      tail = new Each(tail, ValidationFields.STATE_FIELD, new OffsetFunction(), Fields.SWAP);

      log.info("[{}] planned trimmed output with {}", getName(), Arrays.toString(trim.getFields()));
      trimmedTails.put(trim, tail);
    }
    return trim;
  }

  @Override
  protected Pipe getTail(String basename) {
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

  @Override
  protected FlowDef onConnect(FlowDef flowDef, CascadingStrategy strategy) {
    checkState(structralCheck != null);
    Tap<?, ?, ?> source = strategy.getSourceTap(getSchema());
    try {
      Fields header = strategy.getFileHeader(getSchema());
      structralCheck.processFileHeader(header);

    } catch(IOException e) {
      e.printStackTrace();
    } catch(IllegalArgumentException e) {
      if(e.getMessage().contains("duplicate field name found")) {
        throw new PlanningException(getSchema().getName(), ValidationErrorCode.DUPLICATE_HEADER_ERROR);
      }
    }

    flowDef.addSource(head, source);

    for(Map.Entry<Trim, Pipe> e : trimmedTails.entrySet()) {
      flowDef.addTailSink(e.getValue(), strategy.getTrimmedTap(e.getKey()));
    }
    return flowDef;
  }

  private void applySystemPipes(Pipe pipe) {
    pipe = new Each(pipe, new RemoveEmptyLineFilter());
    pipe = new Each(pipe, new RemoveHeaderFilter());
    structralCheck = new StructralCheckFunction(getSchema().getFieldNames());
    pipe =
        new Each(pipe, new Fields(ValidationFields.OFFSET_FIELD_NAME, StructralCheckFunction.LINE_FIELD_NAME),
            structralCheck, Fields.SWAP); // parse "line" into the actual expected fields
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
