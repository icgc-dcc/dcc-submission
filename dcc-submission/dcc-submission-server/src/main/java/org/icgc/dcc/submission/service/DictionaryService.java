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
package org.icgc.dcc.submission.service;

import static com.google.common.base.Preconditions.checkArgument;
import static org.icgc.dcc.submission.dictionary.model.DictionaryState.OPENED;

import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.dictionary.DictionaryServiceException;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Term;
import org.icgc.dcc.submission.dictionary.visitor.DictionaryCloneVisitor;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.repository.CodeListRepository;
import org.icgc.dcc.submission.repository.DictionaryRepository;

import com.google.common.base.Optional;
import com.google.inject.Inject;

@Slf4j
@RequiredArgsConstructor(onConstructor = @_(@Inject))
public class DictionaryService {

  private final ReleaseService releases;
  private final DictionaryRepository dictionaryRepository;
  private final CodeListRepository codeListRepository;

  public List<Dictionary> getDictionaries() {
    return dictionaryRepository.findDictionaries();
  }

  public Dictionary getCurrentDictionary() {
    Release openRelease = releases.getNextRelease();
    return getCurrentDictionary(openRelease);
  }

  public Dictionary getCurrentDictionary(Release openRelease) {
    String currentDictionaryVersion = openRelease.getDictionaryVersion();
    return getDictionaryByVersion(currentDictionaryVersion);
  }

  public Dictionary getDictionaryByVersion(String version) {
    return dictionaryRepository.findDictionaryByVersion(version);
  }

  /**
   * Updates an OPENED dictionary.
   * <p>
   * Must reset submissions IF the nextRelease uses that dictionary (TODO: point to spec).
   * <p>
   * Contains critical blocks for admin concurrency (DCC-?).
   */
  public void updateDictionary(Dictionary dictionary) {
    // TODO: add check dicitonary is OPENED here (instead of within resource)

    checkArgument(dictionary != null);
    long count = dictionaryRepository.countDictionariesByVersion(dictionary.getVersion());
    if (count != 1) {
      throw new DictionaryServiceException("cannot update a non-existent dictionary: " + dictionary.getVersion());
    }
    dictionaryRepository.updateDictionary(dictionary);

    // Reset submissions if applicable
    Release release = releases.getNextRelease();
    if (dictionary.getVersion().equals(release.getDictionaryVersion())) {
      releases.resetSubmissions(release.getName(), release.getProjectKeys());
    }
  }

  public Dictionary cloneDictionary(@NonNull String oldVersion, @NonNull String newVersion) {
    if (oldVersion.equals(newVersion)) {
      throw new DictionaryServiceException("cannot clone a dictionary using the same version: " + newVersion);
    }

    val oldDictionary = this.getDictionaryByVersion(oldVersion);
    if (oldDictionary == null) {
      throw new DictionaryServiceException("cannot clone an non-existent dictionary: " + oldVersion);
    }
    if (getDictionaryByVersion(newVersion) != null) {
      throw new DictionaryServiceException("cannot clone to an already existing dictionary: " + newVersion);
    }

    val dictionaryCloneVisitor = new DictionaryCloneVisitor();
    oldDictionary.accept(dictionaryCloneVisitor);

    val newDictionary = dictionaryCloneVisitor.getDictionaryClone();
    newDictionary.setVersion(newVersion);

    this.addDictionary(newDictionary);

    return newDictionary;
  }

  /**
   * Add a new dictionary to the database after having ensured it provides enough information.
   * <p>
   * Do not reset submission states since by design no OPENED release points to that new dictionary yet.
   */
  public void addDictionary(@NonNull Dictionary dictionary) {
    String version = dictionary.getVersion();
    if (version == null) {
      throw new DictionaryServiceException("New dictionary must specify a valid version");
    }

    if (this.getDictionaryByVersion(version) != null) {
      throw new DictionaryServiceException("cannot add an existing dictionary: " + version);
    }

    if (OPENED != dictionary.getState()) {
      throw new DictionaryServiceException(String.format("New dictionary must be in OPENED state: %s instead",
          dictionary.getState()));
    }

    dictionaryRepository.saveDictionary(dictionary);
  }

  public List<CodeList> getCodeLists() {
    return codeListRepository.findCodeLists();
  }

  /**
   * Add new codelists to the database.
   * <p>
   * Do not reset submission states since by design no dictionary points to those new codelists yet.
   */
  public void addCodeList(List<CodeList> codeLists) {
    log.info("Saving codelists {}", codeLists);

    for (CodeList codeList : codeLists) {
      checkArgument(codeList != null);
      String name = codeList.getName();
      if (getCodeList(name).isPresent()) {
        throw new DictionaryServiceException("Aborting codelist addition: cannot add existing codeList: " + name);
      }
    }

    codeListRepository.saveCodeLists(codeLists);
  }

  public Optional<CodeList> getCodeList(String name) {
    checkArgument(name != null);
    log.debug("retrieving codelist {}", name);
    CodeList codeList = codeListRepository.findCodeListByName(name);
    return codeList == null ? Optional.<CodeList> absent() : Optional.<CodeList> of(codeList);
  }

  /**
   * This does not need to reset submission states as long as only inconsequential properties are updated.
   */
  public void updateCodeList(CodeList newCodeList) {
    checkArgument(newCodeList != null);
    String name = newCodeList.getName();

    Optional<CodeList> optional = this.getCodeList(name);
    if (optional.isPresent() == false) {
      throw new DictionaryServiceException("Cannot perform update to non-existant codeList: " + name);
    }

    codeListRepository.updateCodeList(name, newCodeList);
  }

  /**
   * Adds a new term to codelist.
   * <p>
   * Must reset INVALID submissions IF the nextRelease uses a dictionary that uses the corresponding codelist (as the
   * change may render them VALID). (TODO: point to spec).
   */
  public void addCodeListTerm(String codeListName, Term term) {
    checkArgument(codeListName != null);
    checkArgument(term != null);

    Optional<CodeList> optional = this.getCodeList(codeListName);
    if (optional.isPresent() == false) {
      throw new DictionaryServiceException("cannot add term to non-existant codeList: " + codeListName);
    }
    CodeList codeList = optional.get();
    if (codeList.containsTerm(term)) {
      throw new DictionaryServiceException("cannot add an existing term: " + term.getCode());
    }
    codeList.addTerm(term);

    codeListRepository.addCodeListTerm(codeListName, term);

    // Reset INVALID submissions if applicable
    Release openRelease = releases.getNextRelease();
    Dictionary currentDictionary = getCurrentDictionary(openRelease);
    if (currentDictionary.usesCodeList(codeListName)) {
      log.info("Resetting submission due to active dictionary code list term addition...");
      releases.resetSubmissions(openRelease.getName(), openRelease.getInvalidProjectKeys());
    } else {
      log.info("No need to reset submissions due to active dictionary code list term addition...");
    }
  }

}
