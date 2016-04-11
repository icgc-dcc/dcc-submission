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

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.transform;

import java.util.List;

import org.icgc.dcc.common.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.dictionary.model.Dictionary;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import lombok.RequiredArgsConstructor;

/**
 * WIP (use {@link KVHardcodedDictionary} until finished).
 */
@RequiredArgsConstructor
public class KVDynamicDictionary implements KVDictionary {

  private final Dictionary dictionary;

  @Override
  public Iterable<KVExperimentalDataType> getExperimentalDataTypes() {
    return copyOf(transform(dictionary.getFeatureTypes(),
        new Function<FeatureType, KVExperimentalDataType>() {

          @Override
          public KVExperimentalDataType apply(FeatureType featureType) {
            return KVExperimentalDataType.from(featureType);
          }

        }));
  }

  @Override
  public List<KVFileType> getExperimentalFileTypes(KVExperimentalDataType dataType) {
    return copyOf(transform(
        dictionary.getFileTypesReferencedBranch(dataType.getFeatureType()),
        new Function<FileType, KVFileType>() {

          @Override
          public KVFileType apply(FileType fileType) {
            return KVFileType.from(fileType);
          }

        }));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.icgc.dcc.submission.validation.key.core.KVDictionary#hasOutgoingSurjectiveRelation(org.icgc.dcc.submission.
   * validation.key.core.KVFileType)
   */
  @Override
  public boolean hasOutgoingSurjectiveRelation(KVFileType fileType) {
    // TODO Auto-generated method stub
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.icgc.dcc.submission.validation.key.core.KVDictionary#getKeysIndices(org.icgc.dcc.submission.validation.key.
   * core.KVFileType)
   */
  @Override
  public KVFileTypeKeysIndices getKeysIndices(KVFileType fileType) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.icgc.dcc.submission.validation.key.core.KVDictionary#getOptionalReferencedFileType1(org.icgc.dcc.submission
   * .validation.key.core.KVFileType)
   */
  @Override
  public Optional<KVFileType> getOptionalReferencedFileType1(KVFileType fileType) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.icgc.dcc.submission.validation.key.core.KVDictionary#getOptionalReferencedFileType2(org.icgc.dcc.submission
   * .validation.key.core.KVFileType)
   */
  @Override
  public Optional<KVFileType> getOptionalReferencedFileType2(KVFileType fileType) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.icgc.dcc.submission.validation.key.core.KVDictionary#getErrorFieldNames(org.icgc.dcc.submission.validation.
   * key.core.KVFileType, org.icgc.dcc.submission.validation.key.core.KVErrorType)
   */
  @Override
  public List<String> getErrorFieldNames(KVFileType fileType, KVErrorType errorType) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.icgc.dcc.submission.validation.key.core.KVDictionary#getPrimaryKeyNames(org.icgc.dcc.submission.validation.
   * key.core.KVFileType)
   */
  @Override
  public List<String> getPrimaryKeyNames(KVFileType fileType) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.icgc.dcc.submission.validation.key.core.KVDictionary#getSurjectionForeignKeyNames(org.icgc.dcc.submission.
   * validation.key.core.KVFileType)
   */
  @Override
  public List<String> getSurjectionForeignKeyNames(KVFileType fileType) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.icgc.dcc.submission.validation.key.core.KVDictionary#getReferencingFileType(org.icgc.dcc.submission.validation
   * .key.core.KVFileType)
   */
  @Override
  public KVFileType getReferencingFileType(KVFileType fileType) {
    // TODO Auto-generated method stub
    return null;
  }

}
