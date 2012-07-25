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

import static com.google.common.base.Preconditions.checkState;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.SummaryType;
import org.icgc.dcc.validation.FlowType;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Buffer;
import cascading.operation.BufferCall;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.operation.aggregator.Max;
import cascading.operation.aggregator.Min;
import cascading.pipe.CoGroup;
import cascading.pipe.Each;
import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.pipe.joiner.InnerJoin;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

public abstract class AggregateReportingPlanElement extends BaseReportingPlanElement {

  private static final String NULLS = "nulls";

  private static final String POPULATED = "populated";

  private static final String MIN = "min";

  private static final String MAX = "max";

  private static final String AVG = "avg";

  private static final String STD_DEV = "stddev";

  private final boolean includeBoundaryRelated;

  private final boolean includeAverageRelated;

  private final Fields aggregateFields;

  public AggregateReportingPlanElement(FileSchema fileSchema, boolean includeBoundaryRelated,
      boolean includeAverageRelated, SummaryType summaryType, List<Field> fields) {
    super(fileSchema, fields, summaryType);

    this.includeBoundaryRelated = includeBoundaryRelated;
    this.includeAverageRelated = includeBoundaryRelated ? includeAverageRelated : false;
    this.aggregateFields = buildFields(fields);
  }

  private Fields buildFields(List<Field> fields) {// TODO: better way?
    Fields averageFields = new Fields();
    for(Field field : fields) {
      averageFields = averageFields.append(new Fields(field.getName()));
    }
    return averageFields;
  }

  @Override
  public Pipe report(Pipe pipe) {
    pipe = new Each(pipe, aggregateFields, new FieldToValueFunction(aggregateFields.size()), FIELD_VALUE_FIELDS);

    // TODO: chain every instead of splitting?

    Pipe completeness = new GroupBy(pipe, FIELD_FIELDS);
    completeness = new Every(completeness, VALUE_FIELDS, new CompletenessBuffer(), new Fields(FIELD, NULLS, POPULATED));

    Pipe min = null;
    Pipe max = null;
    Pipe avgRelated = null;
    if(includeBoundaryRelated) {

      // TODO: consider using apache commons' DescriptiveStatistics for min and max as well?
      min = new Pipe(buildSubPipeName(MIN), pipe);
      min = new GroupBy(min, FIELD_FIELDS);
      min = new Every(min, VALUE_FIELDS, new Min(VALUE_FIELDS), FIELD_VALUE_FIELDS);
      min = new Rename(min, FIELD_VALUE_FIELDS, new Fields(renameField(MIN), MIN));

      max = new Pipe(buildSubPipeName(MAX), pipe);
      max = new GroupBy(max, FIELD_FIELDS);
      max = new Every(max, VALUE_FIELDS, new Max(VALUE_FIELDS), FIELD_VALUE_FIELDS);
      max = new Rename(max, FIELD_VALUE_FIELDS, new Fields(renameField(MAX), MAX));

      if(includeAverageRelated) {
        avgRelated = new Pipe(buildSubPipeName(AVG), pipe);
        avgRelated = new GroupBy(avgRelated, FIELD_FIELDS);
        avgRelated = new Every(avgRelated, VALUE_FIELDS, new AverageRelatedBuffer(), new Fields(FIELD, AVG, STD_DEV));
        avgRelated = new Rename(avgRelated, FIELD_FIELDS, new Fields(renameField(AVG)));
      }
    }

    // TODO: use MixedJoin instead + select output fields directly instead of using Retain afterwards
    if(includeBoundaryRelated) {
      pipe = new CoGroup(completeness, FIELD_FIELDS, min, new Fields(renameField(MIN)), new InnerJoin());
      pipe = new CoGroup(pipe, FIELD_FIELDS, max, new Fields(renameField(MAX)), new InnerJoin());
      if(includeAverageRelated) {
        pipe = new CoGroup(pipe, FIELD_FIELDS, avgRelated, new Fields(renameField(AVG)), new InnerJoin());
      }
    } else {
      pipe = completeness;
    }

    Fields fieldsOfInterest = new Fields(FIELD, NULLS, POPULATED);
    if(includeBoundaryRelated) {
      fieldsOfInterest = fieldsOfInterest.append(new Fields(MIN, MAX));
      if(includeAverageRelated) {
        fieldsOfInterest = fieldsOfInterest.append(new Fields(AVG, STD_DEV));
      }
    }

    pipe = new Retain(pipe, fieldsOfInterest);
    pipe =
        new Each(pipe, fieldsOfInterest, new AggregateSummaryFunction(includeBoundaryRelated, includeAverageRelated),
            REPORT_FIELDS);

    return pipe;
  }

  private String renameField(String field) {
    return field + "_" + FIELD;
  }

