/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.core;

import static com.google.common.base.Stopwatch.createUnstarted;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.icgc.dcc.submission.validation.core.Validators.checkState;

import java.util.List;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.ValidationExecutor;

import com.google.common.base.Stopwatch;

/**
 * A {@code Validation} is a high level container which encapsulates the execution context of sequentially executed
 * {@link Validator}s.
 */
@Value
@Slf4j
public class Validation {

  /**
   * The per-instance context of the validation.
   */
  @NonNull
  private final ValidationContext context;

  /**
   * Sequence of validators to apply to the {@link #context}.
   */
  @NonNull
  private final List<Validator> validators;

  /**
   * Timer to record overall validation duration.
   */
  private final Stopwatch duration = createUnstarted();

  /**
   * The identifier used to {@code submit} and {@code cancel} with the {@link ValidationExecutor}.
   */
  public String getId() {
    return context.getProjectKey();
  }

  /**
   * Executes the sequence of {@link #validators}
   */
  @SneakyThrows
  public void execute() throws InterruptedException {
    duration.start();
    try {
      log.info(repeat("=", 80));
      log.info("");
      log.info("BEGIN VALIDATION: '{}'", getId());
      log.info("");
      log.info(repeat("=", 80));

      // Cooperate
      checkState(getClass().getSimpleName());

      val n = validators.size();
      int i = 1;
      for (val validator : validators) {
        val name = validator.getName();

        log.info(repeat("-", 80));
        log.info("[" + i++ + "/" + n + "] Executing '{}' for '{}'...", name, getId());
        log.info(repeat("-", 80));
        validator.validate(context);
        log.info("Finished executing '{}' for '{}'...", name, getId());

        val failure = context.hasErrors();
        if (failure) {
          log.warn("Execution of '{}' for '{}' has '{}' errors",
              new Object[] { name, getId(), context.getErrorCount() });

          // Fail fast
          break;
        }

        // Cooperate
        checkState(getClass().getSimpleName());
      }
    } catch (Throwable t) {
      log.error("Exception running validation for '{}': {}", getId(), t);

      // For {@link ListeningFuture#onError()}
      throw t;
    } finally {
      duration.stop();

      log.info(repeat("=", 80));
      log.info("");
      log.info("FINISHED VALIDATION: '{}' in {}", getId(), duration);
      log.info("");
      log.info(repeat("=", 80));
    }
  }

}
