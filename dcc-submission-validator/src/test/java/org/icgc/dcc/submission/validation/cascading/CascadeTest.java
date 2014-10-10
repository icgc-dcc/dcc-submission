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
package org.icgc.dcc.submission.validation.cascading;

import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Lists;

@Slf4j
@Ignore
public class CascadeTest extends BaseCascadeTest {

  /**
   * Simulate max {@code validator.max_simultaneous}.
   */
  static final int MAX_SIMUTANEOUS = 3;

  @Test
  public void testGet() throws InterruptedException, ExecutionException {
    log.info("[Main] Executing action...");
    val executor = newFixedThreadPool(MAX_SIMUTANEOUS);
    val future = executeCascade(executor, new Runnable() {

      @Override
      public void run() {
        log.info("[Action] start");
        log.info("[Action] end");
      }

    });
    executor.shutdown();

    log.info("[Main] Getting future...");
    val throwable = future.get();
    log.info("[Main] Future got");

    // No throwable hopefully
    assertThat(throwable).isNull();
  }

  @Test(expected = CancellationException.class)
  public void testCancel() throws InterruptedException, ExecutionException {
    val executor = newFixedThreadPool(MAX_SIMUTANEOUS);
    val flag = new CountDownLatch(1);
    val future = executeCascade(executor, new Runnable() {

      @Override
      @SneakyThrows
      public void run() {
        // Flag flow is running
        log.info("[Action] Flagging...");
        flag.countDown();

        try {
          log.info("[Action] Start");
          sleepUninterruptibly(100, SECONDS);
          log.info("[Action] End");
        } catch (Throwable t) {
          log.error("[Action] Exception:", t);
          throw t;
        }
      }

    });
    executor.shutdown();

    // Wait for running flow
    log.info("[Main] Awaiting flag...");
    flag.await();

    log.info("[Main] Cancelling future...");
    val mayInterruptIfRunning = true;
    val cancelled = future.cancel(mayInterruptIfRunning);
    if (cancelled) {
      log.info("[Main] Future successfully cancelled");
    } else {
      log.info("[Main] Future not cancelled successfully");
    }

    // Should throw if cancelled
    future.get();
  }

  @Test
  public void testParallel() throws InterruptedException, ExecutionException, BrokenBarrierException {
    val jobs = MAX_SIMUTANEOUS;
    val barrier = new CyclicBarrier(jobs + 1);
    val count = new AtomicInteger();

    val futures = Lists.<Future<Throwable>> newArrayList();
    val executor = newFixedThreadPool(MAX_SIMUTANEOUS);
    for (int i = 0; i < jobs; i++) {
      futures.add(executeCascade(executor, new Runnable() {

        @Override
        @SneakyThrows
        public void run() {
          // Flag flow is running
          val id = count.incrementAndGet();
          log.info("[Action:{}] Awaiting barrier...", id);
          barrier.await();

          try {
            // Wait forever since we expect to be cancelled
            log.info("[Action:{}] Start", id);
            for (;;) {
            }
          } catch (Throwable t) {
            log.error("[Action:{}] Exception:", id, t);
            throw t;
          }
        }

      }));
    }
    executor.shutdown();

    // Wait for running flow
    log.info("[Main] Awaiting barrier...");
    barrier.await();

    for (val future : futures) {
      log.info("[Main] Cancelling future...");
      val mayInterruptIfRunning = true;
      val cancelled = future.cancel(mayInterruptIfRunning);
      if (cancelled) {
        log.info("[Main] Future successfully cancelled");
      } else {
        log.info("[Main] Future not cancelled successfully");
      }
    }
  }

}
