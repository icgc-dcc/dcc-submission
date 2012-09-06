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
package org.icgc.dcc.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.report.Outcome;
import org.icgc.dcc.validation.report.SchemaReport;
import org.icgc.dcc.validation.report.SubmissionReport;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.cascade.CascadeDef;
import cascading.flow.Flow;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Plan {

  private final List<FileSchema> plannedSchema = Lists.newArrayList();

  private final Map<String, InternalFlowPlanner> internalPlanners = Maps.newHashMap();

  private final Map<String, ExternalFlowPlanner> externalPlanners = Maps.newHashMap();

  private Cascade cascade;

  private final CascadingStrategy cascadingStrategy;

  private Map<String, Object> params;

  public Plan(CascadingStrategy cascadingStrategy) {
    this.cascadingStrategy = cascadingStrategy;
  }

  public void include(FileSchema fileSchema, InternalFlowPlanner internal, ExternalFlowPlanner external) {
    this.plannedSchema.add(fileSchema);
    this.internalPlanners.put(fileSchema.getName(), internal);
    this.externalPlanners.put(fileSchema.getName(), external);
  }

  public InternalFlowPlanner getInternalFlow(String schema) {
    InternalFlowPlanner schemaPlan = internalPlanners.get(schema);
    if(schemaPlan == null) {
      this.params.put("schema", schema);
      throw new PlanningException(schema, ValidationErrorCode.MISSING_SCHEMA_ERROR, this.params);
    }
    return schemaPlan;
  }

  public Iterable<InternalFlowPlanner> getInternalFlows() {
    return Iterables.unmodifiableIterable(internalPlanners.values());
  }

  public ExternalFlowPlanner getExternalFlow(String schema) {
    ExternalFlowPlanner schemaPlan = externalPlanners.get(schema);
    if(schemaPlan == null) {
      this.params.put("schema", schema);
      throw new PlanningException(schema, ValidationErrorCode.MISSING_SCHEMA_ERROR, this.params);
    }
    return schemaPlan;
  }

  public Iterable<ExternalFlowPlanner> getExternalFlows() {
    return Iterables.unmodifiableIterable(externalPlanners.values());
  }

  public Iterable<? extends FileSchemaFlowPlanner> getFlows(FlowType type) {
    switch(type) {
    case INTERNAL:
      return Iterables.unmodifiableIterable(internalPlanners.values());
    case EXTERNAL:
      return Iterables.unmodifiableIterable(externalPlanners.values());
    default:
      throw new IllegalArgumentException();
    }
  }

  public void connect(CascadingStrategy cascadingStrategy) {
    CascadeDef cascade = new CascadeDef();
    Map<String, TupleState> errors = Maps.newLinkedHashMap();
    for(FileSchemaFlowPlanner planner : Iterables.concat(internalPlanners.values(), externalPlanners.values())) {
      try {
        Flow<?> flow = planner.connect(cascadingStrategy);
        if(flow != null) {
          cascade.addFlow(flow);
        }
      } catch(PlanningException e) {
        errors.put(e.getSchemaName(), e.getTupleState());
      }
    }
    if(errors.size() > 0) {
      throw new FatalPlanningException(errors);
    }

    this.cascade = new CascadeConnector().connect(cascade);
  }

  public Cascade getCascade() {
    return this.cascade;
  }

  public Outcome collect(SubmissionReport report) {
    Outcome result = Outcome.PASSED;
    Map<String, SchemaReport> schemaReports = new HashMap<String, SchemaReport>();
    for(FileSchemaFlowPlanner planner : Iterables.concat(internalPlanners.values(), externalPlanners.values())) {
      SchemaReport schemaReport = new SchemaReport();
      Outcome outcome = planner.collect(cascadingStrategy, schemaReport);
      if(outcome == Outcome.FAILED) {
        result = Outcome.FAILED;
      }
      if(!schemaReports.containsKey(schemaReport.getName())) {
        schemaReports.put(schemaReport.getName(), schemaReport);
      } else {
        // combine internal and external plans into one
        SchemaReport sreport = schemaReports.get(schemaReport.getName());

        if(schemaReport.getFieldReports() != null) {
          sreport.getFieldReports().addAll(schemaReport.getFieldReports());
        }
        if(sreport.getErrors() != null) {
          sreport.getErrors().addAll(schemaReport.getErrors());
        } else if(schemaReport.getErrors() != null) {
          sreport.setErrors(schemaReport.getErrors());
        }
      }
    }

    // remove empty report
    schemaReports.remove(null);

    report.setSchemaReports(new ArrayList<SchemaReport>(schemaReports.values()));
    return result;
  }

  public FileSchema getFileSchema(String name) {
    for(FileSchema schema : plannedSchema) {
      if(schema.getName().equals(name)) {
        return schema;
      }
    }
    return null;
  }
}
