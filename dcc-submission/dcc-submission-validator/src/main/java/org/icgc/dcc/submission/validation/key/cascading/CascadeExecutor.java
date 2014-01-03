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
package org.icgc.dcc.submission.validation.key.cascading;

import static cascading.cascade.CascadeDef.cascadeDef;
import static cascading.flow.FlowDef.flowDef;
import static java.lang.String.format;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.icgc.dcc.submission.validation.platform.PlatformStrategy;

import cascading.cascade.Cascade;
import cascading.cascade.CascadeConnector;
import cascading.flow.Flow;
import cascading.pipe.Each;
import cascading.pipe.Pipe;

@RequiredArgsConstructor
public class CascadeExecutor {

  public static final String COMPONENT_NAME = "key-validator";
  private static final String CASCADE_NAME = format("%s-cascade", COMPONENT_NAME);
  private static final String FLOW_NAME = format("%s-flow", COMPONENT_NAME);

  @NonNull
  private final PlatformStrategy platformStrategy;

  public void execute(Runnable runnable) throws InterruptedException {
    val cascade = connectCascade(runnable);

    cascade.complete();
  }

  private Cascade connectCascade(Runnable runnable) {
    Pipe pipe = new Each("key-validation", new ExecuteFunction(runnable));

    val flow = createFlow(platformStrategy, pipe);
    val cascade = createCascade(flow);

    return cascade;
  }

  private static Flow<?> createFlow(PlatformStrategy platformStrategy, Pipe pipe) {
    Flow<?> flow = platformStrategy.getFlowConnector()
        .connect(flowDef()
            .setName(FLOW_NAME)
            .addSource(pipe, new EmptySourceTap<Void>("empty"))
            .addTailSink(pipe, new EmptySinkTap<Void>("empty")));

    return flow;
  }

  private static Cascade createCascade(final Flow<?> flow) {
    val cascade = new CascadeConnector()
        .connect(cascadeDef()
            .setName(CASCADE_NAME)
            .addFlow(flow));

    return cascade;
  }

}
