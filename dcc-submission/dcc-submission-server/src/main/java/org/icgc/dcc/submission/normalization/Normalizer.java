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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.report.SubmissionReport;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.flow.Flow;
import cascading.flow.local.LocalFlowConnector;
import cascading.pipe.Pipe;
import cascading.scheme.local.TextDelimited;
import cascading.tap.local.FileTap;

import com.google.common.collect.ImmutableList;

/**
 * TODO See https://wiki.oicr.on.ca/display/DCCSOFT/Data+Normalizer+Component
 */
// @NoArgsConstructor(access = PRIVATE) // TODO: injection
@Slf4j
@RequiredArgsConstructor
public final class Normalizer {

  private static final String COMPONENT_NAME = "normalizer";

  private static final String CASCADE_NAME = format("%s-cascade", COMPONENT_NAME);
  private static final String FLOW_NAME = format("%s-flow", COMPONENT_NAME);
  private static final String START_PIPE_NAME = format("%s-start", COMPONENT_NAME);
  private static final String END_PIPE_NAME = format("%s-end", COMPONENT_NAME);

  /**
   * Order typically matters.
   */
  private final ImmutableList<NormalizationStep> steps;

  /**
   * 
   */
  public void normalize(SubmissionReport report) { // TODO: add platform strategy
    connect(plan()).complete();
  }

  /**
   * 
   */
  private Pipe plan() {
    Object config = null; // TODO: actual config
    Pipe pipe = new Pipe(START_PIPE_NAME);
    for (NormalizationStep step : steps) {
      if (isEnabled(step, config)) {
        log.info("Adding step '{}'", step.name());
        pipe = step.extend(pipe);
      } else {
        log.info("Skipping disabled step '{}'", step.name());
      }
    }
    return new Pipe(END_PIPE_NAME, pipe);
  }

  /**
   * 
   */
  private Cascade connect(Pipe pipe) {
    Flow<?> flow = new LocalFlowConnector()
        .connect(
        flowDef()
            .setName(FLOW_NAME)
            .addSource(
                pipe,
                sourceTap())
            .addTailSink(
                pipe,
                sinkTap()));

    return new CascadeConnector()
        .connect(
        cascadeDef()
            .setName(CASCADE_NAME)
            .addFlow(flow));
  }

  /**
   * Well-formedness validation has already ensured that we have a properly formatted TSV file.
   */
  private FileTap sourceTap() {
    return new FileTap(new TextDelimited(true, "\t"), "/home/tony/git/git0/data-submission/input"); // TODO: actually
                                                                                                    // plug platform
  }

  /**
   * 
   */
  private FileTap sinkTap() {
    return new FileTap(new TextDelimited(true, "\t"), "/tmp/deleteme"); // TODO: actually plug platform
  }

  /**
   * 
   */
  private boolean isEnabled(NormalizationStep step, Object config) {
    return !(step instanceof OptionalStep)
        || ((OptionalStep) step).isEnabled(config);
  }

}
