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

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.model.dictionary.Relation;
import org.icgc.dcc.model.dictionary.Restriction;
import org.icgc.dcc.model.dictionary.visitor.BaseDictionaryVisitor;
import org.icgc.dcc.model.dictionary.visitor.DictionaryVisitor;
import org.icgc.dcc.validation.RestrictionType;
import org.icgc.dcc.validation.restriction.RelationPlanElement;
import org.icgc.dcc.validation.restriction.ValueTypePlanElement;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.cascade.CascadeDef;
import cascading.flow.FlowDef;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class DefaultPlanner implements Planner {

  private final Set<? extends RestrictionType> restrictionTypes;

  private final List<FileSchema> plannedSchema = Lists.newLinkedList();

  private final CascadingStrategy cascadingStrategy;

  private final Map<PlanPhase, Planners> plans = Maps.newHashMap();

  @Inject
  public DefaultPlanner(Set<RestrictionType> restrictionTypes, CascadingStrategy cascadingStrategy) {
    checkArgument(restrictionTypes != null);
    checkArgument(cascadingStrategy != null);
    this.restrictionTypes = restrictionTypes;
    this.cascadingStrategy = cascadingStrategy;
    for(PlanPhase phase : PlanPhase.values()) {
      plans.put(phase, new Planners(phase));
    }
  }

  @Override
  public void prepare(FileSchema schema) {
    plannedSchema.add(schema);
    plans.get(PlanPhase.INTERNAL).planFor(schema.getName(), new DefaultInternalFlowPlanner(this, schema));
    plans.get(PlanPhase.EXTERNAL).planFor(schema.getName(), new DefaultExternalFlowPlanner(this, schema));
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
  public Cascade plan() {
    planInternalFlow();
    planExternalFlow();

    CascadeDef def = new CascadeDef();
    for(Planners p : plans.values()) {
      for(FileSchemaPlanner plan : p.planners.values()) {
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

  private void planInternalFlow() {
    for(FileSchema s : plannedSchema) {
      for(DictionaryVisitor visitor : ImmutableList.of(new ValueTypeVisitor(), new RestrictionsVisitor(
          PlanPhase.INTERNAL))) {
        s.accept(visitor);
      }
    }
  }

  private void planExternalFlow() {
    for(FileSchema s : plannedSchema) {
      for(DictionaryVisitor visitor : ImmutableList.of(new RestrictionsVisitor(PlanPhase.EXTERNAL),
          new RelationVisitor())) {
        s.accept(visitor);
      }
    }
  }

  private FileSchemaPlanner getSchemaPlan(PlanPhase phase, String schema) {
    FileSchemaPlanner schemaPlan = this.plans.get(phase).planner(schema);
    if(schemaPlan == null) throw new IllegalStateException("no plan for " + schema);
    return schemaPlan;
  }

  private class BasePlanningVisitor extends BaseDictionaryVisitor {

    private FileSchema fileSchema;

    private Field field;

    @Override
    public void visit(FileSchema fileSchema) {
      this.fileSchema = fileSchema;
    }

    @Override
    public void visit(Field field) {
      this.field = field;
    }

    public Field getField() {
      return field;
    }

    public FileSchema getFileSchema() {
      return fileSchema;
    }

    protected void apply(PlanElement element) {
      getSchemaPlan(element.phase(), getFileSchema().getName()).apply(element);
    }
  }

  private class ValueTypeVisitor extends BasePlanningVisitor {

    @Override
    public void visit(Field field) {
      apply(new ValueTypePlanElement(field));
    }
  }

  private class RestrictionsVisitor extends BasePlanningVisitor {

    private final PlanPhase phase;

    RestrictionsVisitor(PlanPhase phase) {
      this.phase = phase;
    }

    @Override
    public void visit(Restriction restriction) {
      for(RestrictionType type : restrictionTypes) {
        if(type.builds(restriction.getType())) {
          PlanElement element = type.build(getField(), restriction);
          if(element.phase() == phase) {
            apply(element);
          }
        }
      }
    }
  }

  private class RelationVisitor extends BasePlanningVisitor {

    @Override
    public void visit(Relation relation) {
      apply(new RelationPlanElement(getFileSchema(), relation));
    }

  }

  private class Planners {

    private final PlanPhase phase;

    private final Map<String, FileSchemaPlanner> planners = Maps.newHashMap();

    public Planners(PlanPhase phase) {
      this.phase = phase;
    }

    FileSchemaPlanner planFor(String name, FileSchemaPlanner planner) {
      this.planners.put(name, planner);
      return planner;
    }

    FileSchemaPlanner planner(String name) {
      FileSchemaPlanner schemaPlan = this.planners.get(name);
      if(schemaPlan == null) throw new IllegalStateException("no plan for " + name + " in phase " + phase);
      return schemaPlan;
    }

  }
}
