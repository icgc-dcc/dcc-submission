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
package org.icgc.dcc.submission.validation.primary.planner;

import static org.icgc.dcc.submission.validation.primary.core.FlowType.ROW_BASED;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy;
import org.icgc.dcc.submission.validation.primary.core.Plan;
import org.icgc.dcc.submission.validation.primary.core.PlanElement;
import org.icgc.dcc.submission.validation.primary.core.RestrictionType;
import org.icgc.dcc.submission.validation.primary.visitor.ErrorReportingPlanningVisitor;
import org.icgc.dcc.submission.validation.primary.visitor.PlanningVisitor;
import org.icgc.dcc.submission.validation.primary.visitor.RowBasedRestrictionPlanningVisitor;
import org.icgc.dcc.submission.validation.primary.visitor.SummaryReportingPlanningVisitor;
import org.icgc.dcc.submission.validation.primary.visitor.ValueTypePlanningVisitor;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import cascading.pipe.Pipe;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject) )
public class Planner {

  @NonNull
  private final Set<RestrictionType> restrictionTypes;

  public Plan plan(@NonNull String projectKey, @NonNull Collection<DataType> dataTypes,
      @NonNull SubmissionPlatformStrategy platform, @NonNull Dictionary dictionary) {
    val plan = new Plan(projectKey, dictionary, platform);

    log.info("Including flow planners for '{}'", projectKey);
    includeFlowPlanners(plan, projectKey, dataTypes, platform, dictionary);

    log.info("Applying planning visitors for '{}'", projectKey);
    applyVisitors(plan, platform, projectKey);

    return plan;
  }

  /**
   * Include flow planners based on file presence.
   */

  private void includeFlowPlanners(
      Plan plan, String projectKey, Collection<DataType> dataTypes,
      SubmissionPlatformStrategy platform, Dictionary dictionary) {

    // Selective validation filtering
    val fileSchemata = dictionary.getFileSchemata(dataTypes);
    for (val fileSchema : fileSchemata) {
      val matchingFileNames = platform.listFileNames(fileSchema.getPattern());
      if (matchingFileNames.isEmpty()) {
        log.info("File schema '{}' has no matching datafile in submission directory for '{}'",
            new Object[] { fileSchema.getName(), projectKey });
      } else {
        for (val fileName : matchingFileNames) {
          log.info("Including file '{}' with file schema '{}' flow planners for '{}'",
              new Object[] { fileName, fileSchema.getName(), projectKey });
          plan.include(
              fileName,
              new DefaultRowBasedFlowPlanner(fileSchema, fileName));
        }
      }
    }
  }

  /**
   * Apply visitors to the {@link Plan}. This means collecting {@link PlanElement} then applying those elements to
   * {@link FileFlowPlanner}s (which means extending the flow planner's {@link Pipe} based on the element).
   */
  private void applyVisitors(Plan plan, SubmissionPlatformStrategy platform, String projectKey) {
    val visitors = createVisitors(projectKey, platform, restrictionTypes);

    for (val visitor : visitors) {
      log.info("Applying '{}' planning visitor to '{}'", visitor.getClass().getSimpleName(), projectKey);
      visitor.applyPlan(plan);
    }
  }

  private static List<PlanningVisitor<? extends PlanElement>> createVisitors(
      String projectKey, SubmissionPlatformStrategy platform, Set<RestrictionType> restrictionTypes) {
    return ImmutableList.of(
        new ValueTypePlanningVisitor(projectKey), // Must happen before RangeRestriction
        new RowBasedRestrictionPlanningVisitor(projectKey, restrictionTypes),
        new SummaryReportingPlanningVisitor(projectKey, platform),
        new ErrorReportingPlanningVisitor(projectKey, platform, ROW_BASED));
  }
}
