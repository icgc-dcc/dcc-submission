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
package org.icgc.dcc.dictionary;

import java.util.List;

import org.icgc.dcc.core.morphia.BaseMorphiaService;
import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.DictionaryState;
import org.icgc.dcc.dictionary.model.QCodeList;
import org.icgc.dcc.dictionary.model.QDictionary;
import org.icgc.dcc.dictionary.model.Term;
import org.icgc.dcc.dictionary.visitor.DictionaryCloneVisitor;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.Release;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Offers various CRUD operations pertaining to {@code Dictionary}
 */
public class DictionaryService extends BaseMorphiaService<Dictionary> {

  private static final Logger log = LoggerFactory.getLogger(DictionaryService.class);

  private final ReleaseService releases;

  @Inject
  public DictionaryService(Morphia morphia, Datastore datastore, ReleaseService releases) {
    super(morphia, datastore, QDictionary.dictionary);
    checkArgument(releases != null);
    this.releases = releases;
    registerModelClasses(Dictionary.class, CodeList.class);
  }

  public List<Dictionary> list() {
    return this.query().list();
  }

  public Dictionary getFromVersion(String version) {
    return this.where(QDictionary.dictionary.version.eq(version)).singleResult();
  }

  /**
   * Updates an OPENED dictionary.
   * <p>
   * Must reset submissions IF the nextRelease uses that dictionary (TODO: point to spec).
   * <p>
   * Contains critical blocks for admin concurrency (DCC-?).
   */
  public void update(Dictionary dictionary) {
    // TODO: add check dicitonary is OPENED here (instead of within resource)

    checkArgument(dictionary != null);
    Query<Dictionary> updateQuery = this.buildDictionaryVersionQuery(dictionary);
    if(updateQuery.countAll() != 1) {
      throw new DictionaryServiceException("cannot update a non-existent dictionary: " + dictionary.getVersion());
    }
    datastore().updateFirst(updateQuery, dictionary, false);

    // Reset submissions if applicable
    Release release = releases.getNextRelease().getRelease();
    if(dictionary.getVersion().equals(release.getDictionaryVersion())) {
      releases.resetSubmissions(release.getName(), release.getProjectKeys());
    }
  }

  public Dictionary clone(String oldVersion, String newVersion) {
    checkArgument(oldVersion != null);
    checkArgument(newVersion != null);
    if(oldVersion.equals(newVersion)) {
      throw new DictionaryServiceException("cannot clone a dictionary using the same version: " + newVersion);
    }
    Dictionary oldDictionary = this.getFromVersion(oldVersion);
    if(oldDictionary == null) {
      throw new DictionaryServiceException("cannot clone an non-existent dictionary: " + oldVersion);
    }
    if(getFromVersion(newVersion) != null) {
      throw new DictionaryServiceException("cannot clone to an already existing dictionary: " + newVersion);
    }

    DictionaryCloneVisitor dictionaryCloneVisitor = new DictionaryCloneVisitor();
    oldDictionary.accept(dictionaryCloneVisitor);

    Dictionary newDictionary = dictionaryCloneVisitor.getDictionaryClone();
    newDictionary.setVersion(newVersion);

    this.addDictionary(newDictionary);

    return newDictionary;
  }

  /**
   * Add a new dictionary to the database after having ensured it provides enough information.
   * <p>
   * Do not reset submission states since by design no OPENED release points to that new dictionary yet.
   */
  public void addDictionary(Dictionary dictionary) {
    checkArgument(dictionary != null);

    String version = dictionary.getVersion();
    if(version == null) {
      throw new DictionaryServiceException("New dictionary must specify a valid version");
    }

    if(this.getFromVersion(version) != null) {
      throw new DictionaryServiceException("cannot add an existing dictionary: " + version);
    }

    if(DictionaryState.OPENED != dictionary.getState()) {
      throw new DictionaryServiceException(String.format("New dictionary must be in OPENED state: %s instead",
          dictionary.getState()));
    }

    datastore().save(dictionary);
  }

  public List<CodeList> listCodeList() {
    return this.queryCodeList().list();
  }

  /**
   * Add new codelists to the database.
   * <p>
   * Do not reset submission states since by design no dictionary points to those new codelists yet.
   */
  public void addCodeList(List<CodeList> codeLists) {
    log.info("Saving codelists {}", codeLists);

    for(CodeList codeList : codeLists) {
      checkArgument(codeList != null);
      String name = codeList.getName();
      if(getCodeList(name).isPresent()) {
        throw new DictionaryServiceException("Aborting codelist addition: cannot add existing codeList: " + name);
      }
    }

    this.datastore().save(codeLists);
  }

  public Optional<CodeList> getCodeList(String name) {
    checkArgument(name != null);
    log.debug("retrieving codelist {}", name);
    CodeList codeList = this.queryCodeList().where(QCodeList.codeList.name.eq(name)).singleResult();
    return codeList == null ? Optional.<CodeList> absent() : Optional.<CodeList> of(codeList);
  }

  /**
   * This does not need to reset submission states as long as only inconsequential properties are updated.
   */
  public void updateCodeList(CodeList newCodeList) {
    checkArgument(newCodeList != null);
    String name = newCodeList.getName();

    Optional<CodeList> optional = this.getCodeList(name);
    if(optional.isPresent() == false) {
      throw new DictionaryServiceException("Cannot perform update to non-existant codeList: " + name);
    }

    Datastore datastore = datastore();
    datastore.update( //
        datastore.createQuery(CodeList.class).filter("name" + " = ", name), //
        datastore.createUpdateOperations(CodeList.class).set("label", newCodeList.getLabel()));
  }

  /**
   * Adds a new term to codelist.
   * <p>
   * Must reset INVALID submissions IF the nextRelease uses a dictionary that uses the corresponding codelist (as the
   * change may render them VALID). (TODO: point to spec).
   */
  public void addTerm(String codeListName, Term term) {
    checkArgument(codeListName != null);
    checkArgument(term != null);

    Optional<CodeList> optional = this.getCodeList(codeListName);
    if(optional.isPresent() == false) {
      throw new DictionaryServiceException("cannot add term to non-existant codeList: " + codeListName);
    }
    CodeList codeList = optional.get();
    if(codeList.containsTerm(term)) {
      throw new DictionaryServiceException("cannot add an existing term: " + term.getCode());
    }
    codeList.addTerm(term);

    Datastore datastore = datastore();
    datastore.update( //
        datastore.createQuery(CodeList.class).filter("name" + " = ", codeListName), //
        datastore.createUpdateOperations(CodeList.class).add("terms", term));

    // Reset INVALID submissions if applicable
    Release openRelease = releases.getNextRelease().getRelease();
    Dictionary currentDictionary = getCurrentDictionary(openRelease);
    if(currentDictionary.usesCodeList(codeListName)) {
      releases.resetSubmissions(openRelease.getName(), openRelease.getInvalidProjectKeys());
    }
  }

  public Dictionary getCurrentDictionary() {
    Release openRelease = releases.getNextRelease().getRelease();
    return getCurrentDictionary(openRelease);
  }

  public Dictionary getCurrentDictionary(Release openRelease) {
    String currentDictionaryVersion = openRelease.getDictionaryVersion();
    return getFromVersion(currentDictionaryVersion);
  }

  private Query<Dictionary> buildDictionaryVersionQuery(Dictionary dictionary) {
    return datastore().createQuery(Dictionary.class) //
        .filter("version" + " = ", dictionary.getVersion());
  }

  private MorphiaQuery<CodeList> queryCodeList() {
    return new MorphiaQuery<CodeList>(morphia(), datastore(), QCodeList.codeList);
  }
}
