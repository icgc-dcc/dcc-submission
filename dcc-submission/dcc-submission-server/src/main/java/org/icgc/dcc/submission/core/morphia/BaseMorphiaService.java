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
package org.icgc.dcc.submission.core.morphia;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.Callable;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.core.model.DccConcurrencyException;
import org.icgc.dcc.submission.core.model.DccModelOptimisticLockException;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Predicate;

@Slf4j
public abstract class BaseMorphiaService<T> {

  /**
   * The number of attempts that should be sufficient to obtain a lock. Otherwise the problem is probably not
   * recoverable - deadlock or other.
   */
  protected static final int MAX_ATTEMPTS = 10;

  private final Morphia morphia;
  private final Datastore datastore;
  private final EntityPath<T> entityPath;

  protected final MailService mailService;

  @Inject
  public BaseMorphiaService(Morphia morphia, Datastore datastore, EntityPath<T> entityPath, MailService mailService) {
    this.morphia = checkNotNull(morphia);
    this.datastore = checkNotNull(datastore);
    this.entityPath = checkNotNull(entityPath);
    this.mailService = checkNotNull(mailService);
  }

  public Datastore datastore() {
    return datastore;
  }

  public Morphia morphia() {
    return morphia;
  }

  public MongodbQuery<T> query() {
    return new MorphiaQuery<T>(morphia(), datastore(), entityPath);
  }

  public MongodbQuery<T> where(Predicate predicate) {
    return query().where(predicate);
  }

  protected void registerModelClasses(Class<?>... entities) {
    if (entities != null) {
      for (Class<?> e : entities) {
        morphia.map(e);
        datastore.ensureIndexes(e);
      }
    }
  }

  /**
   * Calls the supplied {@code callback} a "reasonable" number times until a {@link DccModelOptimisticLockException} is
   * not thrown. If a retry fails, an email will be sent.
   * 
   * @param description - a description of what the {@code callback} does
   * @param callback - the action to perform with retry
   * @return the return value of the {@code callback}
   */
  @SneakyThrows
  protected <R> Optional<R> withRetry(String description, Callable<R> callback) {
    int attempts = 0;
    while (attempts < MAX_ATTEMPTS) {
      try {
        return Optional.<R> fromNullable(callback.call());
      } catch (DccModelOptimisticLockException e) {
        attempts++;

        log.warn("There was a concurrency issue while attempting to {}, number of attempts: {}", description, attempts);
        sleepUninterruptibly(1, SECONDS);
      }
    }
    if (attempts >= MAX_ATTEMPTS) {
      String message = format("Failed to %s, could not acquire lock", description);
      mailService.sendAdminProblem(message);

      throw new DccConcurrencyException(message);
    }

    return Optional.absent();
  }

}
