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

package org.icgc.dcc.submission.repository;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.icgc.dcc.submission.dictionary.model.DictionaryState.CLOSED;
import static org.icgc.dcc.submission.dictionary.model.QDictionary.dictionary;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.QDictionary;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

public class DictionaryRepository extends AbstractRepository<Dictionary, QDictionary> {

  @Inject
  public DictionaryRepository(@NonNull Morphia morphia, @NonNull Datastore datastore) {
    super(morphia, datastore, dictionary);
  }

  public long countDictionariesByVersion(@NonNull String version) {
    return count(entity.version.eq(version));
  }

  public List<Dictionary> findDictionaries() {
    return list();
  }

  public Dictionary findDictionaryByVersion(@NonNull String version) {
    return uniqueResult(entity.version.eq(version));
  }

  public void saveDictionary(@NonNull Dictionary dictionary) {
    save(dictionary);
  }

  public void updateDictionary(@NonNull Dictionary dictionary) {
    updateFirst(
        createQuery()
            .filter("version", dictionary.getVersion()),
        dictionary, false);
  }

  public void closeDictionary(@NonNull String version) {
    findAndModify(
        createQuery()
            .filter("version", version),
        createUpdateOperations()
            .set("state", CLOSED));
  }

  /**
   * Return a map of file pattern to {@link FileType} for the dictionary version provided, which is expected to exist.
   */
  public Map<String, FileType> getFilePatternToTypeMap(@NonNull String version) {
    val dictionary = checkNotNull(findDictionaryByVersion(version), "No dictionary with version '%s' found", version);

    val patternToType = new ImmutableMap.Builder<String, FileType>();
    for (val fileSchema : dictionary.getFiles()) {
      patternToType.put(fileSchema.getPattern(), fileSchema.getFileType());
    }
    return patternToType.build();
  }

}
