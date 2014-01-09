/*
 * Copyright (c): TODO - edu.umd.cloud9.mapred;
 */
package org.icgc.dcc.hadoop.io;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.Progressable;

public class NullOutputFormat implements OutputFormat<NullWritable, NullWritable> {

  @Override
  public void checkOutputSpecs(FileSystem ignored, JobConf job) {

  }

  @Override
  public RecordWriter<NullWritable, NullWritable> getRecordWriter(FileSystem ignored,
      JobConf job, String name, Progressable progress) {
    return new NullRecordWriter();
  }

  public static class NullRecordWriter implements RecordWriter<NullWritable, NullWritable> {

    @Override
    public void close(Reporter reporter) {

    }

    @Override
    public void write(NullWritable key, NullWritable value) {

    }
  }
}