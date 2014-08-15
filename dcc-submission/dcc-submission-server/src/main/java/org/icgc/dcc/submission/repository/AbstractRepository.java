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
package org.icgc.dcc.submission.repository;

import static com.mongodb.WriteConcern.ACKNOWLEDGED;
import static lombok.AccessLevel.PROTECTED;

import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;

@Accessors(fluent = true)
public abstract class AbstractRepository<E, Q extends EntityPath<E>> {

  /**
   * Dependencies.
   */
  @Getter(PROTECTED)
  private final Morphia morphia;
  @Getter(PROTECTED)
  private final Datastore datastore;

  /**
   * Query DSL entity path.
   * <p>
   * Named {@code _} to be idiomatic in derived classes.
   */
  protected final Q _;

  @Inject
  public AbstractRepository(@NonNull Morphia morphia, @NonNull Datastore datastore, @NonNull Q entityPath) {
    this.morphia = morphia;
    this.datastore = datastore;
    this._ = entityPath;

    registerEntityType(entityPath.getType());
  }

  protected MongodbQuery<E> query() {
    return new MorphiaQuery<E>(morphia(), datastore(), _);
  }

  protected MongodbQuery<E> where(@NonNull Predicate predicate) {
    return query().where(predicate);
  }

  protected long count() {
    return query().count();
  }

  protected long count(@NonNull Predicate predicate) {
    return where(predicate).count();
  }

  protected E uniqueResult(@NonNull Predicate predicate, Path<?>... paths) {
    return where(predicate).uniqueResult(paths);
  }

  protected E singleResult(@NonNull Predicate predicate, Path<?>... paths) {
    return where(predicate).singleResult(paths);
  }

  protected List<E> list(Path<?>... paths) {
    return list(query(), paths);
  }

  protected List<E> list(@NonNull Predicate predicate, Path<?>... paths) {
    return list(where(predicate), paths);
  }

  protected List<E> list(@NonNull MongodbQuery<E> query, Path<?>... paths) {
    return ImmutableList.copyOf(query.list(paths));
  }

  protected Query<E> createQuery() {
    return datastore().createQuery(getEntityType());
  }

  protected UpdateOperations<E> createUpdateOperations() {
    return datastore().createUpdateOperations(getEntityType());
  }

  protected static String fieldName(@NonNull Path<?> path) {
    return path.getMetadata().getName();
  }

  /**
   * This is currently necessary in order to use the {@code field.$.nestedField} notation in updates. Otherwise one gets
   * an error like:<br>
   * <blockquote> The field '$' could not be found in 'org.icgc.dcc.submission.release.model.Release' while validating -
   * submissions.$.state; if you wish to continue please disable validation. </blockquote>
   * 
   * @see https://groups.google.com/d/msg/morphia/ta-qd_XrgaE/hO7KTjPWNyEJ
   */
  protected UpdateOperations<E> createUpdateOperations$() {
    return createUpdateOperations().disableValidation();
  }

  protected <R> UpdateResults<R> update(@NonNull Query<R> query, @NonNull UpdateOperations<R> ops) {
    return datastore().update(query, ops);
  }

  protected <R> UpdateResults<R> updateFirst(@NonNull Query<R> query, @NonNull R entity, boolean createIfMissing) {
    return datastore().updateFirst(query, entity, createIfMissing);
  }

  protected <R> R findAndModify(@NonNull Query<R> query, @NonNull UpdateOperations<R> ops) {
    return datastore().findAndModify(query, ops);
  }

  protected <R> R findAndModify(@NonNull Query<R> query, @NonNull UpdateOperations<R> ops, boolean oldVersion,
      boolean createIfMissing) {
    return datastore().findAndModify(query, ops, oldVersion, createIfMissing);
  }

  protected <R> Key<R> save(@NonNull R entity) {
    return datastore().save(entity, ACKNOWLEDGED);
  }

  protected <R> Iterable<Key<R>> save(@NonNull Iterable<R> entities) {
    return datastore().save(entities, ACKNOWLEDGED);
  }

  private void registerEntityType(Class<?> entityType) {
    morphia().map(entityType);
    datastore().ensureIndexes(entityType);
  }

  @SuppressWarnings("unchecked")
  private Class<E> getEntityType() {
    return (Class<E>) _.getType();
  }

}
