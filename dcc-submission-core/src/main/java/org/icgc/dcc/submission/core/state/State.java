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

import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.submission.core.model.Outcome;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.fs.SubmissionFileEvent;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;

/**
 * Classical State Pattern abstraction modeling the state behavior of an associated submission within the context of a
 * release.
 * 
 * @see http://en.wikipedia.org/wiki/State_pattern
 */
public interface State {

  /**
   * The name of the state
   */
  String getName();

  /**
   * Are file system modifications allowed?
   */
  boolean isReadOnly();

  /**
   * Actions
   */

  void initialize(StateContext context);

  void modifyFile(StateContext context, SubmissionFileEvent event);

  void queueRequest(StateContext context, Iterable<DataType> dataTypes);

  void startValidation(StateContext context, Iterable<DataType> dataTypes, Report nextReport);

  void cancelValidation(StateContext context, Iterable<DataType> dataTypes);

  void finishValidation(StateContext context, Iterable<DataType> dataTypes, Outcome outcome, Report nextReport);

  void signOff(StateContext context);

  Submission closeRelease(StateContext context, Release nextRelease);

  void reset(StateContext context);

}