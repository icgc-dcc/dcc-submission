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
package org.icgc.dcc.submission.server.service;

import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.icgc.dcc.submission.dictionary.model.DictionaryState.OPENED;

import java.util.List;

import org.icgc.dcc.common.core.json.Jackson;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Term;
import org.icgc.dcc.submission.dictionary.visitor.DictionaryCloneVisitor;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.server.dictionary.DictionaryServiceException;
import org.icgc.dcc.submission.server.repository.CodeListRepository;
import org.icgc.dcc.submission.server.repository.DictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMultimap;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DictionaryService {

  /**
   * Dependencies.
   */
  @NonNull
  private final ReleaseService releaseService;
  @NonNull
  private final DictionaryRepository dictionaryRepository;
  @NonNull
  private final CodeListRepository codeListRepository;

  public List<Dictionary> getDictionaries() {
    return dictionaryRepository.findDictionaries();
  }

  public Dictionary getCurrentDictionary() {
    val release = releaseService.getNextRelease();
    return getCurrentDictionary(release);
  }

  public Dictionary getCurrentDictionary(@NonNull Release openRelease) {
    val version = openRelease.getDictionaryVersion();
    return getDictionaryByVersion(version);
  }

  public ArrayNode getVersions() {
    val mapper = Jackson.DEFAULT;
    val dicts = dictionaryRepository.getVersions();
    val releases = releaseService.getReleases();

    val builder = ImmutableMultimap.<String, Release> builder();
    for (val release : releases) {
      builder.put(release.getDictionaryVersion(), release);
    }
    val releaseMap = builder.build();

    val response = mapper.createArrayNode();
    for (val dict : dicts) {
      val dictJson = mapper.createObjectNode();
      dictJson.put("created", dict.getCreated().getTime());
      dictJson.put("lastUpdated", dict.getCreated().getTime());
      dictJson.put("version", dict.getVersion());
      dictJson.put("state", dict.getState().toString());

      val dictReleases = releaseMap.get(dict.getVersion());
      val releaseList = mapper.createArrayNode();
      for (val release : dictReleases) {
        releaseList.add(release.getName());
      }
      dictJson.set("releases", releaseList);

      response.add(dictJson);
    }

    return response;
  }

  public Dictionary getDictionaryByVersion(@NonNull String version) {
    return dictionaryRepository.findDictionaryByVersion(version);
  }

  /**
   * Updates an OPENED dictionary.
   * <p>
   * Must reset submissions IF the nextRelease uses that dictionary (TODO: point to spec).
   * <p>
   * Contains critical blocks for admin concurrency (DCC-?).
   */
  public void updateDictionary(@NonNull Dictionary dictionary, boolean reset) {
    // TODO: Add check dictionary is OPENED here (instead of within resource)
    val version = dictionary.getVersion();
    val count = dictionaryRepository.countDictionariesByVersion(version);
    if (count != 1) {
      throw new DictionaryServiceException("Cannot update a non-existent dictionary: " + dictionary.getVersion());
    }

    log.info("Updating dictionary version {}...", dictionary.getVersion());
    dictionaryRepository.updateDictionary(dictionary);

    // Reset submissions if applicable
    if (!isReleaseDictionary(version)) {
      log.info(
          "Current release not associated with updated dictionary version {}. No need to reset release submissions.",
          dictionary.getVersion());
      return;
    }

    if (!reset) {
      log.warn(
          "Skipping reset of release submissions has been requested. Previously signed-off or valid submissions may now be invalid!");
      return;
    }

    log.info("Resetting release submissions...");
    releaseService.resetSubmissions();
  }

  /**
   * Updates the supplied {@code dictionary} without resetting a release.
   * 
   * @param dictionary
   */
  public void updateDictionary(@NonNull Dictionary dictionary) {
    updateDictionary(dictionary, false);
  }

  public Dictionary cloneDictionary(@NonNull String oldVersion, @NonNull String newVersion) {
    if (oldVersion.equals(newVersion)) {
      throw new DictionaryServiceException("Cannot clone a dictionary using the same version: " + newVersion);
    }

    val oldDictionary = this.getDictionaryByVersion(oldVersion);
    if (oldDictionary == null) {
      throw new DictionaryServiceException("Cannot clone an non-existent dictionary: " + oldVersion);
    }
    if (getDictionaryByVersion(newVersion) != null) {
      throw new DictionaryServiceException("Cannot clone to an already existing dictionary: " + newVersion);
    }

    val dictionaryCloneVisitor = new DictionaryCloneVisitor();
    oldDictionary.accept(dictionaryCloneVisitor);

    val newDictionary = dictionaryCloneVisitor.getDictionaryClone();
    newDictionary.setVersion(newVersion);

    addDictionary(newDictionary);

    return newDictionary;
  }

  /**
   * Add a new dictionary to the database after having ensured it provides enough information.
   * <p>
   * Do not reset submission states since by design no OPENED release points to that new dictionary yet.
   */
  public void addDictionary(@NonNull Dictionary dictionary) {
    val version = dictionary.getVersion();
    if (version == null) {
      throw new DictionaryServiceException("New dictionary must specify a valid version");
    }

    if (getDictionaryByVersion(version) != null) {
      throw new DictionaryServiceException("Cannot add an existing dictionary: " + version);
    }

    if (OPENED != dictionary.getState()) {
      throw new DictionaryServiceException(format("New dictionary must be in OPENED state: %s instead",
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
  public void addCodeList(@NonNull List<CodeList> codeLists) {
    log.info("Saving code lists: {}", codeLists);

    for (val codeList : codeLists) {
      checkArgument(codeList != null);
      String name = codeList.getName();
      if (getCodeList(name).isPresent()) {
        throw new DictionaryServiceException("Aborting codelist addition: cannot add existing codeList: " + name);
      }
    }

    codeListRepository.saveCodeLists(codeLists);
  }

  public Optional<CodeList> getCodeList(@NonNull String name) {
    log.debug("Retrieving code list: {}", name);
    val codeList = codeListRepository.findCodeListByName(name);
    return fromNullable(codeList);
  }

  /**
   * This does not need to reset submission states as long as only inconsequential properties are updated.
   */
  public void updateCodeList(@NonNull CodeList newCodeList) {
    val name = newCodeList.getName();

    val optional = getCodeList(name);
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
  public void addCodeListTerm(@NonNull String codeListName, @NonNull Term term) {
    val optional = getCodeList(codeListName);
    if (optional.isPresent() == false) {
      throw new DictionaryServiceException("Cannot add term to non-existant codel list: " + codeListName);
    }

    val codeList = optional.get();
    if (codeList.containsTerm(term)) {
      throw new DictionaryServiceException("Cannot add an existing term: " + term.getCode());
    }

    codeList.addTerm(term);
    codeListRepository.addCodeListTerm(codeListName, term);

    // Reset INVALID submissions if applicable
    val release = releaseService.getNextRelease();
    val currentDictionary = getCurrentDictionary(release);
    if (currentDictionary.usesCodeList(codeListName)) {
      log.info("Resetting submission due to active dictionary code list term addition...");
      releaseService.resetInvalidSubmissions();
    } else {
      log.info("No need to reset submissions due to active dictionary code list term addition...");
    }
  }

  private boolean isReleaseDictionary(final java.lang.String version) {
    val release = releaseService.getNextRelease();
    val versionMatch = version.equals(release.getDictionaryVersion());

    return versionMatch;
  }

}
