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
package org.icgc.dcc.submission.validation.accession.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import lombok.val;

public class AccessionDictionaryTest {

  @Test
  public void testIsExcludedById() throws Exception {
    val dictionary = new AccessionDictionary();
    val excluded = dictionary.isExcluded("PAEN-AU", "analysis1");
    assertThat(excluded).isTrue();
  }

  @Test
  public void testIsExcludedByPattern() throws Exception {
    val dictionary = new AccessionDictionary();
    val excluded = dictionary.isExcluded("PAEN-AU", "analysis2");
    assertThat(excluded).isTrue();
  }

  @Test
  public void testIsNotExcludedProjectDefined() throws Exception {
    val dictionary = new AccessionDictionary();
    val excluded = dictionary.isExcluded("PAEN-AU", "foo");
    assertThat(excluded).isFalse();
  }

  @Test
  public void testIsNotExcludedProjectNotDefined() throws Exception {
    val dictionary = new AccessionDictionary();
    val excluded = dictionary.isExcluded("DNE", "bar");
    assertThat(excluded).isFalse();
  }

}
