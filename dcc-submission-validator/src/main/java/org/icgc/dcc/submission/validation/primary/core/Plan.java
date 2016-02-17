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
package org.icgc.dcc.submission.validation.primary.core;

import static com.google.common.collect.Iterables.unmodifiableIterable;
import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.validation.core.ReportContext;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy;
import org.icgc.dcc.submission.validation.primary.planner.FileFlowPlanner;
import org.icgc.dcc.submission.validation.primary.planner.RowBasedFlowPlanner;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.cascade.CascadeDef;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class Plan {

  /**
   * The maximum number of flows that can run concurrently for a given project plan.
   * <p>
   * The number of threads is proportional to the number of steps which is a function of the number of files,
   * restrictions and summaries for the schema associated with those files.
   */
  public static final int MAX_CONCURRENT_FLOWS = 25;

  /**
   * The maximum number of flow steps that may be executing concurrently per flow.
   * <p>
   * Limited to prevent overwhelming the server.
   */
  public static final int MAX_CONCURRENT_FLOW_STEPS = 10;

  /**
   * Inputs.
   */
  @NonNull
  private final String projectKey;
  @NonNull
  private final Dictionary dictionary;
  @NonNull
  private final SubmissionPlatformStrategy platform;

  /**
   * Metadata.
   */
  private final Map<String, RowBasedFlowPlanner> rowBasedFlowPlanners = newHashMap();

  /**
   * Transient state
   */
  private Cascade cascade;

  public void connect() {
    val cascadeDef = new CascadeDef()
        .setName(projectKey + " validation cascade")
        .setMaxConcurrentFlows(MAX_CONCURRENT_FLOWS);

    for (val flowPlanner : rowBasedFlowPlanners.values()) {
      val flow = flowPlanner.connect(platform);
      if (flow != null) {
        flow.writeDOT("/tmp/validation-flow-" + flow.getName() + ".dot");
        flow.writeStepsDOT("/tmp/validation-flow-steps-" + flow.getName() + ".dot");

        cascadeDef.addFlow(flow);
      }
    }

    cascade = new CascadeConnector().connect(cascadeDef);
    cascade.writeDOT("/tmp/validation-cascade.dot");
  }

  public void include(String fileName, RowBasedFlowPlanner rowBasedFlowPlanner) {
    rowBasedFlowPlanners.put(fileName, rowBasedFlowPlanner);
  }

  public void collectSubmissionReport(ReportContext reportContext) {
    for (val planner : rowBasedFlowPlanners.values()) {
      planner.collectFileReport(platform, reportContext);
    }
  }

  public Dictionary getDictionary() {
    return dictionary;
  }

  public Iterable<RowBasedFlowPlanner> getRowBasedFlowPlanners() {
    return unmodifiableIterable(rowBasedFlowPlanners.values());
  }

  public Iterable<? extends FileFlowPlanner> getFileFlowPlanners(FlowType type) {
    return unmodifiableIterable(rowBasedFlowPlanners.values());
  }

  public Cascade getCascade() {
    return cascade;
  }

}
