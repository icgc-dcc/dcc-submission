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
package org.icgc.dcc.submission.validation.service;

import static com.google.common.collect.Maps.newConcurrentMap;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Manages the execution and cancellation of a fixed number of {@code Validation} "slots".
 */
@Slf4j
public class ValidationExecutor {

  /**
   * Bookkeeping for canceling, indexed by {@link Validation#getId()}.
   */
  private final ConcurrentMap<String, Future<?>> futures = newConcurrentMap();

  /**
   * The executor used to execute validation tasks.
   */
  private final ThreadPoolExecutor executor;

  /**
   * Creates a validator with the supplied number of validation "slots".
   * 
   * @param maxConcurrentValidations - The maximum number of concurrently executing validation tasks.
   */
  public ValidationExecutor(int maxConcurrentValidations) {
    log.info("Initializing executor with {} maximum concurrent validations", maxConcurrentValidations);
    this.executor = createExecutor(maxConcurrentValidations);
  }

  /**
   * Execute a validation job asynchronously.
   * <p>
   * Uses {@link Validation#getId()} to identify in a {@link #cancel} call.
   * 
   * @param validation
   * @throws RejectedExecutionException if there are no "slots" available
   */
  public ListenableFuture<Validation> execute(final Validation validation) {
    val id = validation.getId();

    log.info("execute: Submitting validation '{}' ... {}", id, getStats());
    val future = listeningDecorator(executor).submit(new Callable<Validation>() {

      @Override
      @SneakyThrows
      public Validation call() throws Exception {
        try {
          log.info("call: Executing validation '{}'... {}", id, getStats());
          validation.execute();
          log.info("call: Finished executing validation '{}'... {}", id, getStats());
        } catch (Throwable t) {
          log.error("call: Exception executing validation '{}' {}: {}", new Object[] { id, getStats(), t });

          // Propagate for {@link ListeningFuture#onError()}
          throw t;
        } finally {
          futures.remove(id);
        }

        // Make available for {@link ListeningFuture#onSuccess()}
        log.info("call: Exiting validation '{}' without error... {}", id, getStats());
        return validation;
      }

    });

    // Track it for cancellation
    futures.put(id, future);

    return future;
  }

  /**
   * Cancel a running validation.
   * <p>
   * Will return {@code false} if the task has already completed, has already been cancelled, or could not be found
   * 
   * @param id the id of the task
   * @return whether the task was successfully cancelled
   */
  public boolean cancel(String id) {
    try {
      val future = futures.get(id);
      val available = future != null;
      if (available) {
        log.warn("cancel: Cancelling validation '{}'... {}", id, getStats());
        val cancelled = future.cancel(true);
        log.warn("cancel: Finshed cancelling validation '{}', cancelled: {}... {}",
            new Object[] { id, cancelled, getStats() });

        return cancelled;
      }

    } finally {
      // No project left behind
      futures.remove(id);
    }

    log.error("cancel: No validation found '{}'... {}", id, getStats());
    return false;
  }

  /**
   * Shuts down the internal executor.
   */
  public void shutdown() {
    log.info("Shutting down executor...");
    executor.shutdownNow();
  }

  /**
   * Creates the internal executor with the appropriate application semantics.
   * 
   * @param maxConcurrentValidations
   * @return
   */
  private ThreadPoolExecutor createExecutor(int maxConcurrentValidations) {
    // Bind all pool sizes to this value
    val poolSize = maxConcurrentValidations;

    // From the Javadoc:
    //
    // "A blocking queue in which each insert operation must wait for a corresponding remove operation by another
    // thread, and vice versa. A synchronous queue does not have any internal capacity, not even a capacity of one."
    //
    // We need these semantics because we want the total number of "slots" to be as defined in the constructor. This
    // ensures the queue length is always zero.
    val queue = new SynchronousQueue<Runnable>();

    return new ThreadPoolExecutor(
        poolSize, poolSize, // Core and max are the same
        0, SECONDS, // Arbitrary when both pool sizes are the same
        queue,

        // Need this to get a customized exception when "slots" are full
        new RejectedExecutionHandler() {

          @Override
          public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            log.info("Rejecting... {}", getStats());

            // Raison d'être
            throw new ValidationRejectedException();
          }

        });
  }

  /**
   * Gets basic task statistics about the underlying executor.
   * 
   * @return a formatted stats string
   */
  private String getStats() {
    return format("taskCount: %s, activeCount: %s, completedCount: %s",
        executor.getTaskCount(), executor.getActiveCount(), executor.getCompletedTaskCount());
  }

}
