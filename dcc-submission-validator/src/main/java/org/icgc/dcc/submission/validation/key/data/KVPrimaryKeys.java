/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static java.lang.String.format;
import static org.icgc.dcc.core.util.FormatUtils.formatCount;
import static org.icgc.dcc.submission.validation.key.core.KVSubmissionProcessor.ROW_CHECKS_ENABLED;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.val;

import com.google.common.collect.Sets;

/**
 * Keeps track of primary keys for each file.
 * <p>
 * There's trade off here, we accept to potentially store the same PK under different files (if there's a uniqueness
 * violation). It's a bit wasteful but keeps the code complexity lower in {@link KVFileProcessor}.
 */
@RequiredArgsConstructor
public final class KVPrimaryKeys {

  private final Map<String, Set<KVKey>> pks = newLinkedHashMap();

  public List<String> getFilePaths() {
    return newArrayList(pks.keySet());
  }

  public Iterator<KVKey> getPrimaryKeys(String fileName) {
    return pks.get(fileName).iterator();
  }

  public boolean containsPk(KVKey pk) {
    for (val filePks : pks.values()) {
      if (filePks.contains(pk)) {
        return true;
      }
    }
    return false;
  }

  public void updatePks(String fileName, KVRow row) {
    if (ROW_CHECKS_ENABLED) {
      checkState(row.hasPk(), "Expected to have a PK: '%s' ('%s')", row, fileName);
    }

    if (!pks.containsKey(fileName)) {
      pks.put(fileName, Sets.<KVKey> newHashSet());
    }

    pks.get(fileName).add(row.getPk());
  }

  public long getSize() {
    long size = 0;
    for (val filePks : pks.values()) {
      size += filePks.size();
    }

    return size;
  }

  @Override
  public String toString() {
    return format("KVPrimaryKeys(pks=%s, size=%s)", formatCount(pks.size()), formatCount(getSize()));
  }

}
