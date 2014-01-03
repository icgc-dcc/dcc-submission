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
package org.icgc.dcc.submission.validation.key.data;

import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Maps.transformValues;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.icgc.dcc.submission.validation.key.KVFileDescription;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;

import com.google.common.base.Function;

/**
 * Represents the data dagest for the whole submission, as either existing or incremental data.
 */
public class KVSubmissionDataDigest {

  private final Map<KVFileType, KVFileDataDigest> data = newLinkedHashMap();

  // TODO: use delegate?
  public boolean contains(KVFileType fileType) {
    return data.containsKey(fileType);
  }

  public Set<Entry<KVFileType, KVFileDataDigest>> entrySet() {
    return data.entrySet();
  }

  public void put(KVFileType fileType, KVFileDataDigest fileData) {
    data.put(fileType, fileData);
  }

  public KVFileDataDigest get(KVFileType fileType) {
    return data.get(fileType);
  }

  public Map<KVFileType, KVFileDescription> getFileDescriptions() {
    return copyOf(transformValues(
        data,
        new Function<KVFileDataDigest, KVFileDescription>() {

          @Override
          public KVFileDescription apply(KVFileDataDigest datum) {
            return datum.getKvFileDescription();
          }
        }));
  }
}
