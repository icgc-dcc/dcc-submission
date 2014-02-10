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

import static lombok.AccessLevel.PACKAGE;
import static org.icgc.dcc.submission.core.model.Outcome.CANCELLED;
import static org.icgc.dcc.submission.core.model.Outcome.FAILED;
import static org.icgc.dcc.submission.core.model.Outcome.SUCCEEDED;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.submission.core.model.Outcome;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.release.model.SubmissionState;

@NoArgsConstructor(access = PACKAGE)
public class ValidatingState extends AbstractState {

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public void cancelValidation(@NonNull StateContext context, @NonNull Iterable<DataType> dataTypes) {
    context.setState(SubmissionState.NOT_VALIDATED);

    val report = context.getReport();
    report.updateFiles(context.getSubmissionFiles());
    report.reset(dataTypes);
  }

  @Override
  public void finishValidation(@NonNull StateContext context, @NonNull Iterable<DataType> dataTypes,
      @NonNull Outcome outcome, @NonNull Report newReport) {
    val oldReport = context.getReport();

    if (outcome == SUCCEEDED) {
      newReport.refreshState();
      context.setReport(newReport);

      context.setState(newReport.isValid() ? SubmissionState.VALID : SubmissionState.NOT_VALIDATED);
    } else if (outcome == FAILED) {
      oldReport.setState(SubmissionState.ERROR, dataTypes);

      context.setState(SubmissionState.ERROR);
    } else if (outcome == CANCELLED) {
      // TODO: Should this branch be removed gue to cancelValidation?
      context.setState(SubmissionState.NOT_VALIDATED);
    }
  }

}
