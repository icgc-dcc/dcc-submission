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
package org.icgc.dcc.validation.plan;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.icgc.dcc.model.dictionary.Dictionary;
import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.validation.FileSchemaDirectory;
import org.icgc.dcc.validation.PlanningVisitor;
import org.icgc.dcc.validation.RestrictionType;
import org.icgc.dcc.validation.visitor.RelationPlanningVisitor;
import org.icgc.dcc.validation.visitor.RestrictionPlanningVisitor;
import org.icgc.dcc.validation.visitor.UniqueFieldsPlanningVisitor;
import org.icgc.dcc.validation.visitor.ValueTypePlanningVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.cascade.CascadeDef;
import cascading.flow.FlowDef;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class DefaultPlanner implements Planner {

  private static final Logger log = LoggerFactory.getLogger(DefaultPlanner.class);

  private final List<PlanningVisitor> internalFlowVisitors;

  private final List<PlanningVisitor> externalFlowVisitors;

  private final CascadingStrategy cascadingStrategy;

  private final Map<PlanPhase, Planners> plans = Maps.newHashMap();

  @Inject
  public DefaultPlanner(Set<RestrictionType> restrictionTypes, CascadingStrategy cascadingStrategy) {
    checkArgument(restrictionTypes != null);
    checkArgument(cascadingStrategy != null);
    this.cascadingStrategy = cascadingStrategy;
    internalFlowVisitors =
        ImmutableList.of(new ValueTypePlanningVisitor(), new UniqueFieldsPlanningVisitor(),
            new RestrictionPlanningVisitor(PlanPhase.INTERNAL, restrictionTypes));
    externalFlowVisitors =
        ImmutableList.of(new RelationPlanningVisitor(), new RestrictionPlanningVisitor(PlanPhase.EXTERNAL,
            restrictionTypes));
  }

  @Override
  public InternalFlowPlanner getInternalFlow(String schema) {
    return (InternalFlowPlanner) getSchemaPlan(PlanPhase.INTERNAL, schema);
  }

  @Override
  public ExternalFlowPlanner getExternalFlow(String schema) {
    return (ExternalFlowPlanner) getSchemaPlan(PlanPhase.EXTERNAL, schema);
  }

  @Override
  public Cascade plan(FileSchemaDirectory directory, Dictionary dictionary) {
    List<FileSchema> plannedSchema = Lists.newArrayList();
    for(PlanPhase phase : PlanPhase.values()) {
      plans.put(phase, new Planners(phase));
    }
    for(FileSchema fileSchema : dictionary.getFiles()) {
      if(directory.hasFile(fileSchema)) {
        plannedSchema.add(fileSchema);
        plans.get(PlanPhase.INTERNAL).planFor(fileSchema.getName(), new DefaultInternalFlowPlanner(this, fileSchema));
        plans.get(PlanPhase.EXTERNAL).planFor(fileSchema.getName(), new DefaultExternalFlowPlanner(this, fileSchema));
      }
    }
    plan(plannedSchema, internalFlowVisitors);
    plan(plannedSchema, externalFlowVisitors);

    CascadeDef def = new CascadeDef();
    for(Planners p : plans.values()) {
      for(FileSchemaFlowPlanner plan : p.planners.values()) {
        FlowDef flowDef = plan.plan();
        if(flowDef != null) {
          def.addFlow(cascadingStrategy.getFlowConnector().connect(flowDef));
        }
      }
    }
    return new CascadeConnector().connect(def);
  }

  @Override
  public CascadingStrategy getCascadingStrategy() {
    return cascadingStrategy;
  }

  private void plan(List<FileSchema> plannedSchema, List<PlanningVisitor> visitors) {
    for(FileSchema fs : plannedSchema) {
      for(PlanningVisitor visitor : visitors) {
        fs.accept(visitor);
        for(PlanElement element : visitor.getElements()) {
          log.info("[{}]: applying plan element {}", fs.getName(), element.describe());
          getSchemaPlan(visitor.getPhase(), fs.getName()).apply(element);
        }
      }
    }
  }

  private FileSchemaFlowPlanner getSchemaPlan(PlanPhase phase, String schema) {
    FileSchemaFlowPlanner schemaPlan = this.plans.get(phase).planner(schema);
    if(schemaPlan == null) throw new IllegalStateException("no plan for " + schema);
    return schemaPlan;
  }

  private class Planners {

    private final PlanPhase phase;

    private final Map<String, FileSchemaFlowPlanner> planners = Maps.newHashMap();

    public Planners(PlanPhase phase) {
      this.phase = phase;
    }

    FileSchemaFlowPlanner planFor(String name, FileSchemaFlowPlanner planner) {
      this.planners.put(name, planner);
      return planner;
    }

    FileSchemaFlowPlanner planner(String name) {
      FileSchemaFlowPlanner schemaPlan = this.planners.get(name);
      if(schemaPlan == null) throw new IllegalStateException("no plan for " + name + " in phase " + phase);
      return schemaPlan;
    }

  }
}
