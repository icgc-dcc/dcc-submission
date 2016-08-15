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

package org.icgc.dcc.submission.repository;

import static org.icgc.dcc.submission.dictionary.model.QCodeList.codeList;

import java.util.List;

import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.QCodeList;
import org.icgc.dcc.submission.dictionary.model.Term;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.NonNull;

public class CodeListRepository extends AbstractRepository<CodeList, QCodeList> {

  @Autowired
  public CodeListRepository(@NonNull Morphia morphia, @NonNull Datastore datastore) {
    super(morphia, datastore, codeList);
  }

  public List<CodeList> findCodeLists() {
    return list();
  }

  public CodeList findCodeListByName(@NonNull String codeListName) {
    return uniqueResult(entity.name.eq(codeListName));
  }

  public void saveCodeLists(@NonNull List<CodeList> codeLists) {
    save(codeLists);
  }

  public void updateCodeList(@NonNull String codeListName, @NonNull CodeList updatedCodeList) {
    update(
        createQuery()
            .filter("name", codeListName),
        createUpdateOperations()
            .set("label", updatedCodeList.getLabel()));
  }

  public void addCodeListTerm(@NonNull String codeListName, @NonNull Term newTerm) {
    update(
        createQuery()
            .filter("name", codeListName),
        createUpdateOperations()
            .add("terms", newTerm));
  }

}
