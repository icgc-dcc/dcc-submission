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

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.submission.validation.ValidationListener.NOOP_LISTENER;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.concurrent.ThreadSafe;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.util.ThreadNamingRunnable;
import org.icgc.dcc.submission.validation.core.Validation;

import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

/**
 * Manages the execution and cancellation of a fixed number of {@code Validation} "slots".
 * <p>
 * Similar to the standard JDK {@link ExecutorService} abstraction. Delegates to a fixed thread pool executor and
 * provides asynchronous callbacks for execution outcomes.
 */
@Slf4j
@ThreadSafe
@RequiredArgsConstructor(onConstructor = @___(@Inject))
public class ValidationExecutor {

  /**
   * The maximum number of concurrently executing validation jobs.
   * <p>
   * The number of validation "slots".
   */
  private final int maxConcurrentValidations;

  /**
   * The delegate thread pool used to execute validation jobs.
   */
  @Getter(lazy = true, value = PRIVATE)
  private final ThreadPoolExecutor jobPool = createExecutor(maxConcurrentValidations);

  /**
   * Bookkeeping for canceling, indexed by {@link ValidationJob#getJobId()}.
   * <p>
   * Is thread-safe and uses weak references to automatically remove entries when finished.
   */
  @Getter(lazy = true, value = PRIVATE)
  private final Map<String, Future<?>> jobHandles = new MapMaker().weakValues().makeMap();

  /**
   * Returns the number of active validation "slots".
   */
  public int getActiveCount() {
    return getJobPool().getActiveCount();
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
   * @param listener validation listener to callback on validation lifecycle events
   */
  public void execute(@NonNull Validation validation, @NonNull ValidationListener listener) {
    val jobId = validation.getId();

    log.info("execute: Submitting validation job '{}' ... {}", jobId, formatStats());
    val job = new ValidationJob(jobId, validation, listener);
    val jobHandle = submit(jobId, job);

    // Track it for future cancellation purposes
    getJobHandles().put(jobId, jobHandle);
  }

  /**
   * Cancel a running validation.
   * <p>
   * Will return {@code false} if the job has already completed, has already been cancelled, or could not be found
   * 
   * @see https://issues.apache.org/jira/browse/HDFS-1208
   * @param jobId the id of the job
   * @return whether the job was successfully cancelled (may still be running, see HDFS-1208)
   */
  public boolean cancel(@NonNull String jobId) {
    val jobHandle = getJobHandles().get(jobId);
    val available = jobHandle != null;
    if (available) {
      log.warn("cancel: Cancelling validation job '{}'... {}", jobId, formatStats());
      val cancelled = jobHandle.cancel(true);
      log.warn("cancel: Finished cancelling validation job '{}'. cancelled = {} {}",
          new Object[] { jobId, cancelled, formatStats() });

      return cancelled;
    }

    log.warn("cancel: No validation found '{}'... {}", jobId, formatStats());
    return false;
  }

  /**
   * Shuts down the internal pool.
   */
  public void shutdown() {
    log.info("Shutting down pool...");
    getJobPool().shutdownNow();
  }

  /**
   * Submits the supplied {@code job} for execution by the underlying executor.
   * 
   * @param jobId the id of the job to submit
   * @param job the job to submit
   * @return the "promise" of the result
   */
  private Future<?> submit(@NonNull String jobId, @NonNull Runnable job) {
    // Change the thread name of the executing job to be named {@code name}.
    // This makes logs easier to trace andanalyze
    val namedJob = new ThreadNamingRunnable(jobId, job);

    // Delegate to the pool executor and capture the future (a.k.a "promise") result
    val jobHandle = getJobPool().submit(namedJob);

    return jobHandle;
  }

  /**
   * Gets basic job statistics about the underlying pool.
   * 
   * @return a formatted statistics string
   */
  private String formatStats() {
    return format("Executing job(s): %s job", getJobPool().getActiveCount());
  }

  /**
   * Creates the internal thread pool with the appropriate application semantics.
   * @return the thread pool
   */
  private static ThreadPoolExecutor createExecutor(int maxConcurrentValidations) {
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
        new ThreadFactoryBuilder().setNameFormat("validation-slot-%s").build(),

        // Need this to get a customized exception when "slots" are full
        new RejectedExecutionHandler() {

          @Override
          public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            val message = format("Pool limit of %s concurrent validations reached. Validation rejected.", poolSize);
            log.warn(message);

            // Raison d'être
            throw new ValidationRejectedException(message);
          }

        });
  }

}
