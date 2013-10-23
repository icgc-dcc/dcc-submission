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
package org.icgc.dcc.submission.validation;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
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
import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.report.Outcome;
import org.icgc.dcc.submission.validation.report.SchemaReport;
import org.icgc.dcc.submission.validation.report.SubmissionReport;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.cascade.CascadeDef;
import cascading.cascade.CascadeListener;
import cascading.flow.Flow;

import com.google.common.collect.ImmutableMap;
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
  private final CascadingStrategy cascadingStrategy;
  @NonNull
  private final SubmissionDirectory submissionDirectory;

  /**
   * Metadata.
   */
  private final List<FileSchema> plannedSchema = newArrayList();
  private final Map<String, InternalFlowPlanner> internalPlanners = newHashMap();
  private final Map<String, ExternalFlowPlanner> externalPlanners = newHashMap();

  /**
   * Outputs.
   */
  private final Map<String, TupleState> fileLevelErrors = newLinkedHashMap();

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

  public void kill() {
    checkState(!killed, "Attempted to kill plan multiple times");
    this.killed = true;
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

  public void include(FileSchema fileSchema, InternalFlowPlanner internal, ExternalFlowPlanner external) {
    plannedSchema.add(fileSchema);
    internalPlanners.put(fileSchema.getName(), internal);
    externalPlanners.put(fileSchema.getName(), external);
  }

  public InternalFlowPlanner getInternalFlow(String schema) throws MissingFileException {
    InternalFlowPlanner schemaPlan = internalPlanners.get(schema);
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
    ExternalFlowPlanner schemaPlan = externalPlanners.get(schema);
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
      return Iterables.unmodifiableIterable(internalPlanners.values());
    case EXTERNAL:
      return Iterables.unmodifiableIterable(externalPlanners.values());
    default:
      throw new IllegalArgumentException();
    }
  }

  synchronized public void connect(CascadingStrategy cascadingStrategy) {
    CascadeDef cascade = new CascadeDef().setName(queuedProject.getKey() + " validation cascade");
    for (FileSchemaFlowPlanner planner : Iterables.concat(internalPlanners.values(), externalPlanners.values())) {
      try {
        Flow<?> flow = planner.connect(cascadingStrategy);
        if (flow != null) {
          cascade.addFlow(flow);
        }
      } catch (PlanningFileLevelException e) {
        addFileLevelError(e);
      }
    }

    this.cascade = new CascadeConnector().connect(cascade);
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

  /**
   * startTime must have been set already (unit is milliseconds).
   */
  public long getDuration() {
    checkNotNull(startTime);
    return System.currentTimeMillis() - startTime;
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
    for (FileSchemaFlowPlanner planner : Iterables.concat(internalPlanners.values(), externalPlanners.values())) {
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

  public FileSchema getFileSchema(String name) {
    for (val schema : plannedSchema) {
      if (schema.getName().equals(name)) {
        return schema;
      }
    }

    return null;
  }

  public void addFileLevelError(PlanningFileLevelException e) {
    String filename = e.getFilename();
    TupleState tupleState = e.getTupleState();
    checkState(filename != null);
    checkState(tupleState != null);

    // Let it overwrite any previously reported file-level errors
    // This is a band-aid in the context of https://jira.oicr.on.ca/browse/DCC-1289.
    // It shouldn't be hard to report more than one error by augmenting the existing tupleState, but the file-level
    // error reporting must be completely reworked anyway, see https://jira.oicr.on.ca/browse/DCC-391 and
    // https://wiki.oicr.on.ca/display/DCCSOFT/File-Level+Errors
    fileLevelErrors.put(filename, tupleState);
  }

  public boolean hasFileLevelErrors() {
    return !fileLevelErrors.isEmpty();
  }

  public Map<String, TupleState> getFileLevelErrors() {
    return ImmutableMap.<String, TupleState> copyOf(fileLevelErrors);
  }

}
