/*
 * Copyright (c): TODO: edu.umd.cloud9.mapred
 */
package org.icgc.dcc.hadoop.io;

import java.io.IOException;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

// Note, there was a thread on the Hadoop users list on exactly this issue. 
// 5/8/2009, "How to write a map() method that needs no input?"
public class NullInputFormat implements InputFormat<NullWritable, NullWritable> {

  @Override
  public RecordReader<NullWritable, NullWritable> getRecordReader(InputSplit split, JobConf job,
      Reporter reporter) {
    return new NullRecordReader();
  }

  @Override
  public InputSplit[] getSplits(JobConf job, int numSplits) {
    InputSplit[] splits = new InputSplit[numSplits];

    for (int i = 0; i < numSplits; i++)
      splits[i] = new NullInputSplit();

    return splits;

  }

  public void validateInput(JobConf job) {
  }

  public static class NullRecordReader implements RecordReader<NullWritable, NullWritable> {

    private boolean returnRecord = true;

    public NullRecordReader() {

    }

    @Override
    public boolean next(NullWritable key, NullWritable value) throws IOException {
      if (returnRecord == true) {
        returnRecord = false;
        return true;
      }

      return returnRecord;
    }

    @Override
    public NullWritable createKey() {
      return NullWritable.get();
    }

    @Override
    public NullWritable createValue() {
      return NullWritable.get();
    }

    @Override
    public long getPos() throws IOException {
      return 0;
    }

    @Override
    public float getProgress() throws IOException {
      return 0.0f;
    }

    @Override
    public void close() {
    }
  }

}