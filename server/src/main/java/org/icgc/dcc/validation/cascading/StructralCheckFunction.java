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
import java.util.List;

import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;

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

  private Fields fileHeader;

  public StructralCheckFunction(FileSchema fileSchema) {
    super(1);

    List<Field> fields = fileSchema.getFields();
    List<String> fieldNames = new ArrayList<String>();
    for(Field field : fields) {
      fieldNames.add(field.getName());
    }
    this.fieldDeclaration = new Fields(fieldNames.toArray(new String[fields.size()]));

  }

  @Override
  public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
    TupleEntry arguments = functionCall.getArguments();

    // parse line into tuple values
    String line = arguments.getString(0);
    Iterable<String> splitter = Splitter.on('\t').split(line);
    List<String> values = Lists.newArrayList(splitter);

    Fields missingFields = fieldDeclaration.subtract(fileHeader);
    Fields resultFields = this.fileHeader.append(missingFields);

    // add null value to all missing fields
    for(int i = 0; i < missingFields.size(); i++) {
      values.add(null);
    }

    TupleEntry tupleEntry = new TupleEntry(fieldDeclaration, new Tuple(values.toArray(new String[values.size()])));

    functionCall.getOutputCollector().add(tupleEntry);
  }

  public void setFileHeader(Fields header) {
    this.fileHeader = header;
  }
}
