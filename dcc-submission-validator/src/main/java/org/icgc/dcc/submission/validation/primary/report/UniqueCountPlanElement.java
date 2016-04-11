/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.primary.report;

import static org.icgc.dcc.submission.dictionary.model.SummaryType.UNIQUE_COUNT;
import static org.icgc.dcc.submission.validation.cascading.CompletenessBy.MISSING;
import static org.icgc.dcc.submission.validation.cascading.CompletenessBy.NULLS;
import static org.icgc.dcc.submission.validation.cascading.CompletenessBy.POPULATED;
import static org.icgc.dcc.submission.validation.cascading.ValidationFields.STATE_FIELD;

import java.util.Iterator;
import java.util.Map;

import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.cascading.ValidationFields;
import org.icgc.dcc.submission.validation.primary.core.FlowType;

import com.google.common.base.Optional;

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
import cascading.pipe.assembly.Discard;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

/**
 * Plans unique count and completeness (see DCC-770 about completeness) reporting.
 * <p>
 * TODO: if not doing DCC-770<br/>
 * - UniqueCountPlanElement: could this be changed to use an {@code AggregateBy}?<br/>
 * - CompletenessBuffer: don't use the _tmp fields, there may be a way to update the existing values for missing, nulls
 * and populated<br/>
 * - CompletenessAnnotationBuffer: don't waste time in the loop (see TODO)
 */
public final class UniqueCountPlanElement extends BaseStatsReportingPlanElement {

  private static final String UCOUNT = "unique_count";
  private static final String TMP_SUFFIX = "_tmp";
  private static final String NULLS_TMP = NULLS + TMP_SUFFIX; // see DCC-770 about completeness
  private static final String MISSING_TMP = MISSING + TMP_SUFFIX;
  private static final String POPULATED_TMP = POPULATED + TMP_SUFFIX;
  private static final Fields COMPLETENESS_TMP_FIELDS = new Fields(MISSING_TMP, NULLS_TMP, POPULATED_TMP);
  private static final Fields COMPLETENESS_FIELDS = new Fields(MISSING, NULLS, POPULATED);
  private static final Fields FIELD_FIELDS = new Fields(FIELD);
  private static final Fields VALUE_FIELDS = new Fields(VALUE);

  public UniqueCountPlanElement(
      FlowType flowType, String fileName, Map<String, FieldStatDigest> fieldStatDigests) {
    super(flowType, Optional.of(UNIQUE_COUNT), fileName, fieldStatDigests);
  }

  @Override
  public Pipe report(Pipe pipe) {
    pipe = keepStructurallyValidTuples(pipe);

    Pipe[] counts = new Pipe[fieldNames.size()];
    int i = 0;
    for (String fieldName : fieldNames) {
      counts[i++] = count(fieldName, pipe);
    }

    pipe = new Merge(counts);
    pipe = new GroupBy(pipe, FIELD_FIELDS);
    pipe = new Every(pipe, new CompletenessBuffer());
    pipe = new Discard(pipe, COMPLETENESS_TMP_FIELDS);
    pipe = new Each(pipe, new UniqueCountSummaryFunction(), REPORT_FIELDS);

    return pipe;
  }

  protected Pipe count(String fieldName, Pipe pipe) {
    pipe = new Pipe(getSubPipeName(UCOUNT + "_" + fieldName), pipe);

    pipe = new Retain(pipe, new Fields(fieldName).append(ValidationFields.STATE_FIELD));
    pipe = new Rename(pipe, new Fields(fieldName), VALUE_FIELDS);
    pipe = new GroupBy(pipe, VALUE_FIELDS); // the GroupBy is also used to uniquify here

    pipe = new Every(
        pipe,
        STATE_FIELD,
        new CompletenessAnnotationBuffer(fieldName),
        VALUE_FIELDS.append(COMPLETENESS_TMP_FIELDS)); // grouped by value, each group contains a list of _state
    pipe = new Each(
        pipe,
        new Insert(FIELD_FIELDS, fieldName),
        VALUE_FIELDS
            .append(COMPLETENESS_TMP_FIELDS)
            .append(FIELD_FIELDS));

    return pipe;
  }

  @SuppressWarnings("rawtypes")
  public static class CompletenessAnnotationBuffer extends BaseOperation implements Buffer {

    private final String fieldName;

    public CompletenessAnnotationBuffer(String fieldName) {
      super(1, COMPLETENESS_TMP_FIELDS);
      this.fieldName = fieldName;
    }

    @Override
    public void operate(FlowProcess flowProcess, BufferCall bufferCall) {
      String value = bufferCall.getGroup().getString(VALUE);
      @SuppressWarnings("unchecked")
      Iterator<TupleEntry> entries = bufferCall.getArgumentsIterator();
      long nulls = 0, missing = 0, populated = 0;

      while (entries.hasNext()) { // TODO: improve (very inefficient)
        TupleEntry entry = entries.next();
        TupleState state = ValidationFields.state(entry);
        if (value == null) {
          if (state.isFieldMissing(fieldName)) {
            missing++;
          } else {
            nulls++;
          }
        } else if (value.isEmpty()) {
          nulls++;
        } else {
          populated++;
        }
      }
      bufferCall.getOutputCollector().add(new Tuple(missing, nulls, populated)); // do not emit group as long as it's in
                                                                                 // your outputSelector... (not obvious)
    }
  }

  /**
   * TODO: consider Aggregator instead?
   */
  @SuppressWarnings("rawtypes")
  public static class CompletenessBuffer extends BaseOperation implements Buffer {

    public CompletenessBuffer() {
      super(4, COMPLETENESS_FIELDS.append(new Fields(UCOUNT)));
    }

    @Override
    public void operate(FlowProcess flowProcess, BufferCall bufferCall) {
      @SuppressWarnings("unchecked")
      Iterator<TupleEntry> entries = bufferCall.getArgumentsIterator();
      long nulls = 0, missing = 0, populated = 0, uniqueCount = 0;
      while (entries.hasNext()) {
        TupleEntry entry = entries.next();
        nulls += entry.getLong(NULLS_TMP);
        missing += entry.getLong(MISSING_TMP);
        populated += entry.getLong(POPULATED_TMP);
        uniqueCount++;
      }
      bufferCall.getOutputCollector().add( // TODO: see todo in class comment
          new Tuple(missing, nulls, populated, uniqueCount));
    }
  }

  @SuppressWarnings("rawtypes")
  public static class UniqueCountSummaryFunction extends BaseOperation implements Function {

    public UniqueCountSummaryFunction() {
      super(REPORT_FIELDS);
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      TupleEntry entry = functionCall.getArguments();
      FieldSummary fs = new FieldSummary();
      fs.field = entry.getString(FIELD);
      fs.nulls = entry.getLong(NULLS);
      fs.missing = entry.getLong(MISSING);
      fs.populated = entry.getLong(POPULATED);
      fs.summary.put(UCOUNT, entry.getLong(UCOUNT));
      functionCall.getOutputCollector().add(new Tuple(fs));
    }
  }
}
