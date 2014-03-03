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
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.release.model.SubmissionState;

@NoArgsConstructor(access = PACKAGE)
public class QueuedState extends AbstractCancellableState {

  @Override
  public void startValidation(@NonNull StateContext context, @NonNull Iterable<DataType> dataTypes,
      @NonNull Report nextReport) {
    // Ensure the latest files are accounted for
    nextReport.refreshFiles(context.getSubmissionFiles());
    nextReport.resetDataTypes(dataTypes);
    nextReport.notifyState(SubmissionState.VALIDATING, dataTypes);

    // Set to validating and clobber the report
    context.setState(SubmissionState.VALIDATING);
    context.setReport(nextReport);
  }

  @Override
  public void cancelValidation(@NonNull StateContext context, @NonNull Iterable<DataType> dataTypes) {
    // Reset reports related to the validating data types
    val report = context.getReport();
    report.refreshFiles(context.getSubmissionFiles());
    report.resetDataTypes(dataTypes);

    // Transition based on report
    val nextState = getReportedNextState(report);
    context.setState(nextState);
  }

}