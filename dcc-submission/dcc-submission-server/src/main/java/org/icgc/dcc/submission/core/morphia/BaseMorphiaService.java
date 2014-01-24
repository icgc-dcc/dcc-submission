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
import static com.mongodb.WriteConcern.ACKNOWLEDGED;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.concurrent.Callable;

import org.icgc.dcc.submission.core.AbstractService;
import org.icgc.dcc.submission.core.MailService;
import org.icgc.dcc.submission.core.model.DccModelOptimisticLockException;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Key;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.code.morphia.query.UpdateResults;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Predicate;

public abstract class BaseMorphiaService<T> {

  /**
   * Dependencies.
   */
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

  protected void registerModelClasses(Class<?>... entities) {
    if (entities != null) {
      for (Class<?> e : entities) {
        morphia.map(e);
        datastore.ensureIndexes(e);
      }
    }
  }

  protected Datastore datastore() {
    return datastore;
  }

  protected Morphia morphia() {
    return morphia;
  }

  protected MongodbQuery<T> query() {
    return new MorphiaQuery<T>(morphia(), datastore(), entityPath);
  }

  protected MongodbQuery<T> where(Predicate predicate) {
    return query().where(predicate);
  }

  protected Query<T> select() {
    return datastore().createQuery(getEntityClass());
  }

  protected <R> UpdateResults<R> update(Query<R> query, UpdateOperations<R> ops) {
    return datastore().update(query, ops);
  }

  protected <R> UpdateResults<R> updateFirst(Query<R> query, R entity, boolean createIfMissing) {
    return datastore().updateFirst(query, entity, createIfMissing);
  }

  protected <R> R findAndModify(Query<R> query, UpdateOperations<R> ops) {
    return datastore().findAndModify(query, ops);
  }

  protected <R> Key<R> save(R entity) {
    return datastore().save(entity, ACKNOWLEDGED);
  }

  protected <R> List<R> list(List<R> entities) {
    return ImmutableList.copyOf(entities);
  }

  protected UpdateOperations<T> updateOperations() {
    return datastore().createUpdateOperations(getEntityClass());
  }

  /**
   * This is currently necessary in order to use the <i>field.$.nestedField</i> notation in updates. Otherwise one gets
   * an error like <i>
   * "The field '$' could not be found in 'org.icgc.dcc.submission.release.model.Release' while validating - submissions.$.state; if you wish to continue please disable validation."
   * </i>
   * <p>
   * For more information, see
   * http://groups.google.com/group/morphia/tree/browse_frm/month/2011-01/489d5b7501760724?rnum
   * =31&_done=/group/morphia/browse_frm/month/2011-01?
   */
  protected UpdateOperations<T> updateOperations$() {
    return updateOperations().disableValidation();
  }

  /**
   * Calls the supplied {@code callback} a "reasonable" number times until a {@link DccModelOptimisticLockException} is
   * not thrown. If a retry is exhausted, an "admin problem" email will be sent.
   * 
   * @param description - a description of what the {@code callback} does
   * @param callback - the action to perform with retry
   * @return the return value of the {@code callback}
   */
  protected <R> Optional<R> withRetry(String description, Callable<R> callback) {
    return AbstractService.withRetry(description, callback, mailService);
  }

  @SuppressWarnings("unchecked")
  private Class<T> getEntityClass() {
    // Get reified generic super class
    return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
  }
}
