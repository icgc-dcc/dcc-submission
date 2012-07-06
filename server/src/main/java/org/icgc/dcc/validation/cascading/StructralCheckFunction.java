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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.google.common.collect.Iterables;

/**
 * 
 */
public class StructralCheckFunction extends BaseOperation implements Function {

  private final FileSchema fileSchema;

  public StructralCheckFunction(FileSchema fileSchema) {
    super(1);
    this.fileSchema = fileSchema;
  }

  @Override
  public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
    TupleEntry arguments = functionCall.getArguments();
    Fields header = this.fieldDeclaration;
    List<Field> fields = fileSchema.getFields();
    List<String> fieldNames = new ArrayList<String>();
    for(Field field : fields) {
      fieldNames.add(field.getName());
    }
    Fields schemaHeader = new Fields(fieldNames.toArray(new String[fields.size()]));

    functionCall.getOutputCollector().setFields(schemaHeader);

    // parse line into tuple values
    Map<String, String> valueMap = new HashMap<String, String>();

    String line = arguments.getString("line");
    Iterable<String> splitter = Splitter.on('\t').split(line);
    String[] values = Iterables.toArray(splitter, String.class);
    for(int i = 0; i < values.length; i++) {
      valueMap.put((String) header.get(i), values[i]);
    }

    String[] tupleValue = new String[schemaHeader.size()];

    for(int i = 0; i < schemaHeader.size(); i++) {
      tupleValue[i] = valueMap.get(schemaHeader.get(i));
    }

    functionCall.getOutputCollector().add(new Tuple(tupleValue));
  }

  public void setFieldDeclaration(Fields header) {
    this.fieldDeclaration = header;
  }
}
