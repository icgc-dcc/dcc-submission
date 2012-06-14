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
package org.icgc.dcc.model.dictionary;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.Predicate;

/**
 * Offers various CRUD operations pertaining to {@code Dictionary}
 */
public class DictionaryService {

  private static final String DICTIONARY_VERSION = Iterables.getLast(Splitter.on(".").split(
      QDictionary.dictionary.version.toString()));

  private static final String DICTIONARY_STATE = Iterables.getLast(Splitter.on(".").split(
      QDictionary.dictionary.state.toString()));

  private static final String CODE_LIST_NAME = Iterables.getLast(Splitter.on(".").split(
      QCodeList.codeList.name.toString()));

  private static final String CODE_LIST_TERMS = Iterables.getLast(Splitter.on(".").split(
      QCodeList.codeList.terms.toString()));

  private final Morphia morphia;

  private final Datastore datastore;

  @Inject
  public DictionaryService(Morphia morphia, Datastore datastore) {
    super();

    checkArgument(morphia != null);
    checkArgument(datastore != null);

    this.morphia = morphia;
    this.datastore = datastore;
  }

  public Datastore datastore() {
    return datastore;
  }

  public MongodbQuery<Dictionary> query() {
    return new MorphiaQuery<Dictionary>(morphia, datastore, QDictionary.dictionary);
  }

  public MongodbQuery<Dictionary> where(Predicate predicate) {
    return query().where(predicate);
  }

  public List<Dictionary> list() {
    return this.query().list();
  }

  public Dictionary getFromVersion(String version) {
    return this.where(QDictionary.dictionary.version.eq(version)).singleResult();
  }

  public void update(Dictionary dictionary) {
    Query<Dictionary> udpateQuery = this.buildQuery(dictionary);
    checkState(udpateQuery.countAll() == 1);
    this.datastore.updateFirst(udpateQuery, dictionary, false);
  }

  public void close(Dictionary dictionary) {
    Query<Dictionary> updateQuery = this.buildQuery(dictionary);
    checkState(updateQuery.countAll() == 1);
    UpdateOperations<Dictionary> ops =
        this.datastore.createUpdateOperations(Dictionary.class).disableValidation()
            .set(DICTIONARY_STATE, DictionaryState.CLOSED);
    this.datastore.update(updateQuery, ops);
  }

  public Dictionary clone(String oldVersion, String newVersion) {
    if(oldVersion.equals(newVersion)) {
      throw new DictionaryServiceException("cannot clone a dictionary using the same version: " + newVersion);
    }
    Dictionary oldDictionary = this.getFromVersion(oldVersion);
    if(oldDictionary == null) {
      throw new DictionaryServiceException("cannot clone an unexisting dictionary: " + oldVersion);
    }
    if(getFromVersion(newVersion) != null) {
      throw new DictionaryServiceException("cannot clone to an already existing dictionary: " + newVersion);
    }

    Dictionary newDictionary = new Dictionary(oldDictionary);
    newDictionary.setVersion(newVersion);

    this.datastore.save(newDictionary);

    return newDictionary;
  }

  public CodeList getCodeList(String name) {
    return new MorphiaQuery<CodeList>(morphia, datastore, QCodeList.codeList).where(QCodeList.codeList.name.eq(name))
        .singleResult();
  }

  public CodeList createCodeList(String name) {
    CodeList codeList = new CodeList(name);
    this.datastore.save(codeList);
    return codeList;
  }

  public void addTerm(String name, Term term) {
    checkArgument(name != null);
    checkArgument(term != null);

    CodeList codeList = this.getCodeList(name);
    if(codeList.containsTerm(term)) {
      throw new DictionaryServiceException("cannot add an existing term: " + term.getCode());
    }
    codeList.addTerm(term);

    Query<CodeList> updateQuery = this.datastore.createQuery(CodeList.class).filter(CODE_LIST_NAME + " = ", name);
    checkState(updateQuery.countAll() == 1);
    UpdateOperations<CodeList> ops =
        this.datastore.createUpdateOperations(CodeList.class).disableValidation().add(CODE_LIST_TERMS, term);
    this.datastore.update(updateQuery, ops);
  }

  private Query<Dictionary> buildQuery(Dictionary dictionary) {
    return this.datastore.createQuery(Dictionary.class).filter(DICTIONARY_VERSION + " = ", dictionary.getVersion());
  }
}
