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

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.icgc.dcc.submission.release.model.SubmissionState;

@NoArgsConstructor(access = PRIVATE)
public final class States {

  public static State convert(@NonNull SubmissionState submissionState) {
    if (submissionState == SubmissionState.NOT_VALIDATED) {
      return State.NOT_VALIDATED;
    } else if (submissionState == SubmissionState.QUEUED) {
      return State.QUEUED;
    } else if (submissionState == SubmissionState.VALIDATING) {
      return State.VALIDATING;
    } else if (submissionState == SubmissionState.ERROR) {
      return State.ERROR;
    } else if (submissionState == SubmissionState.VALID) {
      return State.VALID;
    } else if (submissionState == SubmissionState.SIGNED_OFF) {
      return State.SIGNED_OFF;
    }

    throw new IllegalArgumentException("Cannot covert " + submissionState + " to " + State.class);
  }

  public static SubmissionState convert(@NonNull State state) {
    if (state == State.NOT_VALIDATED) {
      return SubmissionState.NOT_VALIDATED;
    } else if (state == State.QUEUED) {
      return SubmissionState.QUEUED;
    } else if (state == State.VALIDATING) {
      return SubmissionState.VALIDATING;
    } else if (state == State.ERROR) {
      return SubmissionState.ERROR;
    } else if (state == State.VALID) {
      return SubmissionState.VALID;
    } else if (state == State.SIGNED_OFF) {
      return SubmissionState.SIGNED_OFF;
    }

    throw new IllegalArgumentException("Cannot covert " + state + " to " + SubmissionState.class);
  }

}
