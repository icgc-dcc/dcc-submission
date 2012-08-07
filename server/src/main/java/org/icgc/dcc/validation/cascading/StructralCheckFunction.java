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

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Checks structural aspects of an input data file (header, format, ...)
 */
@SuppressWarnings("rawtypes")
public class StructralCheckFunction extends BaseOperation implements Function {

  private Fields fileHeader;

  private Fields resultFields;

  private Fields extraFields;

  private Fields missingFields;

  public StructralCheckFunction(Iterable<String> fieldNames) {
    super(1, new Fields(Iterables.toArray(fieldNames, String.class)));
  }

  public void handleFileHeader(Fields header) {
    this.fileHeader = header;

    Fields fields = Fields.merge(fileHeader, fieldDeclaration);
    extraFields = fields.subtract(fieldDeclaration);
    resultFields = fileHeader.subtract(extraFields);
    missingFields = fieldDeclaration.subtract(resultFields);
    resultFields = resultFields.append(missingFields);
    checkState(buildSortedList(fieldDeclaration).equals(buildSortedList(resultFields))); // worth checking; order may
                                                                                         // differ
  }

  @Override
  public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
    checkState(resultFields != null);

    TupleEntry arguments = functionCall.getArguments();

    // parse line into tuple values
    String line = arguments.getString(0);
    Iterable<String> splitter = Splitter.on('\t').split(line);
    List<String> values = Lists.newArrayList(splitter);

    int size = fieldDeclaration.size();
    int dataSize = values.size();
    if(dataSize > size) {
      values = values.subList(0, size);
    } else if(dataSize < size) {
      values.addAll(Arrays.asList(new String[size - dataSize]));
    }

    TupleEntry tupleEntry = new TupleEntry(resultFields, new Tuple(values.toArray()));
    functionCall.getOutputCollector().add(tupleEntry);
  }

  @SuppressWarnings("unchecked")
  private List<Comparable> buildSortedList(Fields fields) {
    List<Comparable> l = new ArrayList<Comparable>();
    for(int i = 0; i < fields.size(); i++) {
      l.add(fields.get(i));
    }
    Collections.sort(l);
    return ImmutableList.<Comparable> copyOf(l);
  }
}
