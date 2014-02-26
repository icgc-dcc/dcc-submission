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
package org.icgc.dcc.submission.validation.cascading;

import java.io.IOException;

import lombok.NonNull;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.icgc.dcc.hadoop.io.NullOutputFormat;

import cascading.flow.FlowProcess;
import cascading.scheme.NullScheme;
import cascading.scheme.Scheme;
import cascading.tap.SinkMode;
import cascading.tap.SinkTap;
import cascading.tuple.TupleEntryCollector;
import cascading.tuple.TupleEntrySchemeCollector;

class EmptySinkTap<T> extends SinkTap<T, Object> {

  @NonNull
  private final String identifier;

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public EmptySinkTap(String identifier) {
    this(new NullScheme(), identifier);
  }

  public EmptySinkTap(Scheme<T, ?, Object, ?, ?> scheme, String identifier) {
    super(scheme, SinkMode.UPDATE);
    this.identifier = "empty://sink." + identifier;
  }

  @Override
  public void sinkConfInit(FlowProcess<T> flowProcess, T t) {
    super.sinkConfInit(flowProcess, t);

    // FIXME
    // https://groups.google.com/d/topic/cascading-user/ngLidsZQjIU/discussion
    if (t instanceof JobConf) {
      val jobConf = (JobConf) t;
      FileOutputFormat.setOutputPath(jobConf, new Path("/tmp/ignoreme"));
      jobConf.setOutputFormat(NullOutputFormat.class);
    }
  }

  @Override
  public String getIdentifier() {
    return identifier;
  }

  @Override
  public TupleEntryCollector openForWrite(FlowProcess<T> flowProcess, Object output) throws IOException {
    return new TupleEntrySchemeCollector<T, Object>(flowProcess, getScheme(), output);
  }

  @Override
  public boolean createResource(T conf) throws IOException {
    return true;
  }

  @Override
  public boolean deleteResource(T conf) throws IOException {
    return false;
  }

  @Override
  public boolean resourceExists(T conf) throws IOException {
    return true;
  }

  @Override
  public long getModifiedTime(T conf) throws IOException {
    return 0;
  }

}
