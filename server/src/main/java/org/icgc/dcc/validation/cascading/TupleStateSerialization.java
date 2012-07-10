/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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

package org.icgc.dcc.validation.cascading;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
      ObjectInputStream output = new ObjectInputStream(in);
      TupleState tupleState = null;
      try {
        tupleState = (TupleState) output.readObject();
      } catch(ClassNotFoundException e) {
        e.printStackTrace();
      }
      return tupleState;
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
      ObjectOutputStream outputStream = new ObjectOutputStream(out);
      outputStream.writeObject(t);
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

      return 0;
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
    return new TupleStateSerializer();
  }

  @Override
  public Deserializer<TupleState> getDeserializer(Class<TupleState> c) {
    return new TupleStateDeserializer();
  }

  @Override
  public Comparator<TupleState> getComparator(Class<TupleState> arg0) {
    return new TupleStateComparator();
  }

}