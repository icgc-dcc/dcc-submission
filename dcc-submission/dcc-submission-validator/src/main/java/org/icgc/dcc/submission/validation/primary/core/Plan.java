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
package org.icgc.dcc.submission.validation.primary.core;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.unmodifiableIterable;
import static com.google.common.collect.Maps.newHashMap;

import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.validation.core.ReportContext;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.primary.planner.ExternalFlowPlanner;
import org.icgc.dcc.submission.validation.primary.planner.FileSchemaFlowPlanner;
import org.icgc.dcc.submission.validation.primary.planner.InternalFlowPlanner;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.cascade.CascadeDef;

@RequiredArgsConstructor
public class Plan {

  /**
   * Inputs.
   */
  @NonNull
  private final String projectKey;
  @NonNull
  private final Dictionary dictionary;
  @NonNull
  private final PlatformStrategy platform;

  /**
   * Metadata.
   */
  private final Map<String, InternalFlowPlanner> internalFlowPlanners = newHashMap();
  private final Map<String, ExternalFlowPlanner> externalFlowPlanners = newHashMap();

  /**
   * Transient state
   */
  private Cascade cascade;

  public void connect() {
    val cascadeDef = new CascadeDef().setName(projectKey + " validation cascade");
    for (val flowPlanner : getFlowPlanners()) {
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

  public void include(String fileName, InternalFlowPlanner internalFlowPlanner) {
    internalFlowPlanners.put(fileName, internalFlowPlanner);
  }

  public void collect(ReportContext reportContext) {
    for (val planner : getFlowPlanners()) {
      planner.collect(platform, reportContext);
    }
  }

  public Dictionary getDictionary() {
    return dictionary;
  }

  public InternalFlowPlanner getInternalFlow(String schema) {
    return internalFlowPlanners.get(schema);
  }

  public Iterable<InternalFlowPlanner> getInternalFlows() {
    return unmodifiableIterable(internalFlowPlanners.values());
  }

  public ExternalFlowPlanner getExternalFlow(String schema) {
    return externalFlowPlanners.get(schema);
  }

  public Iterable<ExternalFlowPlanner> getExternalFlows() {
    return unmodifiableIterable(externalFlowPlanners.values());
  }

  public Iterable<? extends FileSchemaFlowPlanner> getFlows(FlowType type) {
    switch (type) {
    case INTERNAL:
      return unmodifiableIterable(internalFlowPlanners.values());
    case EXTERNAL:
      return unmodifiableIterable(externalFlowPlanners.values());
    default:
      throw new IllegalArgumentException();
    }
  }

  public Cascade getCascade() {
    return cascade;
  }

  private Iterable<FileSchemaFlowPlanner> getFlowPlanners() {
    return concat(internalFlowPlanners.values(), externalFlowPlanners.values());
  }

}