  @SuppressWarnings("rawtypes")
  public static final class FieldToValueFunction extends BaseOperation implements Function { // TODO: cascading built-in
                                                                                             // way?
    FieldToValueFunction(int numArgs) {
      super(numArgs, new Fields(FIELD, VALUE));
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      TupleEntry tupleEntry = functionCall.getArguments();
      Fields fields = tupleEntry.getFields();
      checkState(numArgs != 0 && numArgs == fields.size());
      for(int i = 0; i < numArgs; i++) {
        Object object = tupleEntry.getObject(i);
        functionCall.getOutputCollector().add(new Tuple(fields.get(i), object));
      }
    }
  }

  @SuppressWarnings("rawtypes")
  public static final class CompletenessBuffer extends BaseOperation implements Buffer {
    public CompletenessBuffer() {
      super(1, new Fields(NULLS, POPULATED));
    }

    @Override
    public void operate(FlowProcess flowProcess, BufferCall bufferCall) {
      @SuppressWarnings("unchecked")
      Iterator<TupleEntry> tuples = bufferCall.getArgumentsIterator();
      int nulls = 0;
      int populated = 0;
      while(tuples.hasNext()) { // TODO: combine with FrequencySummaryBuffer?
        TupleEntry tuple = tuples.next();
        String value = tuple.getString(0);
        if(value == null || value.isEmpty()) {
          nulls++;
        } else {
          populated++;
        }
      }
      bufferCall.getOutputCollector().add(new Tuple(nulls, populated));
    }
  }

  /**
   * Computes average and standard deviation
   */
  @SuppressWarnings("rawtypes")
  public static final class AverageRelatedBuffer extends BaseOperation implements Buffer {

    AverageRelatedBuffer() {
      super(1, new Fields(AVG, STD_DEV));
    }

    @Override
    public void operate(FlowProcess flowProcess, BufferCall bufferCall) {
      @SuppressWarnings("unchecked")
      Iterator<TupleEntry> tuples = bufferCall.getArgumentsIterator();
      int length = 0;
      SummaryStatistics stats = new SummaryStatistics();
      while(tuples.hasNext()) {
        Double number = tuples.next().getDouble(0);
        if(number != null && Double.isNaN(number) == false) {
          stats.addValue(number);
          length++;
        }
      }
      if(length > 0) {
        double mean = Double.valueOf(String.format("%.2f", stats.getMean()));
        double standardDeviation = Double.valueOf(String.format("%.2f", stats.getStandardDeviation()));
        bufferCall.getOutputCollector().add(new Tuple(mean, standardDeviation));
      } else {
        bufferCall.getOutputCollector().add(new Tuple(null, null));
      }
    }
  }

  @SuppressWarnings("rawtypes")
  public static final class AggregateSummaryFunction extends BaseOperation implements Function {
    private final boolean includeBoundaryRelated;

    private final boolean includeAverageRelated;

    public AggregateSummaryFunction(boolean includeBoundaryRelated, boolean includeAverageRelated) {
      super((includeBoundaryRelated && includeAverageRelated ? 7 : (includeBoundaryRelated ? 5 : 3)), REPORT_FIELDS);
      this.includeBoundaryRelated = includeBoundaryRelated;
      this.includeAverageRelated = includeAverageRelated;
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      FieldSummary fs = new FieldSummary();
      TupleEntry tupleEntry = functionCall.getArguments();

      fs.field = tupleEntry.getString(0);
      fs.nulls = tupleEntry.getInteger(1);
      fs.populated = tupleEntry.getInteger(2);

      if(includeBoundaryRelated) {
        fs.summary.put(MIN, tupleEntry.getString(3));
        fs.summary.put(MAX, tupleEntry.getString(4));
        if(includeAverageRelated) {
          fs.summary.put(AVG, tupleEntry.getString(5));
          fs.summary.put(STD_DEV, tupleEntry.getString(6));
        }
      }

      functionCall.getOutputCollector().add(new Tuple(fs));
    }
  }

  static final class CompletenessPlanElement extends AggregateReportingPlanElement {
    public CompletenessPlanElement(FileSchema fileSchema, List<Field> fields) {
      super(fileSchema, false, false, SummaryType.COMPLETENESS, fields);
    }

    @Override
    public ReportCollector getCollector() {
      // FlowType is always Internal for Summary
      return new SummaryReportCollector(this.fileSchema, FlowType.INTERNAL);
    }
  }

  static final class MinMaxPlanElement extends AggregateReportingPlanElement {
    public MinMaxPlanElement(FileSchema fileSchema, List<Field> fields) {
      super(fileSchema, true, false, SummaryType.MIN_MAX, fields);
    }

    @Override
    public ReportCollector getCollector() {
      // FlowType is always Internal for Summary
      return new SummaryReportCollector(this.fileSchema, FlowType.INTERNAL);
    }
  }

  static final class AveragePlanElement extends AggregateReportingPlanElement {
    public AveragePlanElement(FileSchema fileSchema, List<Field> fields) {
      super(fileSchema, true, true, SummaryType.AVERAGE, fields);
    }

    @Override
    public ReportCollector getCollector() {
      // FlowType is always Internal for Summary
      return new SummaryReportCollector(this.fileSchema, FlowType.INTERNAL);
    }
  }
}
