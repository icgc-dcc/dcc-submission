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

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.validation.InternalFlowPlanningVisitor;
import org.icgc.dcc.validation.InternalPlanElement;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Buffer;
import cascading.operation.BufferCall;
import cascading.operation.Function;
import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

public class FrequencyPlanningVisitor extends InternalFlowPlanningVisitor {

  @Override
  public void visit(Field field) {
    super.visit(field);

    collect(new FrequencyPlanElement(field));

  }

  static class FrequencyPlanElement implements InternalPlanElement {

    private final Field field;

    public FrequencyPlanElement(Field field) {
      this.field = field;
    }

    @Override
    public String describe() {
      return null;
    }

    @Override
    public Pipe extend(Pipe pipe) {
      Pipe freq = new GroupBy(pipe, new Fields(field.getName()));
      freq = new Every(freq, new FrequencyBuffer(field.getName()));
      return freq;
    }
  }

  static final class FrequencyBuffer extends BaseBuffer {

    String field;

    FrequencyBuffer(String field) {
      super(1);
      this.field = field;
    }

    @Override
    public void operate(FlowProcess fp, BufferCall bufferCall) {
      Iterator<TupleEntry> i = bufferCall.getArgumentsIterator();
      int freq = 0;
      String value = null;
      while(i.hasNext()) {
        TupleEntry e = i.next();
        value = e.getObject(0).toString();
        freq++;
      }
      bufferCall.getOutputCollector().add(new TupleEntry(new Fields(field + ":" + value), new Tuple(freq)));
    }
  }

  private static abstract class BaseBuffer extends BaseOperation implements Buffer {

    public BaseBuffer() {
      super();
    }

    public BaseBuffer(Fields fieldDeclaration) {
      super(fieldDeclaration);
    }

    public BaseBuffer(int numArgs) {
      super(numArgs);
    }

    protected BaseBuffer(int args, Fields fields) {
      super(args, fields);
    }

  }

  private static abstract class BaseFunction extends BaseOperation implements Function {

  }

}
