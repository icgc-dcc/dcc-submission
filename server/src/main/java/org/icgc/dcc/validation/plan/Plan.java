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

import java.util.List;
import java.util.Map;

import org.icgc.dcc.model.dictionary.FileSchema;

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

  public void include(FileSchema fileSchema, InternalFlowPlanner internal, ExternalFlowPlanner external) {
    this.plannedSchema.add(fileSchema);
    this.internalPlanners.put(fileSchema.getName(), internal);
    this.externalPlanners.put(fileSchema.getName(), external);
  }

  public InternalFlowPlanner getInternalFlow(String schema) {
    InternalFlowPlanner schemaPlan = internalPlanners.get(schema);
    if(schemaPlan == null) throw new PlannerException("no plan available for schema [" + schema + "]");
    return schemaPlan;
  }

  public Iterable<InternalFlowPlanner> getInternalFlows() {
    return Iterables.unmodifiableIterable(internalPlanners.values());
  }

  public ExternalFlowPlanner getExternalFlow(String schema) {
    ExternalFlowPlanner schemaPlan = externalPlanners.get(schema);
    if(schemaPlan == null) throw new PlannerException("no plan available for schema [" + schema + "]");
    return schemaPlan;
  }

  public Iterable<ExternalFlowPlanner> getExternalFlows() {
    return Iterables.unmodifiableIterable(externalPlanners.values());
  }

  public Cascade connect(CascadingStrategy cascadingStrategy) {
    CascadeDef cascade = new CascadeDef();
    for(FileSchemaFlowPlanner plan : Iterables.concat(internalPlanners.values(), externalPlanners.values())) {
      Flow<?> flow = plan.connect(cascadingStrategy);
      if(flow != null) {
        cascade.addFlow(flow);
      }
    }
    return new CascadeConnector().connect(cascade);
  }
}
