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
package org.icgc.dcc.submission.validation.primary.visitor;

import java.util.List;

import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy;
import org.icgc.dcc.submission.validation.primary.core.FlowType;
import org.icgc.dcc.submission.validation.primary.core.Plan;
import org.icgc.dcc.submission.validation.primary.core.ReportingPlanElement;

import lombok.NonNull;
import lombok.val;

public abstract class ReportingPlanningVisitor extends PlanningVisitor<ReportingPlanElement> {

  protected final SubmissionPlatformStrategy platform;

  public ReportingPlanningVisitor(@NonNull String projectKey, @NonNull SubmissionPlatformStrategy platform,
      @NonNull FlowType type) {
    super(projectKey, type);
    this.platform = platform;
  }

  protected List<String> listMatchingFiles(String pattern) {
    return platform.listFileNames(pattern);
  }

  @Override
  public void applyPlan(Plan plan) {
    for (val flowPlanner : plan.getFileFlowPlanners(getFlowType())) {
      flowPlanner.acceptVisitor(this);

      for (val collectedReportingPlanElement : getCollectedPlanElements()) {
        flowPlanner.applyReportingPlanElement(collectedReportingPlanElement);
      }
    }
  }

}
