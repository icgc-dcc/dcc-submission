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
package org.icgc.dcc.validation.report;

import java.util.Iterator;
import java.util.List;

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.SummaryType;
import org.icgc.dcc.validation.FlowType;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Buffer;
import cascading.operation.BufferCall;
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

public final class FrequencyPlanElement extends BaseReportingPlanElement {

  private final String FREQ = "freq";

  public FrequencyPlanElement(FileSchema fileSchema, List<Field> fields) {
    super(fileSchema, fields, SummaryType.FREQUENCY);
  }

  @Override
  public Pipe report(Pipe pipe) {
    Pipe[] freqs = new Pipe[fields.size()];
    int i = 0;
    for(Field field : fields) {
      freqs[i++] = frequency(field.getName(), pipe);
    }
    pipe = new Merge(freqs);
    pipe = new GroupBy(pipe, new Fields(FIELD));
    pipe = new Every(pipe, new Fields(VALUE, FREQ), new FrequencySummaryBuffer(), REPORT_FIELDS);
    return pipe;
  }

  protected Pipe frequency(String field, Pipe pipe) {
    pipe = new Pipe(buildSubPipeName(FREQ + "_" + field), pipe);
    pipe = new Retain(pipe, new Fields(field));
    pipe = new Rename(pipe, new Fields(field), new Fields(VALUE));
    pipe = new CountBy(pipe, new Fields(VALUE), new Fields(FREQ));
    pipe = new Each(pipe, new Insert(new Fields(FIELD), field), new Fields(FIELD, VALUE, FREQ));
    return pipe;
  }

  @SuppressWarnings("rawtypes")
  public static class FrequencySummaryBuffer extends BaseOperation implements Buffer {

    public FrequencySummaryBuffer() {
      super(2, REPORT_FIELDS);
    }

    @Override
    public void operate(FlowProcess flowProcess, BufferCall bufferCall) {
      @SuppressWarnings("unchecked")
      Iterator<TupleEntry> tuples = bufferCall.getArgumentsIterator();
      FieldSummary fs = new FieldSummary();
      fs.field = bufferCall.getGroup().getString(0);
      while(tuples.hasNext()) {
        TupleEntry tuple = tuples.next();
        String value = tuple.getString(0);
        Long frequency = tuple.getLong(1);
        if(value == null || value.isEmpty()) {
          fs.nulls += frequency;
        } else {
          fs.populated += frequency;
          fs.summary.put(value, frequency);
        }
      }
      bufferCall.getOutputCollector().add(new Tuple(fs));
    }
  }

  @Override
  public ReportCollector getCollector() {
    // FlowType is always Internal for Summary
    return new SummaryReportCollector(this.fileSchema, FlowType.INTERNAL);
  }
}
