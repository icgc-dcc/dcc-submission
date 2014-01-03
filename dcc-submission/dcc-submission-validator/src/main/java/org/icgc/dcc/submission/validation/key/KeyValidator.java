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
package org.icgc.dcc.submission.validation.key;

import static cascading.cascade.CascadeDef.cascadeDef;
import static cascading.flow.FlowDef.flowDef;
import static java.lang.String.format;
import static org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType.SSM_P_TYPE;
import lombok.val;

import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.flow.Flow;
import cascading.pipe.Each;
import cascading.pipe.Pipe;

public class KeyValidator implements Validator {

  public static final String COMPONENT_NAME = "Key Validator";

  /**
   * Type that is the focus of normalization (there could be more in the future).
   */
  public static final SubmissionFileType FOCUS_TYPE = SSM_P_TYPE;

  private static final String CASCADE_NAME = format("%s-cascade", COMPONENT_NAME);
  private static final String FLOW_NAME = format("%s-flow", COMPONENT_NAME);

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }

  @Override
  public void validate(ValidationContext context) throws InterruptedException {
    val cascade =
        connectCascade(context.getPlatformStrategy(), context.getRelease().getName(), context.getProjectKey());

    cascade.complete();
  }

  private Cascade connectCascade(PlatformStrategy platformStrategy, String releaseName, String projectKey) {
    Pipe pipe = new Each("key-validation", new ExecuteFunction(new Runnable() {

      @Override
      public void run() {
        // TODO: Put call to business logic here
        System.out.println("Starting validation inside the cluster!");
      }

    }));

    val flow = createFlow(platformStrategy, projectKey, pipe);
    val cascade = createCascade(projectKey, flow);

    return cascade;
  }

  private static Flow<?> createFlow(PlatformStrategy platformStrategy, String projectKey, Pipe pipe) {
    Flow<?> flow = platformStrategy.getFlowConnector()
        .connect(flowDef()
            .setName(FLOW_NAME)
            .addSource(pipe, new EmptySourceTap<Void>(projectKey))
            .addTailSink(pipe, new EmptySinkTap<Void>(projectKey)));
    flow.writeDOT(format("/tmp/%s-%s.dot", projectKey, flow.getName()));
    flow.writeStepsDOT(format("/tmp/%s-%s-steps.dot", projectKey, flow.getName()));

    return flow;
  }

  private static cascading.cascade.Cascade createCascade(String projectKey, final cascading.flow.Flow<?> flow) {
    val cascade = new CascadeConnector()
        .connect(cascadeDef()
            .setName(CASCADE_NAME)
            .addFlow(flow));
    cascade.writeDOT(format("/tmp/%s-%s.dot", projectKey, cascade.getName()));

    return cascade;
  }

}
