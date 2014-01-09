/*
 * Copyright (c): TODO - edu.umd.cloud9.mapred
 */
package org.icgc.dcc.hadoop.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.mapred.InputSplit;

public class NullInputSplit implements InputSplit {

  @Override
  public long getLength() {
    return 0;
  }

  @Override
  public String[] getLocations() {
    String[] locs = {};
    return locs;
  }

  @Override
  public void readFields(DataInput in) throws IOException {
  }

  @Override
  public void write(DataOutput out) throws IOException {
  }
}