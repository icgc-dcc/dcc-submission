/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.hadoop.cascading.taps;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.icgc.dcc.core.model.FieldNames.MONGO_INTERNAL_ID;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import cascading.flow.FlowProcess;
import cascading.scheme.Scheme;
import cascading.scheme.SinkCall;
import cascading.scheme.SourceCall;
import cascading.tap.SinkTap;
import cascading.tap.Tap;
import cascading.tuple.TupleEntry;
import cascading.tuple.TupleEntryCollector;
import cascading.tuple.TupleEntrySchemeCollector;

import com.google.common.base.Throwables;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

/**
 * See https://github.com/ifesdjeen/cascading-mongodb/blob/master/src/main/java/com/clojurewerz/cascading/mongodb/
 * MongoDBTap.java for inspiration.
 */
@Slf4j
public class MongoDbTap extends SinkTap<Object, Object> {

  private final String mongoURI;

  public MongoDbTap(
      Scheme<Object, Void, Object, ?, ?> scheme,
      @NonNull final String mongoURI) {

    super(scheme);
    this.mongoURI = checkNotNull(mongoURI);
  }

  @Override
  public String getIdentifier() {
    return mongoURI;
  }

  @Override
  public TupleEntryCollector openForWrite(FlowProcess<Object> flowProcess, Object output) throws IOException {
    return new MongoTupleEntrySchemeCollector(flowProcess, getScheme(), mongoClient(), getIdentifier());
  }

  @Override
  public boolean createResource(Object conf) throws IOException {
    return false;
  }

  @Override
  public boolean deleteResource(Object conf) throws IOException {
    return false;
  }

  @Override
  public boolean resourceExists(Object conf) throws IOException {
    return false;
  }

  @Override
  public long getModifiedTime(Object conf) throws IOException {
    return 0;
  }

  /**
   * Used in {@link MongoDbScheme#mongoClient()}, closed in {@link #commitResource(Object)} or
   * {@link #rollbackResource(Object)}.
   */
  @SneakyThrows
  private Mongo mongoClient() {
    return new MongoClient(new MongoClientURI(mongoURI));
  }

  /**
   * Custom {@link TupleEntrySchemeCollector} so that the {@link Mongo#close()} method can be called.
   * {@link TupleEntrySchemeCollector#close()} would do that on our behalf if {@link Mongo} implemented
   * {@link Closeable}, unfortunately it does not as of version 2.11.0.
   */
  private static class MongoTupleEntrySchemeCollector extends TupleEntrySchemeCollector<Object, Mongo> {

    public MongoTupleEntrySchemeCollector(
        FlowProcess<Object> flowProcess,
        Scheme<Object, Void, Object, ?, ?> scheme,
        Mongo output,
        String identifier) {
      super(flowProcess, scheme, output, identifier);
    }

    @Override
    public void close() {
      try {
        log.info("Closing mongodb connection");
        getOutput().close();
      } finally {
        super.close();
      }
    }

  }

  public static interface RecordSaveCallback extends Serializable {

    void onSaved(DBObject object, DBCollection collection);

    void onComplete(DBCollection dbCollection);

  }

  /**
   * See https://github.com/ifesdjeen/cascading-mongodb/blob/master/src/main/java/com/clojurewerz/cascading/mongodb/
   * MongoDBScheme.java for inspiration.
   */
  @Slf4j
  @RequiredArgsConstructor
  public static abstract class MongoDbScheme extends Scheme<Object, Void, Object, Void, Void> {// TODO: keep abstract?

    protected final String databaseName;
    protected final String counterName; // TODO: make optional
    private final RecordSaveCallback callback; // TODO: make optional

    protected transient DBCollection dbCollection;

    @Override
    public void sinkPrepare(FlowProcess<Object> flowProcess, SinkCall<Void, Object> sinkCall) throws IOException {
      super.sinkPrepare(flowProcess, sinkCall);

      log.info("Setting collection '{}'", getCollectionName());
      this.dbCollection = getDbCollection(sinkCall, databaseName, getCollectionName());
    }

    protected abstract String getCollectionName();

    @Override
    public boolean source(FlowProcess<Object> flowProcess, SourceCall<Void, Void> sourceCall) throws IOException {
      return false;
    }

    @Override
    public void sink(FlowProcess<Object> flowProcess, SinkCall<Void, Object> sinkCall) throws IOException {
      preSinkHook(sinkCall);
      TupleEntry entry = sinkCall.getOutgoingEntry();
      persistDocument(sinkCall, entry);
      flowProcess.increment(databaseName, counterName, 1);
    }

    protected abstract void preSinkHook(SinkCall<Void, Object> sinkCall);

    private final void persistDocument(SinkCall<Void, Object> sinkCall, TupleEntry entry) {
      DBObject dbObject = convert(entry);
      try {
        checkResult(dbCollection.save(dbObject, WriteConcern.ACKNOWLEDGED));
      } catch (MongoException e) {
        log.warn("{} caught while trying to save {}",
            MongoException.class.getSimpleName(), dbObject);
        Throwables.propagate(e);
      }

      if (callback != null) {
        callback.onSaved(dbObject, dbCollection);
      }
      // This is required when the id is automatically generated.
      // TODO: verify that the above is true
      dbObject.put(MONGO_INTERNAL_ID, null);

      persistHook(sinkCall, entry);
    }

    protected abstract DBObject convert(TupleEntry entry);

    protected abstract void persistHook(SinkCall<Void, Object> sinkCall, TupleEntry entry);

    @Override
    public void sinkCleanup(FlowProcess<Object> flowProcess, SinkCall<Void, Object> sinkCall) throws IOException {
      super.sinkCleanup(flowProcess, sinkCall);
      if (callback != null) {
        callback.onComplete(dbCollection);
      }
    }

    @Override
    public void sourceConfInit(FlowProcess<Object> flowProcess, Tap<Object, Void, Object> tap, Object conf) {
    }

    @Override
    public void sinkConfInit(FlowProcess<Object> flowProcess, Tap<Object, Void, Object> tap, Object conf) {
    }

    @SneakyThrows
    protected final void checkResult(WriteResult insert) {
      if (insert.getError() != null) {
        throw new IOException(insert.getError());
      }
    }

    protected final DBCollection getDbCollection(SinkCall<Void, Object> sinkCall, String databaseName,
        String collectionName) {
      return mongoClient(sinkCall)
          .getDB(databaseName)
          .getCollection(collectionName);
    }

    /**
     * TODO: DCC-1276 (does upsert update if nothing changed?)
     */
    protected final WriteResult upsert(
        @NonNull final DBCollection collection,
        @NonNull final DBObject dbObject) {
      boolean upsert = true;
      boolean multi = true;
      return collection.update(
          dbObject,
          dbObject,
          upsert,
          !multi,
          WriteConcern.ACKNOWLEDGED);
    }

    /**
     * Created in {@link MongoDbTap#mongoClient()}.
     */
    private final Mongo mongoClient(SinkCall<Void, Object> sinkCall) {
      return (Mongo) sinkCall.getOutput();
    }

  }

}
