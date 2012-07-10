package org.icgc.dcc.validation.cascading;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Comparator;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.io.serializer.Deserializer;
import org.apache.hadoop.io.serializer.Serialization;
import org.apache.hadoop.io.serializer.Serializer;

import cascading.CascadingException;
import cascading.tuple.Comparison;
import cascading.tuple.StreamComparator;
import cascading.tuple.hadoop.SerializationToken;
import cascading.tuple.hadoop.io.BufferedInputStream;

@SerializationToken(tokens = { 222 }, classNames = { "org.icgc.dcc.validation.cascading.TupleState" })
public class TupleStateSerialization extends Configured implements Comparison<TupleState>, Serialization<TupleState> {

  public static class TupleStateDeserializer implements Deserializer<TupleState> {
    private DataInputStream in;

    @Override
    public void open(InputStream in) throws IOException {
      if(in instanceof DataInputStream) {
        this.in = (DataInputStream) in;
      } else {
        this.in = new DataInputStream(in);
      }
    }

    @Override
    public TupleState deserialize(TupleState t) throws IOException {
      return new TupleState();
    }

    @Override
    public void close() throws IOException {
      in.close();
    }

  }

  public static class TupleStateSerializer implements Serializer<TupleState> {
    private DataOutputStream out;

    @Override
    public void open(OutputStream out) throws IOException {
      if(out instanceof DataOutputStream) {
        this.out = (DataOutputStream) out;
      } else {
        this.out = new DataOutputStream(out);
      }
    }

    @Override
    public void serialize(TupleState t) throws IOException {
      WritableUtils.writeString(out, t.toString());
    }

    @Override
    public void close() throws IOException {
      out.close();
    }

  }

  public static class TupleStateComparator implements StreamComparator<BufferedInputStream>, Comparator<TupleState>,
      Serializable {

    @Override
    public int compare(TupleState lhs, TupleState rhs) {
      if(lhs == null) {
        return -1;
      }

      if(rhs == null) {
        return 1;
      }

      return lhs.compareTo(rhs);
    }

    @Override
    public int compare(BufferedInputStream lhsStream, BufferedInputStream rhsStream) {
      try {
        if(lhsStream == null && rhsStream == null) {
          return 0;
        }

        if(lhsStream == null) {
          return -1;
        }

        if(rhsStream == null) {
          return 1;
        }

        String lhsString = WritableUtils.readString(new DataInputStream(lhsStream));
        String rhsString = WritableUtils.readString(new DataInputStream(rhsStream));

        return lhsString.compareTo(rhsString);
      } catch(IOException exception) {
        throw new CascadingException(exception);
      }
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