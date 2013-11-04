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
package org.icgc.dcc.submission.validation.core;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.unmodifiableIterable;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.validation.MissingFileException;
import org.icgc.dcc.submission.validation.planner.ExternalFlowPlanner;
import org.icgc.dcc.submission.validation.planner.FileSchemaFlowPlanner;
import org.icgc.dcc.submission.validation.planner.InternalFlowPlanner;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.report.Outcome;
import org.icgc.dcc.submission.validation.report.SchemaReport;
import org.icgc.dcc.submission.validation.report.SubmissionReport;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.cascade.CascadeDef;
import cascading.cascade.CascadeListener;

import com.google.common.collect.Iterables;

@Slf4j
@RequiredArgsConstructor
public class Plan {

  /**
   * Inputs.
   */
  @NonNull
  private final QueuedProject queuedProject;
  @NonNull
  private final Dictionary dictionary;
  @NonNull
  private final PlatformStrategy cascadingStrategy;
  @NonNull
  private final SubmissionDirectory submissionDirectory;

  /**
   * Metadata.
   */
  private final List<FileSchema> plannedSchema = newArrayList();
  private final Map<String, InternalFlowPlanner> internalPlanners = newHashMap();
  private final Map<String, ExternalFlowPlanner> externalPlanners = newHashMap();

  /**
   * Transient state
   */
  private Cascade cascade;

  @Getter
  private volatile boolean killed;

  // TODO: Use proper timer (see DCC-739)
  private long startTime;

  public String path(FileSchema schema) throws FileNotFoundException, IOException {
    return cascadingStrategy.path(schema).getName();
  }

  public void markKilled() {
    checkState(!killed, "Attempted to kill plan multiple times");
    killed = true;
  }

  /**
   * startTime must have been set already (unit is milliseconds).
   */
  public long getDuration() {
    if (startTime == 0L) {
      return 0L;
    }

    return System.currentTimeMillis() - startTime;
  }

  public void include(FileSchema fileSchema, InternalFlowPlanner internal, ExternalFlowPlanner external) {
    plannedSchema.add(fileSchema);
    internalPlanners.put(fileSchema.getName(), internal);
    externalPlanners.put(fileSchema.getName(), external);
  }

  public FileSchema getFileSchema(String name) {
    for (val schema : plannedSchema) {
      if (schema.getName().equals(name)) {
        return schema;
      }
    }

    return null;
  }

  public Dictionary getDictionary() {
    return dictionary;
  }

  public QueuedProject getQueuedProject() {
    return queuedProject;
  }

  public String getProjectKey() {
    return queuedProject.getKey();
  }

  public InternalFlowPlanner getInternalFlow(String schema) throws MissingFileException {
    val schemaPlan = internalPlanners.get(schema);
    if (schemaPlan == null) {
      log.error(format("No corresponding file for schema %s, schemata with files are %s", schema,
          internalPlanners.keySet()));

      throw new MissingFileException(schema);
    }

    return schemaPlan;
  }

  public Iterable<InternalFlowPlanner> getInternalFlows() {
    return unmodifiableIterable(internalPlanners.values());
  }

  public ExternalFlowPlanner getExternalFlow(String schema) throws MissingFileException {
    val schemaPlan = externalPlanners.get(schema);
    if (schemaPlan == null) {
      throw new MissingFileException(schema);
    }

    return schemaPlan;
  }

  public Iterable<ExternalFlowPlanner> getExternalFlows() {
    return unmodifiableIterable(externalPlanners.values());
  }

  public Iterable<? extends FileSchemaFlowPlanner> getFlows(FlowType type) {
    switch (type) {
    case INTERNAL:
      return unmodifiableIterable(internalPlanners.values());
    case EXTERNAL:
      return unmodifiableIterable(externalPlanners.values());
    default:
      throw new IllegalArgumentException();
    }
  }

  synchronized public void connect(PlatformStrategy platformStrategy) {
    val cascadeDef = new CascadeDef().setName(queuedProject.getKey() + " validation cascade");
    for (val planner : getPlanners()) {
      val flow = planner.connect(platformStrategy);
      if (flow != null) {
        cascadeDef.addFlow(flow);
      }
    }

    cascade = new CascadeConnector().connect(cascadeDef);
  }

  /**
   * Starts the cascade in a non-blocking manner and takes care of associated action like starting timer and emptying
   * the working directory.
   * 
   * @see https://groups.google.com/d/msg/cascading-user/gjxB2Bg-56w/R1h5lhn-g2IJ
   */
  synchronized public void startCascade() {
    startTime = System.currentTimeMillis();
    submissionDirectory.resetValidationDir();

    int size = cascade.getFlows().size();
    log.info("starting cascade with {} flows", size);
    cascade.start();

    // See link above
    log.info("Pausing for flows to start so that cancellation won't find an empty jobMap", size);
    sleepUninterruptibly(2, SECONDS);
  }

  /**
   * Stops the cascade in a blocking manner.
   */
  synchronized public void stopCascade() {
    cascade.stop();
  }

  synchronized public Plan addCascadeListener(CascadeListener listener) {
    cascade.addListener(listener);
    return this;
  }

  synchronized public Cascade getCascade() {
    return cascade;
  }

  public Outcome collect(SubmissionReport report) {
    Outcome result = Outcome.PASSED;
    Map<String, SchemaReport> schemaReports = newLinkedHashMap();
    for (val planner : concat(internalPlanners.values(), externalPlanners.values())) {
      SchemaReport schemaReport = new SchemaReport();
      Outcome outcome = planner.collect(cascadingStrategy, schemaReport);

      if (outcome == Outcome.FAILED) {
        result = Outcome.FAILED;
      }

      if (!schemaReports.containsKey(schemaReport.getName())) {
        schemaReports.put(schemaReport.getName(), schemaReport);
      } else {
        // combine internal and external plans into one
        SchemaReport sreport = schemaReports.get(schemaReport.getName());

        sreport.addFieldReports(schemaReport.getFieldReports());
        sreport.addErrors(schemaReport.getErrors());
      }
    }

    // remove empty report
    schemaReports.remove(null);

    report.setSchemaReports(new ArrayList<SchemaReport>(schemaReports.values()));
    return result;
  }

  private Iterable<FileSchemaFlowPlanner> getPlanners() {
    return Iterables.concat(internalPlanners.values(), externalPlanners.values());
  }

}
