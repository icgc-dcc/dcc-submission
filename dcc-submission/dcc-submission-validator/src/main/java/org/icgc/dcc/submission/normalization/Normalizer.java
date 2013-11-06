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
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.normalization.configuration.ConfigurableStep.OptionalStep;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.flow.Flow;
import cascading.flow.FlowDef;
import cascading.flow.local.LocalFlowConnector;
import cascading.pipe.Pipe;
import cascading.scheme.local.TextDelimited;
import cascading.tap.local.FileTap;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;

/**
 * TODO See https://wiki.oicr.on.ca/display/DCCSOFT/Data+Normalizer+Component
 */
// @NoArgsConstructor(access = PRIVATE) // TODO: injection
@Slf4j
@RequiredArgsConstructor
public final class Normalizer {

  private static final String COMPONENT_NAME = Normalizer.class.getSimpleName();

  private static final String CASCADE_NAME = format("%s-cascade", COMPONENT_NAME);
  private static final String FLOW_NAME = format("%s-flow", COMPONENT_NAME);
  private static final String START_PIPE_NAME = format("%s-start", COMPONENT_NAME);
  private static final String END_PIPE_NAME = format("%s-end", COMPONENT_NAME);

  /**
   * Key for the project corresponding to the submission.
   */
  private final String projectKey;

  /**
   * Order typically matters.
   */
  private final ImmutableList<NormalizationStep> steps;

  /**
   * 
   */
  private final Config config;

  /**
   * 
   */
  public void normalize() { // TODO: add platform strategy
    val pipes = planCascade();
    val connected = connectCascade(pipes);
    connected.completeCascade();
    report(connected);
    sanityChecks(connected);
  }

  /**
   * 
   */
  private Pipes planCascade() {
    Pipe startPipe = new Pipe(START_PIPE_NAME);

    Pipe pipe = startPipe;
    for (NormalizationStep step : steps) {
      if (isEnabled(step, config)) {
        log.info("Adding step '{}'", step.shortName());
        pipe = step.extend(pipe);
      } else {
        log.info("Skipping disabled step '{}'", step.shortName());
      }
    }
    Pipe endPipe = new Pipe(END_PIPE_NAME, pipe);

    return new Pipes(startPipe, endPipe);
  }

  /**
   * 
   */
  private ConnectedCascade connectCascade(Pipes pipes) {
    FlowDef flowDef =
        flowDef()
            .setName(FLOW_NAME)
            .addSource(
                pipes.getStartPipe(),
                inputTap())
            .addTailSink(
                pipes.getEndPipe(),
                outputTap());

    Flow<?> flow = new LocalFlowConnector() // FIXME
        .connect(flowDef);

    Cascade cascade = new CascadeConnector()
        .connect(
        cascadeDef()
            .setName(CASCADE_NAME)
            .addFlow(flow));

    return new ConnectedCascade(flow, cascade);
  }

  /**
   * 
   */
  private void report(ConnectedCascade connected) {
    System.out.println(projectKey);
    for (NormalizationCounter counter : NormalizationCounter.values()) {
      System.out.println(counter + ": " + connected.getCounterValue(counter));
    }
  }

  /**
   * 
   */
  private void sanityChecks(ConnectedCascade connected) {
    long totalEnd = connected.getCounterValue(NormalizationCounter.TOTAL_END);
    long totalStart = connected.getCounterValue(NormalizationCounter.TOTAL_START);
    long masked = connected.getCounterValue(NormalizationCounter.MASKED);
    long markedAsControlled = connected.getCounterValue(NormalizationCounter.MARKED_AS_CONTROLLED);
    long dropped = connected.getCounterValue(NormalizationCounter.DROPPED);
    long uniqueStart = connected.getCounterValue(NormalizationCounter.UNIQUE_START);
    long uniqueFiltered = connected.getCounterValue(NormalizationCounter.UNIQUE_FILTERED);

    checkState(totalEnd == (totalStart + masked - dropped), "TODO");
    checkState(masked <= markedAsControlled, "TODO");
    checkState(uniqueStart <= totalStart, "TODO");
    checkState(uniqueFiltered <= uniqueStart, "TODO");
    checkState(uniqueFiltered <= uniqueStart, "TODO");
  }

  /**
   * Well-formedness validation has already ensured that we have a properly formatted TSV file.
   */
  private FileTap inputTap() {
    return new FileTap(new TextDelimited(true, "\t"), "/home/tony/git/git0/data-submission/input"); // TODO: actually
                                                                                                    // plug platform
  }

  /**
   * 
   */
  private FileTap outputTap() {
    return new FileTap(new TextDelimited(true, "\t"), "/tmp/deleteme"); // TODO: actually plug platform
  }

  /**
   * 
   */
  private boolean isEnabled(NormalizationStep step, Config config) {
    return !(step instanceof OptionalStep)
        || ((OptionalStep) step).isEnabled(config);
  }

  @Value
  private static final class Pipes {

    private final Pipe startPipe;
    private final Pipe endPipe;
  }

  @Value
  private static final class ConnectedCascade {

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
