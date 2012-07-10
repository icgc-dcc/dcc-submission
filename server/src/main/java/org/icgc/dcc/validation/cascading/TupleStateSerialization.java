package org.icgc.dcc.validation.cascading;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Comparator;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.serializer.Deserializer;
import org.apache.hadoop.io.serializer.Serialization;
import org.apache.hadoop.io.serializer.Serializer;

import cascading.tuple.Comparison;
import cascading.tuple.StreamComparator;
import cascading.tuple.hadoop.SerializationToken;
import cascading.tuple.hadoop.io.BufferedInputStream;

@SerializationToken(tokens = { 222 }, classNames = { "org.icgc.dcc.validation.cascading.TupleState" })
public class TupleStateSerialization extends Configured implements Comparison<TupleState>, Serialization<TupleState> {

  public static class TupleStateDeserializer implements Deserializer<TupleState> {

    @Override
    public void open(InputStream in) throws IOException {
      // TODO Auto-generated method stub

    }

    @Override
    public TupleState deserialize(TupleState t) throws IOException {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void close() throws IOException {
      // TODO Auto-generated method stub

    }

  }

  public static class TupleStateSerializer implements Serializer<TupleState> {

    @Override
    public void open(OutputStream out) throws IOException {
      // TODO Auto-generated method stub

    }

    @Override
    public void serialize(TupleState t) throws IOException {
      // TODO Auto-generated method stub

    }

    @Override
    public void close() throws IOException {
      // TODO Auto-generated method stub

    }

  }

  public static class TupleStateComparator implements StreamComparator<BufferedInputStream>, Comparator<TupleState>,
      Serializable {

    @Override
    public int compare(TupleState arg0, TupleState arg1) {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public int compare(BufferedInputStream arg0, BufferedInputStream arg1) {
      // TODO Auto-generated method stub
      return 0;
    }

  }

  @Override
  public boolean accept(Class<?> c) {
    return TupleState.class.isAssignableFrom(c);
  }

  @Override
  public Serializer<TupleState> getSerializer(Class<TupleState> c) {
    // TODO Auto-generated method stub
    return new TupleStateSerializer();
  }

  @Override
  public Deserializer<TupleState> getDeserializer(Class<TupleState> c) {
    // TODO Auto-generated method stub
    return new TupleStateDeserializer();
  }

  @Override
  public Comparator<TupleState> getComparator(Class<TupleState> arg0) {
    // TODO Auto-generated method stub
    return new TupleStateComparator();
  }

}
