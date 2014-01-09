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

import java.io.Closeable;
import java.io.IOException;

import lombok.NonNull;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.JobConf;
import org.icgc.dcc.hadoop.io.NullInputFormat;

import cascading.flow.FlowProcess;
import cascading.scheme.NullScheme;
import cascading.scheme.Scheme;
import cascading.tap.SourceTap;
import cascading.tuple.TupleEntryIterator;
import cascading.tuple.TupleEntrySchemeIterator;

class EmptySourceTap<T> extends SourceTap<T, Closeable> {

  @NonNull
  private final String identifier;

  @SuppressWarnings({ "rawtypes", "unchecked" })
  public EmptySourceTap(String identifier) {
    this(new NullScheme(), identifier);
  }

  public EmptySourceTap(Scheme<T, Closeable, ?, ?, ?> scheme, String identifier) {
    super(scheme);
    this.identifier = "empty://source." + identifier;
  }

  @Override
  public void sourceConfInit(FlowProcess<T> flowProcess, T t) {
    super.sourceConfInit(flowProcess, t);

    // FIXME
    // https://groups.google.com/d/topic/cascading-user/ngLidsZQjIU/discussion
    if (t instanceof JobConf) {
      val jobConf = (JobConf) t;
      FileInputFormat.setInputPaths(jobConf, new Path("/tmp/ignoreme"));
      jobConf.setInputFormat(NullInputFormat.class);
    }
  }

  @Override
  public String getIdentifier() {
    return identifier;
  }

  @Override
  public TupleEntryIterator openForRead(FlowProcess<T> flowProcess, Closeable input)
      throws IOException {
    return new TupleEntrySchemeIterator<T, Closeable>(flowProcess, getScheme(), new Closeable() {

      @Override
      public void close() throws IOException {
      }

    });
  }

  @Override
  public boolean resourceExists(T conf) throws IOException {
    return true;
  }

  @Override
  public long getModifiedTime(T conf) throws IOException {
    return System.currentTimeMillis();
  }
}
