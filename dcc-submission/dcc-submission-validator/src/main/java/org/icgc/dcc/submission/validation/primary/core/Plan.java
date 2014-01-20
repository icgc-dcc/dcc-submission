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
import static java.lang.String.format;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.core.ReportContext;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.primary.MissingFileException;
import org.icgc.dcc.submission.validation.primary.planner.ExternalFlowPlanner;
import org.icgc.dcc.submission.validation.primary.planner.FileSchemaFlowPlanner;
import org.icgc.dcc.submission.validation.primary.planner.InternalFlowPlanner;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.cascade.CascadeDef;

@Slf4j
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
  private final PlatformStrategy cascadingStrategy;

  /**
   * Metadata.
   */
  private final Map<String, InternalFlowPlanner> internalFlowPlanners = newHashMap();
  private final Map<String, ExternalFlowPlanner> externalFlowPlanners = newHashMap();

  /**
   * Transient state
   */
  private Cascade cascade;

  public String path(FileSchema schema) throws FileNotFoundException, IOException {
    return cascadingStrategy.path(schema).getName();
  }

  public void connect(PlatformStrategy platformStrategy) {
    val cascadeDef = new CascadeDef().setName(projectKey + " validation cascade");
    for (val flowPlanner : getFlowPlanners()) {
      val flow = flowPlanner.connect(platformStrategy);
      if (flow != null) {
        flow.writeDOT("/tmp/validation-flow-" + flow.getName() + ".dot");
        flow.writeStepsDOT("/tmp/validation-flow-steps-" + flow.getName() + ".dot");

        cascadeDef.addFlow(flow);
      }
    }

    cascade = new CascadeConnector().connect(cascadeDef);
    cascade.writeDOT("/tmp/validation-cascade.dot");
  }

  public void include(FileSchema fileSchema, InternalFlowPlanner internal, ExternalFlowPlanner external) {
    internalFlowPlanners.put(fileSchema.getName(), internal);
    // externalFlowPlanners.put(fileSchema.getName(), external);
  }

  public void collect(ReportContext reportContext) {
    for (val planner : getFlowPlanners()) {
      planner.collect(cascadingStrategy, reportContext);
    }
  }

  public Dictionary getDictionary() {
    return dictionary;
  }

  public InternalFlowPlanner getInternalFlow(String schema) throws MissingFileException {
    val schemaPlan = internalFlowPlanners.get(schema);
    if (schemaPlan == null) {
      log.error(format("No corresponding file for schema %s, schemata with files are %s", schema,
          internalFlowPlanners.keySet()));

      throw new MissingFileException(schema);
    }

    return schemaPlan;
  }

  public Iterable<InternalFlowPlanner> getInternalFlows() {
    return unmodifiableIterable(internalFlowPlanners.values());
  }

  public ExternalFlowPlanner getExternalFlow(String schema) throws MissingFileException {
    val schemaPlan = externalFlowPlanners.get(schema);
    if (schemaPlan == null) {
      throw new MissingFileException(schema);
    }

    return schemaPlan;
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
