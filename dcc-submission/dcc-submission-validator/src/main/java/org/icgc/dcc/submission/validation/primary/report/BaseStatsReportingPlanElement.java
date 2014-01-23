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
package org.icgc.dcc.submission.validation.primary.report;

import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static org.icgc.dcc.submission.validation.cascading.CompletenessBy.COMPLETENESS;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.submission.dictionary.model.SummaryType;
import org.icgc.dcc.submission.validation.cascading.TupleStates;
import org.icgc.dcc.submission.validation.core.FieldReport;
import org.icgc.dcc.submission.validation.core.ReportContext;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.primary.PlanExecutionException;
import org.icgc.dcc.submission.validation.primary.core.FlowType;
import org.icgc.dcc.submission.validation.primary.core.ReportingPlanElement;

import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;

public abstract class BaseStatsReportingPlanElement implements ReportingPlanElement {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  static final String FIELD = "field";
  static final String VALUE = "value";
  static final String REPORT = "report";
  static final Fields FIELD_FIELDS = new Fields(FIELD);
  static final Fields VALUE_FIELDS = new Fields(VALUE);
  static final Fields FIELD_VALUE_FIELDS = new Fields(FIELD, VALUE);
  static final Fields REPORT_FIELDS = new Fields(REPORT);

  protected final FlowType flowType;
  /**
   * May be none in the case of COMPLETENESS-only.
   */
  protected final Optional<SummaryType> optionalSummaryType;
  protected final String fileName;
  /**
   * Subset of fields (digests) from the file schema matching the summary type under consideration.
   */
  private final Map<String, FieldStatDigest> fieldStatDigests;

  protected final List<String> fieldNames;

  protected BaseStatsReportingPlanElement(
      FlowType flowType, Optional<SummaryType> optionalSummaryType,
      String fileName, Map<String, FieldStatDigest> fieldStatDigests) {
    this.flowType = flowType;
    this.optionalSummaryType = optionalSummaryType;
    this.fileName = fileName;
    this.fieldStatDigests = fieldStatDigests;

    // Extract the names
    this.fieldNames = newArrayList(transform(
        fieldStatDigests.values(),
        new Function<FieldStatDigest, String>() {

          @Override
          public String apply(FieldStatDigest fieldStatDigest) {
            return fieldStatDigest.getName();
          }
        }));
  }

  public Pipe keepStructurallyValidTuples(Pipe pipe) {
    return new Each(pipe, TupleStates.keepStructurallyValidTuplesFilter());
  }

  @Override
  public String getElementName() {
    return optionalSummaryType.isPresent() ?
        optionalSummaryType.get().getDescription() : COMPLETENESS;
  }

  @Override
  public String describe() {
    return String.format("%s-%s-%s", fileName, getElementName(), fieldNames);
  }

  protected String getSubPipeName(String prefix) {
    return format("%s_%s_pipe", fileName, prefix);
  }

  public FlowType getFlowType() {
    return this.flowType;
  }

  @Override
  public ReportCollector getCollector() {
    return new SummaryReportCollector(
        fileName,
        fieldStatDigests);
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

    private final String fileName;
    private final Map<String, FieldStatDigest> fieldStatDigests;

    public SummaryReportCollector(@NonNull String fileName, @NonNull Map<String, FieldStatDigest> fieldStatDigests) {
      this.fileName = fileName;
      this.fieldStatDigests = fieldStatDigests;
    }

    @Override
    public void collect(PlatformStrategy strategy, ReportContext context) {
      try {
        @Cleanup
        val reportIntputStream = getReportInputStream(strategy, fileName);
        val fieldSummary = getFieldSummaries(reportIntputStream);

        while (fieldSummary.hasNext()) {
          val fieldReport = FieldReport.convert(fieldSummary.next());
          val fieldName = fieldReport.getName();

          val fieldStatDigest = fieldStatDigests.get(fieldName);
          fieldReport.setLabel(fieldStatDigest.getLabel());
          fieldReport.setType(fieldStatDigest.getSummaryType()); // May be null

          context.reportField(fileName, fieldReport);
        }
      } catch (Exception e) {
        throw new PlanExecutionException(e);
      }
    }

    @SneakyThrows
    private InputStream getReportInputStream(PlatformStrategy strategy, String fileName) {
      return strategy.readReportTap(fileName, getFlowType(), getElementName());
    }

    @SneakyThrows
    private Iterator<FieldSummary> getFieldSummaries(InputStream reportIntputStream) {
      return MAPPER
          .reader()
          .withType(FieldSummary.class)
          .readValues(reportIntputStream);
    }

  }
}
