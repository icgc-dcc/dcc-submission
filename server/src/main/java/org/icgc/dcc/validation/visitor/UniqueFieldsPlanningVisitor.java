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

import java.util.Iterator;
import java.util.List;

import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.validation.InternalFlowPlanningVisitor;
import org.icgc.dcc.validation.InternalPlanElement;
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

  public UniqueFieldsPlanningVisitor() {
  }

  @Override
  public void visit(FileSchema fileSchema) {
    super.visit(fileSchema);
    if(fileSchema.getUniqueFields().size() > 0) {
      collect(new UniqueFieldsPlanElement(fileSchema.getUniqueFields()));
    }
  }

  private static class UniqueFieldsPlanElement implements InternalPlanElement {

    private final List<String> fields;

    private UniqueFieldsPlanElement(Iterable<String> fields) {
      this.fields = ImmutableList.copyOf(fields);
    }

    @Override
    public String describe() {
      return String.format("unique[%s]", fields);
    }

    @Override
    public Pipe extend(Pipe pipe) {
      Fields groupFields = new Fields(fields.toArray(new String[] {}));
      pipe = new GroupBy(pipe, groupFields);
      pipe = new Every(pipe, Fields.ALL, new CountBuffer(), Fields.RESULTS);

      // These don't work because you can only obtain Fields.GROUP or Fields.VALUES, but not both
      // pipe = new CountBy(pipe, groupFields, new Fields("count"));
      // pipe = new Each(pipe, new ValidationFields("count"), new CountIsOne(), Fields.REPLACE);
      // pipe = new Discard(pipe, new Fields("count"));
      return pipe;
    }

    private static class CountBuffer extends BaseOperation implements Buffer {

      CountBuffer() {
        super(Fields.ARGS);
      }

      @Override
      public void operate(FlowProcess flowProcess, BufferCall bufferCall) {
        int count = 0;
        Iterator<TupleEntry> i = bufferCall.getArgumentsIterator();
        while(i.hasNext()) {
          TupleEntry tupleEntry = i.next();
          if(count > 0) {
            ValidationFields.state(tupleEntry).reportError(500, "not unique");
          }
          count++;
          bufferCall.getOutputCollector().add(tupleEntry.getTupleCopy());
        }
      }
    }

  }
}
