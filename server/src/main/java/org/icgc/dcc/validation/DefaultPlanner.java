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

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Set;

import org.icgc.dcc.model.dictionary.Dictionary;
import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.validation.visitor.ExternalRestrictionPlanningVisitor;
import org.icgc.dcc.validation.visitor.InternalRestrictionPlanningVisitor;
import org.icgc.dcc.validation.visitor.RelationPlanningVisitor;
import org.icgc.dcc.validation.visitor.UniqueFieldsPlanningVisitor;
import org.icgc.dcc.validation.visitor.ValueTypePlanningVisitor;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

public class DefaultPlanner implements Planner {

  private final List<InternalFlowPlanningVisitor> internalFlowVisitors;

  private final List<ExternalFlowPlanningVisitor> externalFlowVisitors;

  @Inject
  public DefaultPlanner(Set<RestrictionType> restrictionTypes) {
    checkArgument(restrictionTypes != null);
    internalFlowVisitors = ImmutableList.of(//
        new ValueTypePlanningVisitor(),//
        new UniqueFieldsPlanningVisitor(),//
        new InternalRestrictionPlanningVisitor(restrictionTypes));

    externalFlowVisitors = ImmutableList.<ExternalFlowPlanningVisitor> of(//
        new RelationPlanningVisitor(),//
        new ExternalRestrictionPlanningVisitor(restrictionTypes));
  }

  @Override
  public Plan plan(FileSchemaDirectory directory, Dictionary dictionary) {
    checkArgument(directory != null);
    checkArgument(dictionary != null);

    Plan plan = new Plan();
    for(FileSchema fileSchema : dictionary.getFiles()) {
      if(directory.hasFile(fileSchema)) {
        plan.include(fileSchema, new DefaultInternalFlowPlanner(plan, fileSchema), new DefaultExternalFlowPlanner(plan,
            fileSchema));
      }
    }
    for(InternalFlowPlanningVisitor visitor : internalFlowVisitors) {
      visitor.apply(plan);
    }
    for(ExternalFlowPlanningVisitor visitor : externalFlowVisitors) {
      visitor.apply(plan);
    }

    return plan;
  }

}
