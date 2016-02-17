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
package org.icgc.dcc.submission.service;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.Callable;

import org.icgc.dcc.submission.core.model.DccConcurrencyException;
import org.icgc.dcc.submission.core.model.DccModelOptimisticLockException;

import com.google.common.base.Optional;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AbstractService {

  /**
   * The number of attempts that should be sufficient to obtain a lock. Otherwise the problem is probably not
   * recoverable - deadlock or other.
   */
  public static final int MAX_ATTEMPTS = 10;

  /**
   * Dependencies.
   */
  @NonNull
  protected final MailService mailService;

  protected <R> Optional<R> withRetry(String description, Callable<R> callback) {
    return withRetry(description, callback, mailService);
  }

  /**
   * Calls the supplied {@code callback} a "reasonable" number times until a {@link DccModelOptimisticLockException} is
   * not thrown. If a retry is exhausted, an "admin problem" email will be sent.
   * 
   * @param description - a description of what the {@code callback} does
   * @param callback - the action to perform with retry
   * @return the return value of the {@code callback}
   */
  @SneakyThrows
  public static <R> Optional<R> withRetry(String description, Callable<R> callback, MailService mailService) {
    int attempts = 0;
    while (attempts < MAX_ATTEMPTS) {
      try {
        return fromNullable(callback.call());
      } catch (DccModelOptimisticLockException e) {
        attempts++;

        log.warn("There was a concurrency issue while attempting to {}, number of attempts: {}", description, attempts);
        sleepUninterruptibly(1, SECONDS);
      } catch (Throwable t) {
        log.error("Error attempting {}: {}", description, t.getMessage());
        throw t;
      }
    }
    if (attempts >= MAX_ATTEMPTS) {
      val message = format("Failed to %s, could not acquire lock", description);
      mailService.sendSupportProblem(message, message);

      throw new DccConcurrencyException(message);
    }

    return absent();
  }

}
