/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.core.state;

import static com.google.common.base.Preconditions.checkState;
import static lombok.AccessLevel.PACKAGE;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.submission.core.model.Outcome;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.release.model.SubmissionState;

@Slf4j
@NoArgsConstructor(access = PACKAGE)
public class ValidatingState extends AbstractCancellableState {

  @Override
  public boolean isReadOnly() {
    // Can't modify when transient
    return true;
  }

  @Override
  public void cancelValidation(@NonNull StateContext context, @NonNull Iterable<DataType> dataTypes) {
    // Do not change state of submission since this will happen after the validation finishes with a CANCELLED outcome.
    // Doing otherwise will lead to inconsistencies between the in-memory state and the persistence state
  }

  @Override
  public void finishValidation(@NonNull StateContext context, @NonNull Iterable<DataType> dataTypes,
      @NonNull Outcome outcome, @NonNull Report newReport) {
    val oldReport = context.getReport();

    switch (outcome) {
    case ABORTED:
      handleAborted(context, dataTypes, newReport);
      break;
    case CANCELLED:
      handleCancelled(context, dataTypes, newReport);
      break;
    case COMPLETED:
      handleCompleted(context, dataTypes, newReport);
      break;
    case FAILED:
      handleFailed(context, dataTypes, oldReport);
      break;
    default:
      checkState(false, "Unexpected outcome '%s'", outcome);
    }
  }

  private void handleAborted(StateContext context, Iterable<DataType> dataTypes, Report newReport) {
    // Reset any data type that is VALIDATING to NOT_VALIDATED (even if partially VALID in terms of validators)
    newReport.abort(dataTypes);

    updateContext(context, dataTypes, newReport);
  }

  private void handleCancelled(StateContext context, Iterable<DataType> dataTypes, Report newReport) {
    // Need to reset all the validating data types
    newReport.resetDataTypes(dataTypes);

    updateContext(context, dataTypes, newReport);
  }

  private void handleCompleted(StateContext context, Iterable<DataType> dataTypes, Report newReport) {
    // Valid status needs to be refreshed since there are no natural events that do this (unlike with Errors)
    newReport.refreshState();

    updateContext(context, dataTypes, newReport);
  }

  private void updateContext(StateContext context, Iterable<DataType> dataTypes, Report newReport) {
    // Commit the report that was collected during validation
    newReport.mergeReport(context.getReport(), dataTypes);

    // Transition
    val nextState = getReportedNextState(newReport);
    context.setState(nextState);
    context.setReport(newReport);
  }

  private void handleFailed(StateContext context, Iterable<DataType> dataTypes, Report oldReport) {
    // Use the old report and force a state
    val state = SubmissionState.ERROR;
    log.info("Setting old report state to {}...", state);
    oldReport.inheritState(state, dataTypes);

    // Need to call DCC...
    context.setState(state);
  }

}
