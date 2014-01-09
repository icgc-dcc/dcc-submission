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
package org.icgc.dcc.hadoop.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

public class NullInputFormat implements InputFormat<NullWritable, NullWritable> {

  @Override
  public RecordReader<NullWritable, NullWritable> getRecordReader(InputSplit split, JobConf job, Reporter reporter) {
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

  public static class NullRecordReader implements RecordReader<NullWritable, NullWritable> {

    private boolean returnRecord = true;

    @Override
    public boolean next(NullWritable key, NullWritable value) throws IOException {
      if (returnRecord) {
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