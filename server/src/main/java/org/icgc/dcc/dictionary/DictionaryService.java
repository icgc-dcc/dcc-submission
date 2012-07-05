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
package org.icgc.dcc.dictionary;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import org.icgc.dcc.core.morphia.BaseMorphiaService;
import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.DictionaryState;
import org.icgc.dcc.dictionary.model.QCodeList;
import org.icgc.dcc.dictionary.model.QDictionary;
import org.icgc.dcc.dictionary.model.Term;
import org.icgc.dcc.dictionary.visitor.DictionaryCloneVisitor;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.inject.Inject;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.Predicate;

/**
 * Offers various CRUD operations pertaining to {@code Dictionary}
 */
public class DictionaryService extends BaseMorphiaService {

  @Inject
  public DictionaryService(Morphia morphia, Datastore datastore) {
    super(morphia, datastore);
    registerModelClasses(Dictionary.class, CodeList.class);
  }

  public MongodbQuery<Dictionary> query() {
    return new MorphiaQuery<Dictionary>(morphia(), datastore(), QDictionary.dictionary);
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
    checkArgument(dictionary != null);
    Query<Dictionary> updateQuery = this.buildQuery(dictionary);
    if(updateQuery.countAll() != 1) {
      throw new DictionaryServiceException("cannot update an unexisting dictionary: " + dictionary.getVersion());
    }
    datastore().updateFirst(updateQuery, dictionary, false);
  }

  public void close(Dictionary dictionary) {
    checkArgument(dictionary != null);
    Query<Dictionary> updateQuery = this.buildQuery(dictionary);
    if(updateQuery.countAll() != 1) {
      throw new DictionaryServiceException("cannot close an unexisting dictionary: " + dictionary.getVersion());
    }
    UpdateOperations<Dictionary> ops =
        datastore().createUpdateOperations(Dictionary.class).set("state", DictionaryState.CLOSED);
    datastore().update(updateQuery, ops);
  }

  public Dictionary clone(String oldVersion, String newVersion) {
    checkArgument(oldVersion != null);
    checkArgument(newVersion != null);
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

    DictionaryCloneVisitor dictionaryCloneVisitor = new DictionaryCloneVisitor();
    oldDictionary.accept(dictionaryCloneVisitor);

    Dictionary newDictionary = dictionaryCloneVisitor.getDictionaryClone();
    newDictionary.setVersion(newVersion);

    this.add(newDictionary);

    return newDictionary;
  }

  public void add(Dictionary dictionary) {
    checkArgument(dictionary != null);
    String version = dictionary.getVersion();
    if(this.getFromVersion(version) != null) {
      throw new DictionaryServiceException("cannot add an existing dictionary: " + version);
    }

    datastore().save(dictionary);
  }

  public List<CodeList> listCodeList() {
    return this.queryCodeList().list();
  }

  public CodeList getCodeList(String name) {
    checkArgument(name != null);
    return this.queryCodeList().where(QCodeList.codeList.name.eq(name)).singleResult();
  }

  private MorphiaQuery<CodeList> queryCodeList() {
    return new MorphiaQuery<CodeList>(morphia(), datastore(), QCodeList.codeList);
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
    CodeList oldCodeList = this.getCodeList(name);
    if(oldCodeList == null) {
      throw new DictionaryServiceException("cannot perform update to non-existant codeList: " + name);
    }

    oldCodeList.setLabel(newCodeList.getLabel());
    Query<CodeList> updateQuery = datastore().createQuery(CodeList.class).filter("name" + " = ", name);
    checkState(updateQuery.countAll() == 1);
    UpdateOperations<CodeList> ops =
        datastore().createUpdateOperations(CodeList.class).set("label", newCodeList.getLabel());
    datastore().update(updateQuery, ops);
  }

  public void addTerm(String name, Term term) {
    checkArgument(name != null);
    checkArgument(term != null);

    CodeList codeList = this.getCodeList(name);
    if(codeList.containsTerm(term)) {
      throw new DictionaryServiceException("cannot add an existing term: " + term.getCode());
    }
    codeList.addTerm(term);

    Query<CodeList> updateQuery = datastore().createQuery(CodeList.class).filter("name" + " = ", name);
    checkState(updateQuery.countAll() == 1);
    UpdateOperations<CodeList> ops = datastore().createUpdateOperations(CodeList.class).add("terms", term);
    datastore().update(updateQuery, ops);
  }

  private Query<Dictionary> buildQuery(Dictionary dictionary) {
    return datastore().createQuery(Dictionary.class).filter("version" + " = ", dictionary.getVersion());
  }
}
