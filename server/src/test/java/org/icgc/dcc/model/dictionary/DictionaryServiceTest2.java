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

import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.DictionaryServiceException;
import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.release.ReleaseService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.common.collect.Lists;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DictionaryServiceTest2 {

  private DictionaryService dictionaryService;

  @Mock
  private Morphia morphia;

  @Mock
  private Datastore datastore;

  @Mock
  private ReleaseService releaseService;

  @Mock
  private CodeList codeList1;

  @Mock
  private CodeList codeList2;

  @Before
  public void setUp() {
    this.dictionaryService = new DictionaryService(morphia, datastore, releaseService);

    // TODO: use partial mocking in order to mock queryCodeList() that has a constructor call (and is used by methods we
    // want to test) - DCC-897

    when(codeList1.getName()).thenReturn("codeList1");
    when(codeList2.getName()).thenReturn("codeList2");
  }

  @Test(expected = DictionaryServiceException.class)
  public void test_addCodeList_failOnUnexisting() {
    dictionaryService.addCodeList(Lists.newArrayList(codeList1, codeList2));
    // TODO: verifies
  }
}
