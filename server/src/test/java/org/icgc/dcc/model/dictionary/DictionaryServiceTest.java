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
package org.icgc.dcc.model.dictionary;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.DictionaryServiceException;
import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.Term;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.release.ReleaseService;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.types.Predicate;

public class DictionaryServiceTest {

  private Morphia mockMorphia;

  private Datastore mockDatastore;

  private MongodbQuery<Dictionary> mockMongodbQuery;

  private Query<Dictionary> mockQuery;

  private Dictionary mockDictionary;

  private CodeList mockCodeList;

  private Term mockTerm;

  private DictionaryService dictionaryService;

  private DccFileSystem mockDccFileSystem;

  private ReleaseService mockReleaseService;

  @Before
  @SuppressWarnings("unchecked")
  // TODO: how to mock parametized?
  public void setUp() {
    mockMorphia = mock(Morphia.class);
    mockDatastore = mock(Datastore.class);
    mockMongodbQuery = mock(MongodbQuery.class);
    mockDictionary = mock(Dictionary.class);
    mockCodeList = mock(CodeList.class);
    mockTerm = mock(Term.class);
    mockQuery = mock(Query.class);
    mockDccFileSystem = mock(DccFileSystem.class);
    mockReleaseService = mock(ReleaseService.class);

    when(mockDictionary.getVersion()).thenReturn("abc");
    when(mockCodeList.getName()).thenReturn("def");
    when(mockTerm.getCode()).thenReturn("ghi");
    when(mockDatastore.createQuery(Dictionary.class)).thenReturn(mockQuery);
    when(mockQuery.filter(anyString(), anyString())).thenReturn(mockQuery);
    when(mockQuery.countAll()).thenReturn(0L).thenReturn(0L);
    when(mockMongodbQuery.where(any(Predicate.class))).thenReturn(mockMongodbQuery);
    when(mockMongodbQuery.singleResult()).thenReturn(null).thenReturn(mockDictionary).thenReturn(mockDictionary);

    this.dictionaryService = new DictionaryService(mockMorphia, mockDatastore, mockDccFileSystem, mockReleaseService);
  }

  @Test(expected = DictionaryServiceException.class)
  // TODO: is there a way to check message too?
  public void test_update_failOnUnexisting() {
    dictionaryService.update(mockDictionary);
  }

  @Test(expected = DictionaryServiceException.class)
  public void test_clone_failOnSameVersion() {
    dictionaryService.clone("v1", "v1");
  }

  // TODO: all further test would require to dig into mocking querydsl's MorphiaQuery constructor...
  @Ignore
  @Test(expected = DictionaryServiceException.class)
  public void test_clone_failOnUnexisting() {
    dictionaryService.clone("v1", "v2");
  }

  @Ignore
  @Test(expected = DictionaryServiceException.class)
  public void test_clone_failOnExisting() {
    dictionaryService.clone("v1", "v2");
  }

  @Ignore
  @Test(expected = DictionaryServiceException.class)
  public void test_add_failOnExisting() {
    dictionaryService.addDictionary(mockDictionary);
  }

  @Ignore
  @Test(expected = DictionaryServiceException.class)
  public void test_updateCodeList_failOnExisting() {
    dictionaryService.updateCodeList(mockCodeList);
  }

  @Ignore
  @Test(expected = DictionaryServiceException.class)
  public void test_addTerm_failOnExisting() {
    dictionaryService.addTerm(mockCodeList.getName(), mockTerm);
  }
}
