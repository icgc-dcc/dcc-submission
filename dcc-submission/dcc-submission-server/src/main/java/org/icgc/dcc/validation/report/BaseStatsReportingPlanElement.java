/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.MappingIterator;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.SummaryType;
import org.icgc.dcc.validation.CascadingStrategy;
import org.icgc.dcc.validation.FlowType;
import org.icgc.dcc.validation.PlanExecutionException;
import org.icgc.dcc.validation.ReportingPlanElement;
import org.icgc.dcc.validation.cascading.CompletenessBy;
import org.icgc.dcc.validation.cascading.TupleStates;

import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

abstract class BaseStatsReportingPlanElement implements ReportingPlanElement {

  static final String FIELD = "field";

  static final String VALUE = "value";

  static final String REPORT = "report";

  static final Fields FIELD_FIELDS = new Fields(FIELD);

  static final Fields VALUE_FIELDS = new Fields(VALUE);

  static final Fields FIELD_VALUE_FIELDS = new Fields(FIELD, VALUE);

  static final Fields REPORT_FIELDS = new Fields(REPORT);

  protected final FileSchema fileSchema;

  protected final FlowType flowType;

  protected final SummaryType summaryType;

  protected final List<Field> fields;

  protected BaseStatsReportingPlanElement(FileSchema fileSchema, List<Field> fields, SummaryType summaryType,
      FlowType flowType) {
    this.fileSchema = fileSchema;
    this.fields = fields;
    this.summaryType = summaryType;
    this.flowType = flowType;
  }

  public Pipe keepStructurallyValidTuples(Pipe pipe) {
    return new Each(pipe, TupleStates.keepStructurallyValidTuplesFilter());
  }

  @Override
  public String getName() {
    return this.summaryType != null ? this.summaryType.getDescription() : CompletenessBy.COMPLETENESS;
  }

  @Override
  public String describe() {
    return String.format("%s-%s", this.getName(), Iterables.transform(fields, new Function<Field, String>() {
      @Override
      public String apply(Field input) {
        return input.getName();
      }
    }));
  }

  protected String buildSubPipeName(String prefix) {
    return fileSchema.getName() + "_" + prefix + "_" + "pipe";
  }

  public FileSchema getFileSchema() {
    return this.fileSchema;
  }

  public FlowType getFlowType() {
    return this.flowType;
  }

  @Override
  public ReportCollector getCollector() {
    return new SummaryReportCollector(this.fileSchema);
  }

  public static class FieldSummary {// TODO: use FieldReport instead?

    public String field;

    public long nulls;

    public long missing;

    public long populated;

    /**
     * Depending on the summary type, the map will either contain:<br/>
     * - aggregate types ("unique_count", "min", "max", ...) and their associate values, or<br/>
     * - data values as keys and their associated frequency, or<br/>
     * <br/>
     * TODO: consider using 2 different variables instead, as their conceptually different? The only thing they have in
     * common is that they're both displayed the same way <br/>
     * TODO: rename as it's confusing: SummaryPlanElement uses this summary variable, yet the two "summary" notions here
     * aren't exactly semantically equivalent (FrequencyPlanElement also uses that summary variable, yet isn't a
     * SummaryPlanElement)
     */
    public Map<String, Object> summary = Maps.newLinkedHashMap(); //

    @Override
    public String toString() { // for testing only for now
      return Objects.toStringHelper(FieldSummary.class).add("field", field).add("nulls", nulls).add("missing", missing)
          .add("populated", populated).add("summary", summary).toString();
    }
  }

  class SummaryReportCollector implements ReportCollector {

    private final FileSchema fileSchema;

    public SummaryReportCollector(FileSchema fileSchema) {
      this.fileSchema = fileSchema;
    }

    @Override
    public Outcome collect(CascadingStrategy strategy, SchemaReport report) {
      try {
        InputStream src = strategy.readReportTap(getFileSchema(), getFlowType(), getName());

        report.setName(strategy.path(getFileSchema()).getName());

        ObjectMapper mapper = new ObjectMapper();

        MappingIterator<FieldSummary> fieldSummary = mapper.reader().withType(FieldSummary.class).readValues(src);
        while(fieldSummary.hasNext()) {
          FieldReport freport = FieldReport.convert(fieldSummary.next());
          Field field = this.fileSchema.field(freport.getName()).get();
          freport.setLabel(field.getLabel());
          freport.setType(field.getSummaryType());

          report.addFieldReport(freport);
        }

      } catch(Exception e) {
        throw new PlanExecutionException(e);
      }

      return Outcome.PASSED;
    }
  }
}
