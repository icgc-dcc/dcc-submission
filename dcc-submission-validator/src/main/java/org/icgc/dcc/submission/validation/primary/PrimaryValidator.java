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
package org.icgc.dcc.submission.validation.primary;

import static com.google.common.collect.Iterables.size;
import static org.icgc.dcc.submission.validation.core.Validators.checkInterrupted;

import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.primary.core.Plan;
import org.icgc.dcc.submission.validation.primary.planner.Planner;

import com.google.inject.Inject;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * {@code Validator} implementation that performs relation, restriction and data type validations using Cascading as the
 * execution platform.
 * 
 * @see https://groups.google.com/d/msg/cascading-user/gjxB2Bg-56w/R1h5lhn-g2IJ
 */
@Slf4j
public class PrimaryValidator implements Validator {

  private final Planner planner;

  @Inject
  public PrimaryValidator(@NonNull Planner planner) {
    this.planner = planner;
  }

  @Override
  public String getName() {
    return "Primary Validator";
  }

  @Override
  @SneakyThrows
  public void validate(ValidationContext context) {
    // Shorthands
    val projectKey = context.getProjectKey();
    val dataTypes = context.getDataTypes();
    val dictionary = context.getDictionary();
    val platform = context.getPlatformStrategy();

    // Plan
    log.info("Planning cascade for project '{}'", projectKey);
    Plan plan = planner.plan(projectKey, dataTypes, platform, dictionary);
    log.info("Planned cascade for project '{}', # of row-based flow planners: {}",
        new Object[] { projectKey, size(plan.getRowBasedFlowPlanners()) });

    // Connect
    log.info("Connecting cascade for project '{}'", projectKey);
    plan.connect();
    log.info("Connected cascade for project '{}'", projectKey);
    checkInterrupted(getName());

    try {
      // Start (blocking)
      log.info("Starting cascade for project '{}'", projectKey);
      plan.getCascade().complete();
      log.info("Finished cascade for project '{}'", projectKey);
      checkInterrupted(getName());

      // Report
      log.info("Collecting report for project '{}'", projectKey);
      plan.collectSubmissionReport(context);
      log.info("Finished collecting report for project '{}'", projectKey);
    } catch (Throwable t) {
      log.info("Exception completing cascade for project '{}': '{}'", projectKey, t.getMessage());

      // Stop (blocking)
      log.info("Stopping cascade for project '{}'", projectKey);
      plan.getCascade().stop();
      log.info("Stopped cascade for project '{}'", projectKey);

      throw t;
    }
  }
}
