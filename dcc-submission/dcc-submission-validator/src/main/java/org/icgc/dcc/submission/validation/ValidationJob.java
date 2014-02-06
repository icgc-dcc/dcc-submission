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
package org.icgc.dcc.submission.validation;

import static java.lang.String.format;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.core.Validation;

/**
 * An executing validation job.
 * <p>
 * Triggers {@link ValidationListener} events.
 */
@RequiredArgsConstructor
@Slf4j
public class ValidationJob implements Runnable {

  @NonNull
  private final String jobId;
  @NonNull
  private final Validation validation;
  @NonNull
  private final ValidationListener listener;

  @Override
  @SneakyThrows
  public void run() {
    try {

      //
      // Event: Started
      //

      // Execute the accept callback on the same thread as the validation
      log.info("job: Starting validation '{}'...", jobId);
      listener.onStarted(validation);

      //
      // Event: Executing (no callback)
      //

      log.info("job: Executing validation '{}'...", jobId);
      validation.execute();

      //
      // Event: Completion
      //

      log.info("job: Completing validation '{}'...", jobId);
      listener.onCompletion(validation);
    } catch (InterruptedException e) {

      //
      // Event: Cancelled
      //

      log.info("job: Cancelling validation '{}'...", jobId);
      listener.onCancelled(validation);
    } catch (Throwable t) {

      //
      // Event: Failure
      //

      log.error(format("job: Failing validation '%s'...", jobId), t);
      listener.onFailure(validation, t);
    }

    log.info("job: Exiting validation. '{}' duration: {} ms", jobId, validation.getDuration());
  }

}