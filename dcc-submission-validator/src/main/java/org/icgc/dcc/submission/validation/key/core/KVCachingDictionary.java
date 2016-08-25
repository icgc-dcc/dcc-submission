/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.key.core;

import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableMap;
import static org.icgc.dcc.common.core.util.stream.Streams.stream;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.NonNull;

import com.google.common.base.Optional;

public class KVCachingDictionary implements KVDictionary {

  /**
   * Dependencies.
   */
  private final KVDictionary delegate;

  /**
   * State.
   */
  private final Map<KVFileType, Boolean> hasChildrenCache;
  private final Map<KVFileType, Collection<KVFileType>> surjectiveTypesCache;
  private final Map<KVFileType, List<String>> primaryKeyNamesCache;
  private final Map<KVFileType, KVFileTypeKeysIndices> keyIndicesCache;

  public KVCachingDictionary(@NonNull KVDictionary delegate) {
    this.delegate = delegate;

    // Cache initializations
    this.hasChildrenCache = stream(KVFileType.values())
        .collect(toImmutableMap(fileType -> fileType, fileType -> delegate.hasChildren(fileType)));
    this.surjectiveTypesCache = stream(KVFileType.values())
        .collect(toImmutableMap(fileType -> fileType, fileType -> delegate.getSurjectiveReferencedTypes(fileType)));
    this.primaryKeyNamesCache = stream(KVFileType.values())
        .collect(toImmutableMap(fileType -> fileType, fileType -> delegate.getPrimaryKeyNames(fileType)));
    this.keyIndicesCache = stream(KVFileType.values())
        .collect(toImmutableMap(fileType -> fileType, fileType -> delegate.getKeysIndices(fileType)));
  }

  @Override
  public KVFileTypeKeysIndices getKeysIndices(KVFileType fileType) {
    return keyIndicesCache.get(fileType);
  }

  @Override
  public List<String> getErrorFieldNames(KVFileType fileType, KVErrorType errorType,
      Optional<KVFileType> referencedFileType) {
    return delegate.getErrorFieldNames(fileType, errorType, referencedFileType);
  }

  @Override
  public List<String> getPrimaryKeyNames(KVFileType fileType) {
    return primaryKeyNamesCache.get(fileType);
  }

  @Override
  public Iterable<KVFileType> getTopologicallyOrderedFileTypes() {
    return delegate.getTopologicallyOrderedFileTypes();
  }

  @Override
  public Collection<KVFileType> getParents(KVFileType fileType) {
    return delegate.getParents(fileType);
  }

  @Override
  public boolean hasChildren(KVFileType fileType) {
    return hasChildrenCache.get(fileType);
  }

  @Override
  public Collection<KVFileType> getSurjectiveReferencedTypes(KVFileType fileType) {
    return surjectiveTypesCache.get(fileType);
  }

}
