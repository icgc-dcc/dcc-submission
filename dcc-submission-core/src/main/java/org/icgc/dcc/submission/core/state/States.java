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

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.icgc.dcc.submission.release.model.SubmissionState;

/**
 * State utilities.
 */
@NoArgsConstructor(access = PRIVATE)
public final class States {

  /**
   * State constants.
   */
  public static final State NOT_VALIDATED = new NotValidatedState();
  public static final State QUEUED = new QueuedState();
  public static final State VALIDATING = new ValidatingState();
  public static final State INVALID = new InvalidState();
  public static final State ERROR = new ErrorState();
  public static final State VALID = new ValidState();
  public static final State SIGNED_OFF = new SignedOffState();

  public static SubmissionState convert(@NonNull State state) {
    if (state == NOT_VALIDATED) {
      return SubmissionState.NOT_VALIDATED;
    } else if (state == QUEUED) {
      return SubmissionState.QUEUED;
    } else if (state == VALIDATING) {
      return SubmissionState.VALIDATING;
    } else if (state == INVALID) {
      return SubmissionState.INVALID;
    } else if (state == ERROR) {
      return SubmissionState.ERROR;
    } else if (state == VALID) {
      return SubmissionState.VALID;
    } else if (state == SIGNED_OFF) {
      return SubmissionState.SIGNED_OFF;
    } else {
      throw new IllegalArgumentException("Cannot covert " + state + " to " + SubmissionState.class);
    }
  }

}
