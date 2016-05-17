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
package org.icgc.dcc.submission.validation.core;

import static com.google.common.base.Stopwatch.createUnstarted;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.icgc.dcc.common.core.util.Formats.formatCount;
import static org.icgc.dcc.submission.validation.core.Validators.checkInterrupted;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.icgc.dcc.submission.validation.ValidationExecutor;

import com.google.common.base.Stopwatch;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

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
  private final ValidationContext validationContext;

  /**
   * Sequence of validators to apply to the {@link #validationContext}.
   */
  @NonNull
  private final List<Validator> validators;

  /**
   * Timer to record overall validation duration.
   */
  private final Stopwatch duration = createUnstarted();

  /**
   * Have all the validators completed?
   */
  private final AtomicBoolean completed = new AtomicBoolean(false);

  /**
   * The identifier used to {@code submit} and {@code cancel} with the {@link ValidationExecutor}.
   */
  public String getId() {
    return validationContext.getProjectKey();
  }

  /**
   * Have all the validators completed?
   */
  public boolean isCompleted() {
    return completed.get();
  }

  /**
   * Executes the sequence of {@link #validators}
   */
  @SneakyThrows
  public void execute() throws InterruptedException {
    duration.start();

    log.info(banner());
    log.info("");
    log.info("BEGIN VALIDATION: '{}'", getId());
    log.info("");
    log.info(banner());

    // Cooperate
    checkInterrupted(getClass().getSimpleName());

    val n = validators.size();
    int i = 1;
    String name = null;
    val watch = createUnstarted();

    try {
      for (val validator : validators) {
        name = validator.getName();

        log.info(banner());
        log.info("[" + i + "/" + n + "] > Starting '{}' for '{}'...", name, getId());
        log.info(banner());

        // Execute synchronously
        watch.reset().start();
        validator.validate(validationContext);
        watch.stop();

        log.info(banner());
        log.info("[" + i + "/" + n + "] < Finished '{}' for '{}' in {}", new Object[] { name, getId(), watch });
        log.info(banner());

        val failure = validationContext.hasErrors();
        if (failure) {
          log.warn("Execution of '{}' for '{}' has {} errors",
              new Object[] { name, getId(), formatCount(validationContext.getErrorCount()) });

          // Abort validation pipeline
          break;
        }

        // Cooperate
        checkInterrupted(getClass().getSimpleName());

        i++;
      }

      // Indicate that validation has completed all steps
      completed.set(i - 1 == n);
    } catch (Throwable t) {
      log.error(banner());
      log.error("[" + i + "/" + n + "] < Finished with Exception '{}' for '{}' in {}",
          new Object[] { name, getId(), watch });
      log.error(banner());

      log.error("Exception running validation for: " + getId(), t);

      throw t;
    } finally {
      duration.stop();

      log.info(banner());
      log.info("");
      log.info("FINISHED VALIDATION: '{}' in {}", getId(), duration);
      log.info("");
      log.info(banner());
    }
  }

  private static String banner() {
    return repeat("-", 80);
  }

}
