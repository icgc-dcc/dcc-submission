package org.icgc.dcc.mongodb.hadoop;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;

import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;

import cascading.flow.FlowProcess;
import cascading.tap.SourceTap;
import cascading.tap.hadoop.io.HadoopTupleEntrySchemeIterator;
import cascading.tuple.TupleEntryIterator;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import com.mongodb.hadoop.MongoConfig;
import com.mongodb.hadoop.io.BSONWritable;
import com.mongodb.hadoop.mapred.MongoInputFormat;

public final class MongoSourceTap extends SourceTap<JobConf, RecordReader<BSONWritable, BSONWritable>> {

  private final String collectionUri;

  private transient MongoURI uri;

  public MongoSourceTap(MongoSourceScheme mongoScheme, MongoURI collectionUri) {
    super(mongoScheme);
    checkState(collectionUri.getCollection() != null,
        "mongodb URI should refer to a collection: mongodb://hostname/db.collection");
    this.collectionUri = collectionUri.toString();
    this.uri = collectionUri;
  }

  @Override
  public String getIdentifier() {
    return collectionUri;
  }

  @Override
  public void sourceConfInit(FlowProcess<JobConf> flowProcess, JobConf conf) {
    // Required, but not used directly?
    FileInputFormat.addInputPaths(conf, getIdentifier());

    MongoConfig mongoConfig = new MongoConfig(conf);
    mongoConfig.setInputURI(collectionUri);

    conf.setInputFormat(MongoInputFormat.class);

    super.sourceConfInit(flowProcess, conf);
  }

  @Override
  public TupleEntryIterator openForRead(FlowProcess<JobConf> flowProcess,
      RecordReader<BSONWritable, BSONWritable> recordReader) throws IOException {
    return new HadoopTupleEntrySchemeIterator(flowProcess, this, recordReader);
  }

  @Override
  public boolean resourceExists(JobConf conf) throws IOException {
    Mongo mongo = mongoUri().connect();
    if(mongo.getDatabaseNames().contains(mongoUri().getDatabase())) {
      DB db = mongo.getDB(mongoUri().getDatabase());
      return db.getCollectionNames().contains(mongoUri().getCollection());
    }
    return false;
  }

  @Override
  public long getModifiedTime(JobConf conf) throws IOException {
    return System.currentTimeMillis();
  }

  private MongoURI mongoUri() {
    if(uri == null) {
      uri = new MongoURI(collectionUri);
    }
    return uri;
  }

}