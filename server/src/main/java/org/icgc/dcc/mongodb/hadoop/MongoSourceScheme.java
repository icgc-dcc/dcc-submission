package org.icgc.dcc.mongodb.hadoop;

import java.io.IOException;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.bson.BasicBSONObject;

import cascading.flow.FlowProcess;
import cascading.scheme.Scheme;
import cascading.scheme.SinkCall;
import cascading.scheme.SourceCall;
import cascading.tap.Tap;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

import com.mongodb.hadoop.MongoConfig;
import com.mongodb.hadoop.io.BSONWritable;

public final class MongoSourceScheme extends
    Scheme<JobConf, RecordReader<BSONWritable, BSONWritable>, Void, BSONWritable[], BSONWritable[]> {

  private String keyField;

  public MongoSourceScheme() {
    super(new Fields("_id", "object"));
  }

  @Override
  public void sourceConfInit(FlowProcess<JobConf> flowProcess,
      Tap<JobConf, RecordReader<BSONWritable, BSONWritable>, Void> tap, JobConf conf) {
    MongoConfig mongoConfig = new MongoConfig(conf);
    this.keyField = mongoConfig.getInputKey();
  }

  @Override
  public void sinkConfInit(FlowProcess<JobConf> flowProcess,
      Tap<JobConf, RecordReader<BSONWritable, BSONWritable>, Void> tap, JobConf conf) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void sourcePrepare(FlowProcess<JobConf> flowProcess,
      SourceCall<BSONWritable[], RecordReader<BSONWritable, BSONWritable>> sourceCall) {
    sourceCall.setContext(new BSONWritable[2]);

    sourceCall.getContext()[0] = sourceCall.getInput().createKey();
    sourceCall.getContext()[1] = sourceCall.getInput().createValue();
  }

  @Override
  public boolean source(FlowProcess<JobConf> flowProcess,
      SourceCall<BSONWritable[], RecordReader<BSONWritable, BSONWritable>> sourceCall) throws IOException {

    if(sourceCall.getInput().next(sourceCall.getContext()[0], sourceCall.getContext()[1]) == false) {
      return false;
    }

    Tuple tuple = sourceCall.getIncomingEntry().getTuple();

    BSONWritable[] context = sourceCall.getContext();

    tuple.set(0, context[0].get(keyField).toString());
    // Make a copy
    BasicBSONObject object = new BasicBSONObject();
    object.putAll(context[1]);
    tuple.set(1, object);

    return true;
  }

  @Override
  public void sourceCleanup(FlowProcess<JobConf> flowProcess,
      SourceCall<BSONWritable[], RecordReader<BSONWritable, BSONWritable>> sourceCall) throws IOException {
    sourceCall.setContext(null);
  }

  @Override
  public void sink(FlowProcess<JobConf> flowProcess, SinkCall<BSONWritable[], Void> sinkCall) throws IOException {
    throw new UnsupportedOperationException();
  }
}
