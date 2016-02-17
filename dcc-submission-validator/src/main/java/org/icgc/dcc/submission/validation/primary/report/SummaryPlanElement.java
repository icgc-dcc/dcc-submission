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

import static java.lang.String.format;
import static org.icgc.dcc.submission.dictionary.model.SummaryType.AVERAGE;
import static org.icgc.dcc.submission.dictionary.model.SummaryType.MIN_MAX;
import static org.icgc.dcc.submission.validation.cascading.CompletenessBy.MISSING;
import static org.icgc.dcc.submission.validation.cascading.CompletenessBy.NULLS;
import static org.icgc.dcc.submission.validation.cascading.CompletenessBy.POPULATED;
import static org.icgc.dcc.submission.validation.cascading.MinMaxBy.MAX;
import static org.icgc.dcc.submission.validation.cascading.MinMaxBy.MIN;
import static org.icgc.dcc.submission.validation.cascading.ValidationFields.STATE_FIELD_NAME;
import static org.icgc.dcc.submission.validation.primary.report.DeviationBy.AVG;
import static org.icgc.dcc.submission.validation.primary.report.DeviationBy.STDDEV;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.submission.dictionary.model.SummaryType;
import org.icgc.dcc.submission.validation.cascading.CompletenessBy;
import org.icgc.dcc.submission.validation.cascading.MinMaxBy;
import org.icgc.dcc.submission.validation.primary.core.FlowType;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.operation.Insert;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.pipe.assembly.AggregateBy;
import cascading.pipe.assembly.Discard;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public abstract class SummaryPlanElement extends BaseStatsReportingPlanElement {

  protected static String summaryFieldName(String originalFieldName, String summaryName) {
    return format("%s#%s", originalFieldName, summaryName);
  }

  protected SummaryPlanElement(
      FlowType flowType, Optional<SummaryType> optionalSummaryType,
      String fileName, Map<String, FieldStatDigest> fieldStatDigests) {
    super(flowType, optionalSummaryType, fileName, fieldStatDigests);
  }

  @Override
  public Pipe report(Pipe pipe) {
    pipe = keepStructurallyValidTuples(pipe);

    ArrayList<AggregateBy> summaries = new ArrayList<AggregateBy>();
    for (String fieldName : fieldNames) {
      Iterables.addAll(summaries, collectAggregateBys(fieldName));
    }

    // This is highly inefficient. This adds a constant to every row so we can group on it so that the AggregateBy
    // instances see all values.
    // Alternatively, we could set a random number to each row and group on this, each group would then produce
    // intermediate result (sum, count for average) and another pipe could then process this smaller set as done here.
    Fields constantField = new Fields("__constant__");
    pipe = new Each(pipe, new Insert(constantField, "1"), Fields.ALL);
    pipe = new AggregateBy(pipe, constantField, Iterables.toArray(summaries, AggregateBy.class)); // only one group
                                                                                                  // emerges from
                                                                                                  // grouping on
                                                                                                  // "__constant__"
    pipe = new Discard(pipe, constantField);
    pipe = new Each(
        pipe,
        new SummaryFunction(fieldNames, summaryFields()),
        REPORT_FIELDS);
    return pipe;
  }

  protected AggregateBy makeDeviation(String fieldName) {
    return new DeviationBy(
        new Fields(fieldName),
        fieldName,
        new Fields(
            summaryFieldName(fieldName, AVG),
            summaryFieldName(fieldName, STDDEV)));
  }

  protected AggregateBy makeMinMax(String fieldName) {
    return new MinMaxBy(
        new Fields(fieldName),
        new Fields(
            summaryFieldName(fieldName, MIN),
            summaryFieldName(fieldName, MAX)));
  }

  protected AggregateBy makeCompleteness(String fieldName) {
    return new CompletenessBy(
        new Fields(fieldName, STATE_FIELD_NAME),
        new Fields(
            summaryFieldName(fieldName, NULLS),
            summaryFieldName(fieldName, MISSING),
            summaryFieldName(fieldName, POPULATED)));
  }

  /**
   * Returns a iterable of {@code AggregateBy} instances to be applied to every tuples in the one group that results in
   * grouping on "__constant__"
   * <p>
   * See TODO about that __constant__ field.
   */
  protected abstract Iterable<AggregateBy> collectAggregateBys(String fieldName);

  /**
   * Returns a list of aggregate types such as min, max, average and stddev (possibly empty).
   */
  protected abstract Iterable<String> summaryFields();

  protected Pipe average(String field, Pipe pipe) {
    return pipe;
  }

  /**
   * Input contains only 1 tuple like:<br/>
   * <br/>
   * <table>
   * <tr>
   * <td>"f1#nulls"</td>
   * <td>"f1#missing"</td>
   * <td>...</td>
   * <td>"f2#nulls"</td>
   * <td>"f2#missing"</td>
   * <td>...</td>
   * <tr>
   * <td>4</td>
   * <td>5</td>
   * <td>...</td>
   * <td>0</td>
   * <td>3</td>
   * <td>...</td>
   * </tr>
   * </table>
   * <br/>
   * <br/>
   * And output one tuple per data-field, each tuple having only one "report" {@code Fields} like:<br/>
   * <br/>
   * <table>
   * <tr>
   * <td>"report"</td>
   * </tr>
   * <tr>
   * <td>{ "field": "f1", "nulls": 4, "missing": 5, ...}</td>
   * </tr>
   * <tr>
   * <td>{ "field": "f2", "nulls": 0, "missing": 3, ...}</td>
   * </tr>
   * </table>
   */
  @SuppressWarnings("rawtypes")
  public static class SummaryFunction extends BaseOperation implements Function {

    private final List<String> fieldNames;

    private final List<String> summaryFields;

    public SummaryFunction(List<String> fieldNames, Iterable<String> summaryFields) {
      super(REPORT_FIELDS);
      this.fieldNames = fieldNames;
      this.summaryFields = ImmutableList.copyOf(summaryFields);
    }

    @Override
    public void operate(FlowProcess flowProcess, FunctionCall functionCall) {
      TupleEntry te = functionCall.getArguments();
      for (String fieldName : fieldNames) {
        FieldSummary fs = new FieldSummary();
        fs.field = fieldName;
        fs.nulls = te.getInteger(summaryFieldName(fieldName, CompletenessBy.NULLS));
        fs.missing = te.getInteger(summaryFieldName(fieldName, CompletenessBy.MISSING));
        fs.populated = te.getInteger(summaryFieldName(fieldName, CompletenessBy.POPULATED));
        for (String summaryField : summaryFields) {
          fs.summary.put(summaryField, te.getObject(summaryFieldName(fieldName, summaryField)));
        }
        functionCall.getOutputCollector().add(new Tuple(fs));
      }
    }
  }

  public static class CompletenessPlanElement extends SummaryPlanElement {

    public CompletenessPlanElement(FlowType flowType, String fileName,
        Map<String, FieldStatDigest> fieldStatDigests) {
      super(flowType, Optional.<SummaryType> absent(), fileName, fieldStatDigests);
    }

    @Override
    protected Iterable<AggregateBy> collectAggregateBys(String fieldName) {
      return ImmutableList.of(makeCompleteness(fieldName));
    }

    @Override
    protected Iterable<String> summaryFields() {
      return ImmutableList.of();
    }
  }

  public static class MinMaxPlanElement extends SummaryPlanElement {

    public MinMaxPlanElement(FlowType flowType, String fileName,
        Map<String, FieldStatDigest> fieldStatDigests) {
      super(flowType, Optional.of(MIN_MAX), fileName, fieldStatDigests);
    }

    @Override
    protected Iterable<AggregateBy> collectAggregateBys(String fieldName) {
      return ImmutableList.of(makeMinMax(fieldName), makeCompleteness(fieldName));
    }

    @Override
    protected Iterable<String> summaryFields() {
      return ImmutableList.of(MinMaxBy.MIN, MinMaxBy.MAX);
    }
  }

  public static class AveragePlanElement extends SummaryPlanElement {

    public AveragePlanElement(FlowType flowType, String fileName,
        Map<String, FieldStatDigest> fieldStatDigests) {
      super(flowType, Optional.of(AVERAGE), fileName, fieldStatDigests);
    }

    @Override
    protected Iterable<AggregateBy> collectAggregateBys(String fieldName) {
      return ImmutableList.of(makeDeviation(fieldName), makeMinMax(fieldName), makeCompleteness(fieldName));
    }

    @Override
    protected Iterable<String> summaryFields() {
      return ImmutableList.of(MinMaxBy.MIN, MinMaxBy.MAX, DeviationBy.AVG, DeviationBy.STDDEV);
    }
  }

}
