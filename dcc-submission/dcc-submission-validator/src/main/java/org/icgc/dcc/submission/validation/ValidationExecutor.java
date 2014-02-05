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
package org.icgc.dcc.submission.validation;

import static com.google.common.collect.Maps.newConcurrentMap;
import static com.google.common.util.concurrent.MoreExecutors.listeningDecorator;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.icgc.dcc.submission.validation.ValidationListener.NOOP_LISTENER;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.core.Validation;
import org.icgc.dcc.submission.validation.util.NamingCallable;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Manages the execution and cancellation of a fixed number of {@code Validation} "slots".
 * <p>
 * Similar to the standard JDK {@link ExecutorService} abstraction. Delegates to a fixed thread pool executor and
 * provides asynchronous callbacks for execution outcomes.
 */
@Slf4j
public class ValidationExecutor {

  /**
   * Bookkeeping for canceling, indexed by {@link Validation#getId()}.
   */
  private final ConcurrentMap<String, Future<?>> validationFutures = newConcurrentMap();

  /**
   * The pool used to execute validation tasks.
   */
  private final ThreadPoolExecutor pool;

  /**
   * Creates a validator with the supplied number of validation "slots".
   * 
   * @param maxConcurrentValidations The maximum number of concurrently executing validation tasks.
   */
  public ValidationExecutor(int maxConcurrentValidations) {
    log.info("Initializing pool with a maximum of {} concurrent validations", maxConcurrentValidations);
    this.pool = createExecutor(maxConcurrentValidations);
  }

  /**
   * Returns the number of active validation "slots".
   */
  public int getActiveCount() {
    return pool.getActiveCount();
  }

  /**
   * Execute a validation job asynchronously.
   * <p>
   * Uses {@link Validation#getId()} to identify in a {@link #cancel} call.
   * 
   * @param validation
   * @throws RejectedExecutionException if there are no "slots" available
   */
  public void execute(@NonNull Validation validation) {
    execute(validation, NOOP_LISTENER);
  }

  /**
   * Execute a validation job asynchronously.
   * <p>
   * Uses {@link Validation#getId()} to identify in a {@link #cancel} call.
   * 
   * @param validation the validation job to run. {@link Validation#execute()} is called asynchronously with respect to
   * the caller upon successful submission.
   * @param acceptedCallback a callback that executes asynchronously with respect to the caller when a validation is
   * accepted. Guaranteed to run before the {@code validation}'s {@link Validation#execute()} call.
   * @param completedCallback a callback that executes asynchronously with respect to the caller when a validation is
   * completed. Guaranteed to run after the {@code validation}'s {@link Validation#execute()} call.
   * @throws RejectedExecutionException if there are no "slots" available
   */
  public void execute(final @NonNull Validation validation, final @NonNull ValidationListener listener) {
    val id = validation.getId();

    // Need to apply listening decorator here because we still need access to pool methods later
    log.info("execute: Submitting validation '{}' ... {}", id, getStats());
    val validationFuture = submit(id, new Callable<Validation>() {

      @Override
      @SneakyThrows
      public Validation call() throws Exception {
        try {
          // Execute the accept callback on the same thread as the validation
          log.info("call: Executing validation accepted callback '{}'... {}", id, getStats());
          listener.onStarted(validation);
          log.info("call: Finished validation accepted callback '{}'. {}", id, getStats());

          log.info("call: Executing validation '{}'... {}", id, getStats());
          validation.execute();
          log.info("call: Finished executing validation '{}'. {}", id, getStats());
        } catch (Throwable t) {
          log.error(format("call: Exception executing validation '%s': %s", id, getStats()), t);

          // Propagate for {@link ListeningFuture#onError()}
          throw t;
        } finally {
          log.info("call: Validation '{}' duration: {} ms", id, validation.getDuration());

          // Untrack the result since we are finished and cancellation can no longer occur
          validationFutures.remove(id);
        }

        // Make available for {@link ListeningFuture#onSuccess()}
        log.info("call: Exiting validation '{}' without error... {}", id, getStats());
        return validation;
      }

    });

    // Register behavior to run upon completion
    addOutcomeCallbacks(validation, listener, validationFuture);

    // Track it for future cancellation purposes
    validationFutures.put(id, validationFuture);
  }

