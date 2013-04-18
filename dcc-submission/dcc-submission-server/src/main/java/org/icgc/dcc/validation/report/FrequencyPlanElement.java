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
package org.icgc.dcc.validation.report;

import java.util.Iterator;
import java.util.List;

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.SummaryType;
import org.icgc.dcc.validation.FlowType;
import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.cascading.ValidationFields;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Buffer;
import cascading.operation.BufferCall;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.operation.Insert;
import cascading.pipe.Each;
import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Merge;
import cascading.pipe.Pipe;
import cascading.pipe.assembly.CountBy;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

public final class FrequencyPlanElement extends BaseStatsReportingPlanElement {

  private static final String FREQ = "freq";

  private static final String MISSING_FLAG = "missing?";

  public FrequencyPlanElement(FileSchema fileSchema, List<Field> fields, FlowType flowType) {
    super(fileSchema, fields, SummaryType.FREQUENCY, flowType);
  }

  @Override
  public Pipe report(Pipe pipe) {

    pipe = keepStructurallyValidTuples(pipe);

    Pipe[] freqs = new Pipe[fields.size()];
    int i = 0;
    for(Field field : fields) {
      freqs[i++] = frequency(field.getName(), pipe);
    }
    pipe = new Merge(freqs);
    pipe = new GroupBy(pipe, new Fields(FIELD));
    pipe = new Every(pipe, new Fields(VALUE, FREQ, MISSING_FLAG), new FrequencySummaryBuffer(), REPORT_FIELDS);
    return pipe;
  }

  /**
   * - keep only the field of interest "my_field" and "_state"<br/>
   * - replace "_state" with boolean "missing?"<br/>
   * - rename "my_field" to "value"<br/>
   * - count by the combination of "value"+"missing?" and store results into "freq" (we need to keep track of "missing?"
   * for completeness reporting...)<br/>
   * - insert "my_field" as a value (instead of an {@code Fields} as it originally was)<br/>
   * <br/>
   * Output is like:
   * <table>
   * <tr>
   * <td>"field"</td>
   * <td>"value"</td>
   * <td>"missing?"</td>
   * <td>"freq"</td>
   * </tr>
   * <tr>
   * <td>f1</td>
   * <td>abc</td>
   * <td>false</td>
   * <td>5</td>
   * </tr>
   * <tr>
   * <td>f1</td>
   * <td>null</td>
   * <td>true</td>
   * <td>8</td>
   * </tr>
   * <tr>
   * <td>f1</td>
   * <td>def</td>
   * <td>fasle</td>
   * <td>4</td>
   * </tr>
   * </table>
   */
  protected Pipe frequency(String field, Pipe pipe) {
    pipe = new Pipe(buildSubPipeName(FREQ + "_" + field), pipe);
    pipe = new Retain(pipe, new Fields(field).append(ValidationFields.STATE_FIELD));
    pipe = new Each(pipe, ValidationFields.STATE_FIELD, new MissingFlaggerFunction(field), Fields.SWAP);
    pipe = new Rename(pipe, new Fields(field), new Fields(VALUE));
    pipe = new CountBy(pipe, new Fields(VALUE, MISSING_FLAG), new Fields(FREQ));
    pipe = new Each(pipe, new Insert(new Fields(FIELD), field), new Fields(FIELD, VALUE, MISSING_FLAG, FREQ));
    return pipe;
  }

  @SuppressWarnings("rawtypes")
  static class MissingFlaggerFunction extends BaseOperation implements Function {

    private final String fieldName;

    public MissingFlaggerFunction(String fieldName) {
      super(1, new Fields(MISSING_FLAG));
      this.fieldName = fieldName;
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      TupleEntry entry = functionCall.getArguments();
      TupleState state = ValidationFields.state(entry);
      functionCall.getOutputCollector().add(new Tuple(state.isFieldMissing(fieldName)));
    }
  }

  /**
   * This also populates {@code FieldSummary}'s fields populated, missing and nulls, which collectively represent what
   * we refer to as "completeness".
   * <p>
   * FIXME?: There is some logic in here that is redundant with that of {@code CompletenessBy}... (see DCC-770)
   */
  @SuppressWarnings("rawtypes")
  public static class FrequencySummaryBuffer extends BaseOperation implements Buffer {

    public FrequencySummaryBuffer() {
      super(3, REPORT_FIELDS);
    }

    @Override
    public void operate(FlowProcess flowProcess, BufferCall bufferCall) {
      @SuppressWarnings("unchecked")
      Iterator<TupleEntry> tuples = bufferCall.getArgumentsIterator();
      FieldSummary fs = new FieldSummary();
      fs.field = bufferCall.getGroup().getString(0);
      while(tuples.hasNext()) {
        TupleEntry tuple = tuples.next();
        String value = tuple.getString(0); // TODO: use field names...
        Long frequency = tuple.getLong(1);
        if(value == null) {
          if(tuple.getBoolean(MISSING_FLAG)) {
            fs.missing += frequency;
          } else {
            fs.nulls += frequency;
          }
        } else if(value.isEmpty()) {
          fs.nulls += frequency;
        } else {
          fs.populated += frequency;
          fs.summary.put(value, frequency);
        }
      }
      bufferCall.getOutputCollector().add(new Tuple(fs));
    }
  }
}
