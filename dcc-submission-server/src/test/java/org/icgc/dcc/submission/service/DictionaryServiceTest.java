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
package org.icgc.dcc.submission.service;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import org.icgc.dcc.submission.dictionary.DictionaryServiceException;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Term;
import org.icgc.dcc.submission.repository.CodeListRepository;
import org.icgc.dcc.submission.repository.DictionaryRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DictionaryServiceTest {

  @InjectMocks
  private DictionaryService dictionaryService;

  @Mock
  private ReleaseService mockReleaseService;
  @Mock
  private DictionaryRepository dictionaryRepository;
  @Mock
  private CodeListRepository codeListRepository;
  @Mock
  private Dictionary mockDictionary;
  @Mock
  private CodeList mockCodeList;
  @Mock
  private Term mockTerm;

  @Before
  public void setUp() {
    when(mockDictionary.getVersion()).thenReturn("abc");
    when(mockCodeList.getName()).thenReturn("def");
    when(mockTerm.getCode()).thenReturn("ghi");

    when(dictionaryRepository.countDictionariesByVersion(anyString())).thenReturn(0L);
    when(dictionaryRepository.findDictionaryByVersion(anyString())).thenReturn(mockDictionary);
  }

  @Test(expected = DictionaryServiceException.class)
  public void test_update_failOnUnexisting() {
    dictionaryService.updateDictionary(mockDictionary);
  }

  @Test(expected = DictionaryServiceException.class)
  public void test_clone_failOnSameVersion() {
    dictionaryService.cloneDictionary("v1", "v1");
  }

  @Test(expected = DictionaryServiceException.class)
  public void test_clone_failOnUnexisting() {
    dictionaryService.cloneDictionary("v1", "v2");
  }

  @Test(expected = DictionaryServiceException.class)
  public void test_clone_failOnExisting() {
    dictionaryService.cloneDictionary("v1", "v2");
  }

  @Test(expected = DictionaryServiceException.class)
  public void test_add_failOnExisting() {
    dictionaryService.addDictionary(mockDictionary);
  }

  @Test(expected = DictionaryServiceException.class)
  public void test_updateCodeList_failOnExisting() {
    dictionaryService.updateCodeList(mockCodeList);
  }

  @Test(expected = DictionaryServiceException.class)
  public void test_addTerm_failOnExisting() {
    dictionaryService.addCodeListTerm(mockCodeList.getName(), mockTerm);
  }

}
