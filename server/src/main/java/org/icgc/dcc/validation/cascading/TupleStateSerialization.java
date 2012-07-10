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

import java.util.Comparator;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.serializer.Deserializer;
import org.apache.hadoop.io.serializer.Serialization;
import org.apache.hadoop.io.serializer.Serializer;

import cascading.tuple.Comparison;
import cascading.tuple.hadoop.SerializationToken;

/**
 * 
 */
@SerializationToken(tokens = { 222 }, classNames = { "org.icgc.dcc.validation.cascading.TupleState" })
public class TupleStateSerialization extends Configured implements Comparison<TupleState>, Serialization<TupleState> {

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.hadoop.io.serializer.Serialization#accept(java.lang.Class)
   */
  @Override
  public boolean accept(Class<?> c) {
    // TODO Auto-generated method stub
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.hadoop.io.serializer.Serialization#getSerializer(java.lang.Class)
   */
  @Override
  public Serializer<TupleState> getSerializer(Class<TupleState> c) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.hadoop.io.serializer.Serialization#getDeserializer(java.lang.Class)
   */
  @Override
  public Deserializer<TupleState> getDeserializer(Class<TupleState> c) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see cascading.tuple.Comparison#getComparator(java.lang.Class)
   */
  @Override
  public Comparator<TupleState> getComparator(Class<TupleState> arg0) {
    // TODO Auto-generated method stub
    return null;
  }

}
