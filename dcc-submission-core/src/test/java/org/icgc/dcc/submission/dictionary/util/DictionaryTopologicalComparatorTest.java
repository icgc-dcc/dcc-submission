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
package org.icgc.dcc.submission.dictionary.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.METH_ARRAY_M_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.METH_ARRAY_PROBES_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.METH_ARRAY_P_TYPE;

import java.util.Collections;

import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.test.AbstractDictionaryTest;
import org.junit.Test;

import com.google.common.collect.Lists;

public class DictionaryTopologicalComparatorTest extends AbstractDictionaryTest {

  DictionaryTopologicalComparator comparator;
  Dictionary dictionary;

  @Override
  @SneakyThrows
  public void setUp() {
    super.setUp();
    dictionary = Dictionaries.readDictionary(dictionaryFile.toURI().toURL());
    comparator = new DictionaryTopologicalComparator();
  }

  @Test
  public void testCompareTo() throws Exception {
    val methArrayM = dictionary.getFileSchema(METH_ARRAY_M_TYPE);
    val methArrayP = dictionary.getFileSchema(METH_ARRAY_P_TYPE);
    val methArrayProbes = dictionary.getFileSchema(METH_ARRAY_PROBES_TYPE);
    assertThat(comparator.compare(methArrayM, methArrayP)).isEqualTo(-1);
    assertThat(comparator.compare(methArrayM, methArrayProbes)).isEqualTo(-1);
    assertThat(comparator.compare(methArrayP, methArrayProbes)).isEqualTo(1);

    val fileTypes = Lists.newArrayList(METH_ARRAY_PROBES_TYPE, METH_ARRAY_P_TYPE, METH_ARRAY_M_TYPE);
    Collections.sort(fileTypes);
    assertThat(fileTypes).containsExactly(METH_ARRAY_M_TYPE, METH_ARRAY_PROBES_TYPE, METH_ARRAY_P_TYPE);
  }

}
