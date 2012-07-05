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

import java.util.List;

import cascading.flow.Flow;
import cascading.flow.FlowListener;
import cascading.stats.FlowStats;

/**
 * 
 */
public class ValidationFlowListener implements FlowListener {

  private final List<Flow> flows;

  private final String projectKey;

  private final ValidationCallback callback;

  ValidationFlowListener(ValidationCallback callback, List<Flow> flows, String projectKey) {
    this.flows = flows;
    this.projectKey = projectKey;
    this.callback = callback;
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

    // TODO: check either/or
    if(successfulCascade) {
      callback.handleSuccessfulValidation(projectKey);
    } else if(failedCascade) {
      // TODO
    }
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void onStarting(Flow flow) {
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void onStopping(Flow flow) {
  }

  @Override
  @SuppressWarnings("rawtypes")
  public boolean onThrowable(Flow flow, Throwable t) {
    return false;
  }
}