  /**
   * Add outcome callbacks to completion event of the future's result
   * 
   * @param validation the executing validation
   * @param listener the listener to attach callbacks
   * @param validationFuture the future validation result
   */
  private void addOutcomeCallbacks(
      final Validation validation,
      final ValidationListener listener,
      final ListenableFuture<Validation> validationFuture) {
    Futures.addCallback(validationFuture, new FutureCallback<Validation>() {

      @Override
      public void onSuccess(Validation result) {
        listener.onCompletion(validation);
      }

      @Override
      public void onFailure(Throwable t) {
        if (t instanceof CancellationException) {
          listener.onCancelled(validation);
        } else {
          listener.onFailure(validation, t);
        }
      }

    });
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
      val validationFuture = validationFutures.get(id);
      val available = validationFuture != null;
      if (available) {
        log.warn("cancel: Cancelling validation '{}'... {}", id, getStats());
        val cancelled = validationFuture.cancel(true);
        log.warn("cancel: Finshed cancelling validation '{}', cancelled: {}... {}",
            new Object[] { id, cancelled, getStats() });

        return cancelled;
      }

    } finally {
      // No project left behind
      validationFutures.remove(id);
    }

    log.warn("cancel: No validation found '{}'... {}", id, getStats());
    return false;
  }

  /**
   * Shuts down the internal pool.
   */
  public void shutdown() {
    log.info("Shutting down pool...");
    pool.shutdownNow();
  }

  /**
   * Submits the supplied {@code validationCallable} job for execution by the underlying executor.
   * 
   * @param threadName the name of the thread that executes the callback
   * @param validationCallable the validation job to execute
   * @return
   */
  private ListenableFuture<Validation> submit(@NonNull String threadName,
      @NonNull Callable<Validation> validationCallable) {
    // Change the thread name of the executing callback to be named {@code name}. This makes logs easier to trace and
    // analyze
    val namedValidationCallable = new NamingCallable<Validation>(threadName, validationCallable);

    // Wrap the underlying thread pool executor with Guava's listening decorator that exposes {@code FutureCallback}
    // semantics not provided in the standard JDK versions. This needs to be applied here and not during initialization
    // since we still need the static type of {@link #pool} to access other methods (e.g. see {@link #shutdown})
    val listeningExecutor = listeningDecorator(pool);

    // Delegate to the modified executor and capture the future (a.k.a "promise") result for which to attach callbacks
    val validationFuture = listeningExecutor.submit(namedValidationCallable);

    return validationFuture;
  }

  /**
   * Creates the internal pool with the appropriate application semantics.
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
    // ensures the queue length is always zero and the thread pool size at most {@param maxConcurrentValidations}
    // threads.
    val queue = new SynchronousQueue<Runnable>();

    return new ThreadPoolExecutor(
        poolSize, poolSize, // Core and max are the same
        0, SECONDS, // Arbitrary when both pool sizes are the same
        queue,

        // Name the threads for logging and diagnostics
        new ThreadFactoryBuilder().setNameFormat("validation-%s").build(),

        // Need this to get a customized exception when "slots" are full
        new RejectedExecutionHandler() {

          @Override
          public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            val message = format("Pool limit of %s concurrent validations reached. Validation rejected. %s",
                poolSize, getStats());
            log.warn(message);

            // Raison d'être
            throw new ValidationRejectedException(message);
          }

        });
  }

  /**
   * Gets basic task statistics about the underlying pool.
   * 
   * @return a formatted statistics string
   */
  private String getStats() {
    return format("taskCount: %s, activeCount: %s, completedCount: %s",
        pool.getTaskCount(), pool.getActiveCount(), pool.getCompletedTaskCount());
  }

}
