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
import cascading.flow.FlowDef;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class DefaultPlanner implements Planner {

  private final CascadingStrategy cascadingStrategy;

  private final Map<String, FileSchemaPlanner> plans = Maps.newHashMap();

  public DefaultPlanner(CascadingStrategy cascadingStrategy) {
    this.cascadingStrategy = cascadingStrategy;
  }

  @Override
  public void prepare(FileSchema schema) {
    this.plans.put(schema.getName(), new DefaultFileSchemaPlanner(this, schema));
  }

  @Override
  public List<FileSchemaPlanner> getSchemaPlans() {
    return ImmutableList.copyOf(plans.values());
  }

  @Override
  public FileSchemaPlanner getSchemaPlan(String schema) {
    FileSchemaPlanner schemaPlan = this.plans.get(schema);
    if(schemaPlan == null) throw new IllegalStateException("no plan for " + schema);
    return schemaPlan;
  }

  @Override
  public Cascade plan() {
    CascadeDef def = new CascadeDef();
    for(FileSchemaPlanner plan : getSchemaPlans()) {
      def.addFlow(cascadingStrategy.getFlowConnector().connect(plan.internalFlow()));
      FlowDef external = plan.externalFlow();
      if(external != null) {
        def.addFlow(cascadingStrategy.getFlowConnector().connect(external));
      }
    }
    return new CascadeConnector().connect(def);
  }

  @Override
  public CascadingStrategy getCascadingStrategy() {
    return cascadingStrategy;
  }
}
