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

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.submission.core.model.Outcome;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;

import com.google.common.base.Optional;

public interface State {

  String getName();

  boolean isReadOnly();

  void initializeSubmission(StateContext context);

  void modifySubmission(StateContext context, Optional<Path> path);

  void queueRequest(StateContext context, Iterable<DataType> dataTypes);

  void startValidation(StateContext context, Iterable<DataType> dataTypes, Report nextReport);

  void cancelValidation(StateContext context, Iterable<DataType> dataTypes);

  void finishValidation(StateContext context, Iterable<DataType> dataTypes, Outcome outcome, Report nextReport);

  void signOff(StateContext context);

  Submission performRelease(StateContext context, Release nextRelease);

  static final State NOT_VALIDATED = new NotValidatedState();
  static final State QUEUED = new QueuedState();
  static final State VALIDATING = new ValidatingState();
  static final State ERROR = new ErrorState();
  static final State VALID = new ValidState();
  static final State SIGNED_OFF = new SignedOffState();

}