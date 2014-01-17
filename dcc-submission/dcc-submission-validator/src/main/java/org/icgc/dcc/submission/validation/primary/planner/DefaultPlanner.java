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
package org.icgc.dcc.submission.validation.primary.planner;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.SubmissionDataType;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.primary.core.FlowType;
import org.icgc.dcc.submission.validation.primary.core.Plan;
import org.icgc.dcc.submission.validation.primary.core.PlanElement;
import org.icgc.dcc.submission.validation.primary.core.RestrictionType;
import org.icgc.dcc.submission.validation.primary.visitor.ErrorReportingPlanningVisitor;
import org.icgc.dcc.submission.validation.primary.visitor.ExternalRestrictionPlanningVisitor;
import org.icgc.dcc.submission.validation.primary.visitor.InternalRestrictionPlanningVisitor;
import org.icgc.dcc.submission.validation.primary.visitor.PlanningVisitor;
import org.icgc.dcc.submission.validation.primary.visitor.SummaryPlanningVisitor;
import org.icgc.dcc.submission.validation.primary.visitor.ValueTypePlanningVisitor;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

@Slf4j
public class DefaultPlanner implements Planner {

  private final Set<RestrictionType> restrictionTypes;

  @Inject
  public DefaultPlanner(@NonNull Set<RestrictionType> restrictionTypes) {
    this.restrictionTypes = restrictionTypes;
  }

  @Override
  public Plan plan(@NonNull String projectKey, @NonNull Collection<SubmissionDataType> dataTypes,
      @NonNull PlatformStrategy strategy, @NonNull Dictionary dictionary) {
    val plan = new Plan(projectKey, dictionary, strategy);

    log.info("Including flow planners for '{}'", projectKey);
    includePlanners(plan, projectKey, dataTypes, strategy, dictionary);

    log.info("Applying planning visitors for '{}'", projectKey);
    applyVisitors(plan, projectKey);

    return plan;
  }

  private static void includePlanners(Plan plan, String projectKey, Collection<SubmissionDataType> dataTypes,
      PlatformStrategy strategy, Dictionary dictionary) {
    val systemDirectory = strategy.getSystemDirectory();

    // Selective validation filtering
    val fileSchemata = dictionary.getFileSchemata(dataTypes);
    for (val fileSchema : fileSchemata) {
      val fileSchemaDirectory = strategy.getFileSchemaDirectory();
      val fileSchemaName = fileSchema.getName();

      val include = fileSchemaDirectory.hasFile(fileSchema) || systemDirectory.hasFile(fileSchema);
      if (include) {
        log.info("Including file schema '{}' flow planners for '{}'", fileSchemaName, projectKey);
        plan.include(fileSchema,
            new DefaultInternalFlowPlanner(fileSchema),
            new DefaultExternalFlowPlanner(plan, fileSchema));
      } else {
        log.info("File schema '{}' has no matching datafile in submission directory '{}' for '{}'",
            new Object[] { fileSchemaName, fileSchemaDirectory.getDirectoryPath(), projectKey });
      }
    }
  }

  private void applyVisitors(Plan plan, String projectKey) {
    val visitors = createVisitors(restrictionTypes);
    for (val visitor : visitors) {
      log.info("Applying '{}' planning visitor to '{}'", visitor.getClass().getSimpleName(), projectKey);
      visitor.apply(plan);
    }
  }

  private static List<PlanningVisitor<? extends PlanElement>> createVisitors(Set<RestrictionType> restrictionTypes) {
    return ImmutableList.of(
        // Internal
        new ValueTypePlanningVisitor(), // Must happen before RangeRestriction
        new InternalRestrictionPlanningVisitor(restrictionTypes),

        // Reporting
        new SummaryPlanningVisitor(),
        new ErrorReportingPlanningVisitor(FlowType.INTERNAL),

        // External
        // Doesn't actually have any EXTERNAL restrictionTypes at the moment
        new ExternalRestrictionPlanningVisitor(restrictionTypes),
        new ErrorReportingPlanningVisitor(FlowType.EXTERNAL));
  }

}
