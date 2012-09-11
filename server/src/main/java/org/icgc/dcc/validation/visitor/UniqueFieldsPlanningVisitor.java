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
package org.icgc.dcc.validation.visitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.validation.InternalFlowPlanningVisitor;
import org.icgc.dcc.validation.InternalPlanElement;
import org.icgc.dcc.validation.ValidationErrorCode;
import org.icgc.dcc.validation.cascading.ValidationFields;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Buffer;
import cascading.operation.BufferCall;
import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.TupleEntry;

import com.google.common.collect.ImmutableList;

/**
 * Creates {@code PlanElement}s for unique fields of a {@code FileSchema}.
 */
public class UniqueFieldsPlanningVisitor extends InternalFlowPlanningVisitor {

  public static final String NAME = "uniqueFields";

  @Override
  public void visit(FileSchema fileSchema) {
    super.visit(fileSchema);
    if(fileSchema.getUniqueFields().size() > 0) {
      collect(new UniqueFieldsPlanElement(fileSchema.getUniqueFields()));
    }
  }

  static class UniqueFieldsPlanElement implements InternalPlanElement {

    private final List<String> fields;

    private UniqueFieldsPlanElement(Iterable<String> fields) {
      this.fields = ImmutableList.copyOf(fields);
    }

    @Override
    public String describe() {
      return String.format("%s[%s]", NAME, fields);
    }

    @Override
    public Pipe extend(Pipe pipe) {
      Fields groupFields = new Fields(fields.toArray(new String[] {}));
      pipe = new GroupBy(pipe, groupFields);
      pipe = new Every(pipe, Fields.ALL, new CountBuffer(fields), Fields.RESULTS);
      return pipe;
    }

    @SuppressWarnings("rawtypes")
    static class CountBuffer extends BaseOperation implements Buffer {
      private final List<String> fields;

      CountBuffer(List<String> fields) {
        super(Fields.ARGS);
        this.fields = ImmutableList.copyOf(fields);
      }

      @Override
      @SuppressWarnings("unchecked")
      public void operate(FlowProcess flowProcess, BufferCall bufferCall) {
        Iterator<TupleEntry> i = bufferCall.getArgumentsIterator();
        if(i.hasNext()) {
          TupleEntry firstTuple = i.next();
          bufferCall.getOutputCollector().add(firstTuple.getTupleCopy());
          long firstOffset = ValidationFields.state(firstTuple).getOffset();

          while(i.hasNext()) {
            TupleEntry tupleEntry = i.next();
            List<String> values = fetchValues(tupleEntry);

            ValidationFields.state(tupleEntry).reportError(ValidationErrorCode.UNIQUE_VALUE_ERROR, fields.toString(),
                values, firstOffset);
            bufferCall.getOutputCollector().add(tupleEntry.getTupleCopy());
          }
        }
      }

      private List<String> fetchValues(TupleEntry tupleEntry) {
        List<String> values = new ArrayList<String>();
        for(String field : fields) {
          values.add(tupleEntry.getString(field));
        }
        return values;
      }
    }
  }
}
