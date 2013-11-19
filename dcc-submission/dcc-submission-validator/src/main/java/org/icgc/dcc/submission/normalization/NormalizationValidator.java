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
package org.icgc.dcc.submission.normalization;

import static cascading.cascade.CascadeDef.cascadeDef;
import static cascading.flow.FlowDef.flowDef;
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_ANALYSIS_ID;
import static org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType.SSM_P_TYPE;
import static org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter.TOTAL_END;
import static org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter.TOTAL_START;
import static org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter.UNIQUE_REMAINING;
import static org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter.UNIQUE_START;
import static org.icgc.dcc.submission.normalization.NormalizationUtils.getFileSchema;
import static org.icgc.dcc.submission.validation.core.Validators.checkInterrupted;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.hadoop.fs.DccFileSystem2;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.normalization.NormalizationContext.DefaultNormalizationContext;
import org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter;
import org.icgc.dcc.submission.normalization.steps.Counting;
import org.icgc.dcc.submission.normalization.steps.MaskedRowGeneration;
import org.icgc.dcc.submission.normalization.steps.MutationRebuilding;
import org.icgc.dcc.submission.normalization.steps.PreMarking;
import org.icgc.dcc.submission.normalization.steps.PrimaryKeyGeneration;
import org.icgc.dcc.submission.normalization.steps.RedundantObservationRemoval;
import org.icgc.dcc.submission.normalization.steps.SensitiveRowMarking;
import org.icgc.dcc.submission.normalization.steps.UniqueCounting;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.flow.Flow;
import cascading.pipe.Pipe;
import cascading.tap.Tap;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;

/**
 * Entry point for the normalization component. The component is described in
 * https://wiki.oicr.on.ca/display/DCCSOFT/Data+Normalizer+Component.
 */
@Slf4j
@RequiredArgsConstructor(access = PRIVATE)
public final class NormalizationValidator implements Validator {

  public static final String COMPONENT_NAME = "normalizer";

  /**
   * Type that is the focus of normalization (there could be more in the future).
   */
  private static final SubmissionFileType FOCUS_TYPE = SSM_P_TYPE;

  private static final String CASCADE_NAME = format("%s-cascade", COMPONENT_NAME);
  private static final String FLOW_NAME = format("%s-flow", COMPONENT_NAME);
  private static final String START_PIPE_NAME = format("%s-start", COMPONENT_NAME);
  private static final String END_PIPE_NAME = format("%s-end", COMPONENT_NAME);

  /**
   * Abstraction for the file system and related operations (temporary, see DCC-1876).
   */
  private final DccFileSystem2 dccFileSystem2;

  /**
   * Subset of config that is relevant to the normalization process.
   * <p>
   * TODO: bury under {@link NormalizationConfig}.
   */
  private final Config config;

