/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.release.model.SubmissionState;

@NoArgsConstructor(access = PACKAGE)
public class QueuedState extends AbstractCancellableState {

  @Override
  public void startValidation(@NonNull StateContext stateContext, @NonNull Iterable<DataType> dataTypes,
      @NonNull Report newReport) {
    val originalReport = stateContext.getReport();

    // Ensure the latest files are accounted for
    originalReport.refreshFiles(stateContext.getSubmissionFiles());
    originalReport.resetDataTypes(dataTypes);
    originalReport.inheritState(SubmissionState.VALIDATING, dataTypes);

    newReport.refreshFiles(stateContext.getSubmissionFiles());
    newReport.resetDataTypes(dataTypes);
    newReport.inheritState(SubmissionState.VALIDATING, dataTypes);

    // Set to validating and clobber the report
    stateContext.setState(SubmissionState.VALIDATING);
    stateContext.setReport(originalReport);
  }

  @Override
  public void cancelValidation(@NonNull StateContext stateContext, @NonNull Iterable<DataType> dataTypes) {
    // Reset reports related to the validating data types
    val originalReport = stateContext.getReport();
    originalReport.refreshFiles(stateContext.getSubmissionFiles());
    originalReport.resetDataTypes(dataTypes);

    // Transition based on report
    val nextState = getReportedNextState(originalReport);
    stateContext.setState(nextState);
  }

}