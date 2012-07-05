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
package org.icgc.dcc.validation.cascading;

import java.util.ArrayList;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * 
 */
public class StructralCheckFunction extends BaseOperation implements Function {

  @Override
  public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
    TupleEntry arguments = functionCall.getArguments();
    Fields header = arguments.getFields();
    Fields schemaHeader = this.fieldDeclaration;

    // Fields declared = new Fields("offset");
    // declared = declared.append(schemaHeader);
    // functionCall.getOutputCollector().setFields(declared);
    functionCall.getOutputCollector().setFields(schemaHeader);

    ArrayList<String> tupleValue = new ArrayList<String>();
    String offset = arguments.getString("num");
    tupleValue.add(offset);

    String line = arguments.getString("line");
    Iterable<String> splitter = Splitter.on('\t').split(line);
    tupleValue.addAll(Lists.newArrayList(splitter));

    functionCall.getOutputCollector().add(new Tuple(tupleValue.toArray(new Object[schemaHeader.size()])));

  }

  public void setFieldDeclaration(Fields header) {
    this.fieldDeclaration = header;
  }
}