  /**
   * Steps of the normalization. Order typically matters.
   */
  private final ImmutableList<NormalizationStep> steps;

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }

  /**
   * Returns the default instance for the normalization.
   */
  public static NormalizationValidator getDefaultInstance(DccFileSystem2 dccFileSystem2, Config config) {
    return new NormalizationValidator(
        dccFileSystem2,
        config,
        // Order matters for some steps
        new ImmutableList.Builder<NormalizationStep>()

            .add(new UniqueCounting(
                SUBMISSION_OBSERVATION_ANALYSIS_ID,
                UNIQUE_START))
            .add(new Counting(TOTAL_START))

            // Must happen before rebuilding the mutation
            .add(new PreMarking()) // Must happen no matter what
            .add(new SensitiveRowMarking())
            .add(new MaskedRowGeneration()) // May be skipped

            // Must happen after allele masking
            .add(new MutationRebuilding()) // Must happen before removing redundant observations
            .add(new RedundantObservationRemoval(SUBMISSION_OBSERVATION_ANALYSIS_ID))
            // May be skipped

            .add(new UniqueCounting(
                SUBMISSION_OBSERVATION_ANALYSIS_ID,
                UNIQUE_REMAINING))

            // Must happen after removing duplicates and allele masking
            .add(new PrimaryKeyGeneration())

            .add(new Counting(TOTAL_END))

            .build());
  }

  @Override
  public void validate(ValidationContext validationContext) {
    val optional = grabSubmissionFile(FOCUS_TYPE, validationContext);

    // Only perform normalization of there is a file to normalize
    if (optional.isPresent()) {
      String ssmPFile = optional.get();
      log.info("Starting normalization for {} file: '{}'", FOCUS_TYPE, ssmPFile);
      normalize(ssmPFile, validationContext);
      log.info("Finished normalization for {} file: '{}'", FOCUS_TYPE, ssmPFile);
    } else {
      log.info(
          "Skipping normalization for {}, no matching file in submission: '{}'",
          new Object[] { FOCUS_TYPE, optional.get(), validationContext.getSubmissionDirectory().listFile() });
    }
  }

  /**
   * Handles the normalization.
   */
  private void normalize(String fileName, ValidationContext context) {
    String releaseName = context.getRelease().getName();
    String projectKey = context.getProjectKey();

    // Plan cascade
    val pipes = planCascade(
        DefaultNormalizationContext.getNormalizationContext(
            context.getDictionary(),
            FOCUS_TYPE));

    // Connect cascade
    val connectedCascade = connectCascade(
        pipes,
        context.getPlatformStrategy(),
        getFileSchema(
            context.getDictionary(),
            FOCUS_TYPE),
        releaseName,
        projectKey);

    // Checks validator wasn't interrupted
    checkInterrupted(getName());

    // Run cascade synchronously
    connectedCascade.completeCascade();

    // Perform sanity check on counters
    NormalizationReporter.performSanityChecks(connectedCascade);

    // Report results (error or stats)s
    val checker = NormalizationReporter.createNormalizationOutcomeChecker(config, connectedCascade, fileName);
    if (checker.isLikelyErroneous()) {
      log.warn("The submission is erroneous from the normalization standpoint: '{}'", checker);
      NormalizationReporter.reportError(context, checker);
    } else {
      log.info("No errors were encountered during normalization");
      internalStatisticsReport(connectedCascade);
      externalStatisticsReport(fileName, connectedCascade, context);
    }
  }

  /**
   * Plans the normalization cascade. It will iterate over the {@link NormalizationStep}s and, if enabled, will have
   * them extend the main {@link Pipe}.
   */
  private Pipes planCascade(NormalizationContext normalizationContext) {
    Pipe startPipe = new Pipe(START_PIPE_NAME);

    Pipe pipe = startPipe;
    for (NormalizationStep step : steps) {
      if (NormalizationConfig.isEnabled(step, config)) {
        log.info("Adding step '{}'", step.shortName());
        pipe = step.extend(pipe, normalizationContext);
      } else {
        log.info("Skipping disabled step '{}'", step.shortName());
      }
    }
    Pipe endPipe = new Pipe(END_PIPE_NAME, pipe);

    return new Pipes(startPipe, endPipe);
  }

  /**
   * Connects the cascade to the input and output.
   */
  private ConnectedCascade connectCascade(
      Pipes pipes,
      PlatformStrategy platformStrategy,
      FileSchema fileSchema,
      String releaseName,
      String projectKey) {

    val flowDef =
        flowDef()
            .setName(FLOW_NAME)
            .addSource(
                pipes.getStartPipe(),
                getInputTap(platformStrategy, fileSchema))
            .addTailSink(
                pipes.getEndPipe(),
                getOutputTap(releaseName, projectKey));

    Flow<?> flow = platformStrategy // TODO: not re-using the submission's platform strategy
        .getFlowConnector()
        .connect(flowDef);
    flow.writeDOT(format("/tmp/%s-%s.dot", projectKey, flow.getName())); // TODO: refactor /tmp
    flow.writeStepsDOT(format("/tmp/%s-%s-steps.dot", projectKey, flow.getName()));

    val cascade = new CascadeConnector()
        .connect(
        cascadeDef()
            .setName(CASCADE_NAME)
            .addFlow(flow));
    cascade.writeDOT(format("/tmp/%s-%s.dot", projectKey, cascade.getName()));

    return new ConnectedCascade(
        releaseName,
        projectKey,
        flow,
        cascade);
  }

  /**
   * Writes the internal normalization report.
   * <p>
   * TODO: externalize
   */
  private void internalStatisticsReport(ConnectedCascade connectedCascade) {
    String report = NormalizationReporter.createInternalReportContent(connectedCascade);
    log.info("Internal report: {}", report); // Should be small enough
    dccFileSystem2.writeNormalizationReport(
        connectedCascade.getReleaseName(),
        connectedCascade.getProjectKey(),
        report);
  }

  /**
   * Reports statistics for the external report.
   * <p>
   * TODO: externalize
   */
  private void externalStatisticsReport(
      String fileName,
      ConnectedCascade connectedCascade,
      ValidationContext validationContext) {

    for (val entry : NormalizationReport
        .builder()
        .projectKey(
            validationContext.getProjectKey())
        .counters(
            NormalizationCounter.report(connectedCascade))
        .build()
        .getExternalReportCounters()
        .entrySet()) {

      val key = entry.getKey();
      val value = entry.getValue();
      log.info("External report for '{}': '{}' -> '{}'", new Object[] { fileName, key, value });
      validationContext.reportSummary(fileName, key, value);
    }
  }

  /**
   * Returns the submission file matching the type provided if there is such a file.
   */
  private Optional<String> grabSubmissionFile(SubmissionFileType type, ValidationContext validationContext) {
    return validationContext
        .getSubmissionDirectory()
        .getFile(
            validationContext
                .getDictionary()
                .getFilePattern(type));
  }

  /**
   * Returns the input tap for the cascade. Well-formedness validation has already ensured that we have a properly
   * formatted TSV file.
   */
  private Tap<?, ?, ?> getInputTap(PlatformStrategy platformStrategy, FileSchema fileSchema) {
    return platformStrategy.getSourceTap2(fileSchema);
  }

  /**
   * Returns the output tap for the cascade.
   */
  private Tap<?, ?, ?> getOutputTap(String releaseName, String projectKey) {
    return dccFileSystem2.getNormalizationDataOutputTap(releaseName, projectKey);
  }

  /**
   * Placeholder for the start and end pipe (to ease future connection as a cascade).
   */
  @Value
  private static final class Pipes {

    private final Pipe startPipe;
    private final Pipe endPipe;
  }

  /**
   * Placeholder for the connected cascade.
   */
  @Value
  static final class ConnectedCascade {

    private final String releaseName;
    private final String projectKey;
    private final Flow<?> flow;
    private final Cascade cascade;

    public void completeCascade() {
      cascade.complete();
    }

    public long getCounterValue(NormalizationCounter counter) {
      return flow.getFlowStats().getCounterValue(counter);
    }
  }
}
