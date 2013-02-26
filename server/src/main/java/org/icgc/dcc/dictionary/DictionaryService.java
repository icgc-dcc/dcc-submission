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
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.release.NextRelease;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.Release;
import org.mortbay.log.Log;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Offers various CRUD operations pertaining to {@code Dictionary}
 */
public class DictionaryService extends BaseMorphiaService<Dictionary> {

  private final DccFileSystem fs;

  private final ReleaseService releases;

  @Inject
  public DictionaryService(Morphia morphia, Datastore datastore, DccFileSystem fs, ReleaseService releases) {
    super(morphia, datastore, QDictionary.dictionary);
    checkArgument(fs != null);
    checkArgument(releases != null);
    this.fs = fs;
    this.releases = releases;
    registerModelClasses(Dictionary.class, CodeList.class);
  }

  public List<Dictionary> list() {
    return this.query().list();
  }

  public Dictionary getFromVersion(String version) {
    return this.where(QDictionary.dictionary.version.eq(version)).singleResult();
  }

  public void update(Dictionary dictionary) {
    checkArgument(dictionary != null);
    Query<Dictionary> updateQuery = this.buildDictionaryVersionQuery(dictionary);
    if(updateQuery.countAll() != 1) {
      throw new DictionaryServiceException("cannot update a non-existent dictionary: " + dictionary.getVersion());
    }
    datastore().updateFirst(updateQuery, dictionary, false);

    NextRelease nextRelease = releases.getNextRelease();
    Release release = nextRelease.getRelease();
    ReleaseFileSystem releaseFilesystem = fs.getReleaseFilesystem(release);
    releaseFilesystem.emptyValidationFolders(); // else cascade may not rerun (DCC-416)

    releases.resetSubmissions(release);
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

  public void addCodeList(List<CodeList> codeLists) {
    this.datastore().save(codeLists);
  }

  public Optional<CodeList> getCodeList(String name) {
    checkArgument(name != null);
    Log.debug("retrieving codelist {}", name);
    CodeList codeList = this.queryCodeList().where(QCodeList.codeList.name.eq(name)).singleResult();
    return codeList == null ? Optional.<CodeList> absent() : Optional.<CodeList> of(codeList);
  }

  public CodeList createCodeList(String name) {
    checkArgument(name != null);
    CodeList codeList = new CodeList(name);
    if(getCodeList(name) != null) {
      throw new DictionaryServiceException("cannot create existant codeList: " + name);
    }
    datastore().save(codeList);
    return codeList;
  }

  public void updateCodeList(CodeList newCodeList) {
    checkArgument(newCodeList != null);
    String name = newCodeList.getName();
    Optional<CodeList> optional = this.getCodeList(name);
    if(optional.isPresent() == false) {
      throw new DictionaryServiceException("cannot perform update to non-existant codeList: " + name);
    }

    Query<CodeList> updateQuery = datastore().createQuery(CodeList.class).filter("name" + " = ", name);
    checkState(updateQuery.countAll() == 1);
    UpdateOperations<CodeList> ops =
        datastore().createUpdateOperations(CodeList.class).set("label", newCodeList.getLabel());
    datastore().update(updateQuery, ops);
  }

  public void addTerm(String name, Term term) {
    checkArgument(name != null);
    checkArgument(term != null);

    Optional<CodeList> optional = this.getCodeList(name);
    if(optional.isPresent() == false) {
      throw new DictionaryServiceException("cannot add term to non-existant codeList: " + name);
    }
    CodeList codeList = optional.get();
    if(codeList.containsTerm(term)) {
      throw new DictionaryServiceException("cannot add an existing term: " + term.getCode());
    }
    codeList.addTerm(term);

    Query<CodeList> updateQuery = datastore().createQuery(CodeList.class).filter("name" + " = ", name);
    checkState(updateQuery.countAll() == 1);
    UpdateOperations<CodeList> ops = datastore().createUpdateOperations(CodeList.class).add("terms", term);
    datastore().update(updateQuery, ops);
  }

  private Query<Dictionary> buildDictionaryVersionQuery(Dictionary dictionary) {
    return datastore().createQuery(Dictionary.class) //
        .filter("version" + " = ", dictionary.getVersion());
  }

  private MorphiaQuery<CodeList> queryCodeList() {
    return new MorphiaQuery<CodeList>(morphia(), datastore(), QCodeList.codeList);
  }
}
