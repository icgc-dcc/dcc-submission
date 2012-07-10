/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cascading.flow.Flow;
import cascading.flow.FlowListener;
import cascading.stats.FlowStats;

/**
 * Listens for events sent by individual {@code Flow}s
 */
public class ValidationFlowListener implements FlowListener {

  private static final Logger log = LoggerFactory.getLogger(ValidationFlowListener.class);

  @SuppressWarnings("rawtypes")
  private final List<Flow> flows;

  private final String projectKey;

  private final ValidationCallback callback;

  @SuppressWarnings("rawtypes")
  public ValidationFlowListener(ValidationCallback callback, List<Flow> flows, String projectKey) {
    checkArgument(flows != null);
    checkArgument(projectKey != null);

    this.flows = flows;
    this.projectKey = projectKey;
    this.callback = callback; // allow callback to be null (when running validation independently)
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void onCompleted(Flow flow) {
    boolean successfulCascade = true;
    boolean failedCascade = false;
    for(Flow flowTmp : flows) {
      FlowStats flowStats = flowTmp.getFlowStats();
      if(!flowStats.isSuccessful()) {
        successfulCascade = false;
      }
      if(flowStats.isFailed()) {
        failedCascade = true;
      }
    }

    checkArgument((successfulCascade && failedCascade) == false, "successfulCascade = " + successfulCascade
        + ", failedCascade = " + failedCascade);
    if(null != callback) {
      if(successfulCascade) {
        callback.handleSuccessfulValidation(projectKey);
      } else if(failedCascade) {
        callback.handleFailedValidation(projectKey);
      } // else: other flows must still be running
    }
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void onStarting(Flow flow) {
    log.info("starting flow {}", flow.getName());
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void onStopping(Flow flow) {
    log.info("stopping flow {}", flow.getName());
  }

  @Override
  @SuppressWarnings("rawtypes")
  public boolean onThrowable(Flow flow, Throwable t) {
    log.info("error in flow {}: {}", flow.getName(), t.getMessage());
    t.printStackTrace();// at lease print it so we don't miss it; TODO: pass it somewhere instead?
    return false;
  }
}
