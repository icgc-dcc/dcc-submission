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
package org.icgc.dcc.submission.state;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.validation.core.SubmissionReport;
import org.icgc.dcc.submission.validation.core.ValidationOutcome;

import com.google.common.base.Optional;

@Slf4j
@RequiredArgsConstructor
public class DefaultStateContext implements StateContext {

  @NonNull
  @Getter
  private final Submission submission;
  @NonNull
  @Getter
  private final Release release;
  @NonNull
  @Getter
  private final Dictionary dictionary;

  @NonNull
  private State state;

  @Override
  public void setState(State nextState) {
    executeTransition(nextState);
    this.state = nextState;
  }

  @Override
  public void modifySubmission(@NonNull Optional<Path> path) {
    beginTransition("modifySubmission");
    state.modifySubmission(this, path);
    finishTransition("modifySubmission");
  }

  @Override
  public void queueRequest() {
    beginTransition("queueRequest");
    state.queueRequest(this);
    finishTransition("queueRequest");
  }

  @Override
  public void startValidation() {
    beginTransition("startValidation");
    state.startValidation(this);
    finishTransition("startValidation");
  }

  @Override
  public void finishValidation(@NonNull ValidationOutcome outcome, @NonNull SubmissionReport submissionReport) {
    beginTransition("finishValidation");
    state.finishValidation(this, outcome, submissionReport);
    finishTransition("finishValidation");
  }

  @Override
  public void signOff() {
    beginTransition("signOff");
    state.signOff(this);
    finishTransition("signOff");
  }

  @Override
  public Submission performRelease(@NonNull Release nextRelease) {
    beginTransition("performRelease");
    val result = state.performRelease(this, nextRelease);
    finishTransition("performRelease");

    return result;
  }

  private void beginTransition(@NonNull String action) {
    log.info("Action '{}' requested while in state '{}'", action, state);
  }

  private void executeTransition(@NonNull State nextState) {
    log.info("Changed state from '{}' to '{}'", state, nextState);
  }

  private void finishTransition(@NonNull String action) {
    log.info("Finished action '{}' while in state '{}'", action, state);
  }

}
