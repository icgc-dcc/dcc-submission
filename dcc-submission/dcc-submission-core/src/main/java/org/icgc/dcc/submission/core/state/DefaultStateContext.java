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

import static lombok.AccessLevel.NONE;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.submission.core.model.Outcome;
import org.icgc.dcc.submission.core.model.SubmissionFile;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;

import com.google.common.base.Optional;

@Slf4j
@Value
public class DefaultStateContext implements StateContext {

  @NonNull
  @Getter(NONE)
  private final Submission submission;
  @NonNull
  private final Iterable<SubmissionFile> submissionFiles;

  public State getState() {
    return submission.getState();
  }

  @Override
  public String getProjectKey() {
    return submission.getProjectKey();
  }

  @Override
  public String getProjectName() {
    return submission.getProjectName();
  }

  @Override
  public Report getReport() {
    return submission.getReport();
  }

  @Override
  public void setState(@NonNull SubmissionState nextState) {
    submission.setState(nextState);
  }

  @Override
  public void setReport(@NonNull Report nextReport) {
    submission.setReport(nextReport);
  }

  @Override
  public void modifySubmission(@NonNull Optional<Path> filePath) {
    beginTransition("modifySubmission");
    getState().modifySubmission(this, filePath);
    finishTransition("modifySubmission");
  }

  @Override
  public void queueRequest(@NonNull Iterable<DataType> dataTypes) {
    beginTransition("queueRequest");
    getState().queueRequest(this, dataTypes);
    finishTransition("queueRequest");
  }

  @Override
  public void startValidation(@NonNull Iterable<DataType> dataTypes, @NonNull Report nextReport) {
    beginTransition("startValidation");
    getState().startValidation(this, dataTypes, nextReport);
    finishTransition("startValidation");
  }

  @Override
  public void finishValidation(@NonNull Outcome outcome, @NonNull Report nextReport) {
    beginTransition("finishValidation");
    getState().finishValidation(this, outcome, nextReport);
    finishTransition("finishValidation");
  }

  @Override
  public void signOff() {
    beginTransition("signOff");
    getState().signOff(this);
    finishTransition("signOff");
  }

  @Override
  public Submission performRelease(@NonNull Release nextRelease) {
    beginTransition("performRelease");
    val result = getState().performRelease(this, nextRelease);
    finishTransition("performRelease");

    return result;
  }

  private void beginTransition(@NonNull String action) {
    log.info("Action '{}' requested while in state '{}'", action, getState());
  }

  private void finishTransition(@NonNull String action) {
    log.info("Finished action '{}' while in state '{}'", action, getState());
  }

}
