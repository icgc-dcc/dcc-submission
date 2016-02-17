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
package org.icgc.dcc.submission.validation;

import org.icgc.dcc.submission.validation.core.Validation;

/**
 * Abstraction that handles {@link ValidationExecutor} events.
 */
public interface ValidationListener {

  /**
   * Called at the beginning of a job.
   * <p>
   * The job has been accepted for immediate execution if this has been called.
   * 
   * @param validation the validation that is about to execute
   */
  void onStarted(Validation validation);

  /**
   * Called when a job is cancelled.
   * 
   * @param validation the validation that was successfully cancelled
   */
  void onCancelled(Validation validation);

  /**
   * Called when a job is completed.
   * 
   * @param validation the validation that successfully executed
   */
  void onEnded(Validation validation);

  /**
   * Called when a job fails.
   * 
   * @param validation the validation that failed and was unsuccessful
   * @param cause the cause of the failure
   */
  void onFailure(Validation validation, Throwable cause);

  /**
   * Default no-op listener that does nothing.
   * <p>
   * Useful for satisfying interfaces.
   */
  public static final ValidationListener NOOP_LISTENER = new ValidationListener() {

    @Override
    public void onStarted(Validation validation) {
      // No-op
    }

    @Override
    public void onEnded(Validation validation) {
      // No-op
    }

    @Override
    public void onCancelled(Validation validation) {
      // No-op
    }

    @Override
    public void onFailure(Validation validation, Throwable throwable) {
      // No-op
    }

  };

}
