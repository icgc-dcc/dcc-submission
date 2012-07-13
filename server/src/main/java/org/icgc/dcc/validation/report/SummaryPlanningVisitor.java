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
import java.util.Map;

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.validation.FlowType;
import org.icgc.dcc.validation.ReportingFlowPlanningVisitor;
import org.icgc.dcc.validation.ReportingPlanElement;

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

import com.google.common.collect.Maps;

public class SummaryPlanningVisitor extends ReportingFlowPlanningVisitor {

  private static final String FIELD = "field";

  private static final String VALUE = "value";

  private static final String FREQ = "freq";

  private static final String REPORT = "report";

  public SummaryPlanningVisitor() {
    super(FlowType.INTERNAL);
  }

  @Override
  public void visit(FileSchema fileSchema) {
    super.visit(fileSchema);
    collect(new FrequencyPlanElement(fileSchema.getFields()));
  }

  public static class FieldSummary {

    public String field;

    public long populated;

    public long nulls;

    public Map<String, Object> summary = Maps.newLinkedHashMap();

  }

  static class FrequencyPlanElement implements ReportingPlanElement {

    private final List<Field> fields;

    public FrequencyPlanElement(List<Field> fields) {
      this.fields = fields;
    }

    @Override
    public String getName() {
      return "frequencies";
    }

    @Override
    public String describe() {
      return String.format("freq%s", fields);
    }

    @Override
    public Pipe report(Pipe pipe) {
      Pipe[] freqs = new Pipe[fields.size()];
      int i = 0;
      for(Field field : fields) {
        // TODO: handle all types of SummaryType
        freqs[i++] = frequency(field.getName(), pipe);
      }
      pipe = new Merge(freqs);
      pipe = new GroupBy(pipe, new Fields(FIELD));
      pipe = new Every(pipe, new Fields(VALUE, FREQ), new SummaryBuffer(), new Fields(REPORT));
      return pipe;
    }

    private Pipe frequency(String field, Pipe pipe) {
      pipe = new Pipe("freq_" + field, pipe);
      pipe = new Retain(pipe, new Fields(field));
      pipe = new Rename(pipe, new Fields(field), new Fields(VALUE));
      pipe = new CountBy(pipe, new Fields(VALUE), new Fields(FREQ));
      pipe = new Each(pipe, new Insert(new Fields(FIELD), field), new Fields(FIELD, VALUE, FREQ));
      return pipe;
    }

    @SuppressWarnings("rawtypes")
    private class SummaryBuffer extends BaseOperation implements Buffer {

      SummaryBuffer() {
        super(2, new Fields(REPORT));
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

  }

}
