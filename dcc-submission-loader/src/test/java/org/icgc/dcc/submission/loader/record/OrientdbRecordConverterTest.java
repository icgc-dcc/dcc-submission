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
package org.icgc.dcc.submission.loader.record;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.submission.loader.util.Fields.PROJECT_ID;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import lombok.val;

import org.icgc.dcc.submission.loader.meta.CodeListValuesDecoder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;

@RunWith(MockitoJUnitRunner.class)
public class OrientdbRecordConverterTest {

  @Mock
  CodeListValuesDecoder codeListDecoder;

  OrientdbRecordConverter converter;

  @Before
  public void setUp() {
    when(codeListDecoder.decode(anyString(), anyString())).then(invocation -> invocation.getArguments()[1]);

    converter = new OrientdbRecordConverter("Donor", "ALL-US", codeListDecoder);
  }

  @Test
  public void testConvert() throws Exception {
    val record = ImmutableMap.of("donor_id", "DO1", "donor_sex", "male");
    val doc = converter.convert(record);
    assertThat(doc.fields()).isEqualTo(3);
    assertThat(doc.field("donor_id").toString()).isEqualTo("DO1");
    assertThat(doc.field("donor_sex").toString()).isEqualTo("male");
    assertThat(doc.field(PROJECT_ID).toString()).isEqualTo("ALL-US");
  }

}
