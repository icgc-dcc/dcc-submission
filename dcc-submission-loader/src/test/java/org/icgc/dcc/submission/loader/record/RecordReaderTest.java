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

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.util.Separators.TAB;

import java.util.List;

import lombok.Cleanup;
import lombok.val;

import org.icgc.dcc.submission.loader.util.Readers;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class RecordReaderTest {

  private static final List<String> FIELD_NAMES = ImmutableList.<String> builder()
      .add("id")
      .add("sex")
      .add("age")
      .build();

  RecordReader docReader;

  @Test
  public void testHasNext_true() throws Exception {
    val stringBuilder = Readers.getStringBuilder(FIELD_NAMES);
    stringBuilder.append(Readers.createLine("1", "male", "age"));
    val srcString = stringBuilder.toString();

    @Cleanup
    val documentReader = new RecordReader(Readers.getReader(srcString));
    assertThat(documentReader.hasNext()).isTrue();
    assertThat(documentReader.hasNext()).isTrue();
  }

  @Test
  public void testHasNext_false() throws Exception {
    val stringBuilder = Readers.getStringBuilder(FIELD_NAMES);
    val srcString = stringBuilder.toString();

    @Cleanup
    val documentReader = new RecordReader(Readers.getReader(srcString));
    assertThat(documentReader.hasNext()).isFalse();
  }

  @Test
  public void testNext() throws Exception {
    val stringBuilder = Readers.getStringBuilder(FIELD_NAMES);
    stringBuilder.append(Readers.createLine("1", "male", "age"));
    val srcString = stringBuilder.toString();

    @Cleanup
    val documentReader = new RecordReader(Readers.getReader(srcString));
    val document = documentReader.next();
    assertThat(document.size()).isEqualTo(3);
    assertThat(document.get("id")).isEqualTo("1");
    assertThat(document.get("sex")).isEqualTo("male");
    assertThat(document.get("age")).isEqualTo("age");
  }

  @Test(expected = IllegalStateException.class)
  public void testNext_malformed() throws Exception {
    val stringBuilder = Readers.getStringBuilder(FIELD_NAMES);
    stringBuilder.append(Readers.createLine("1", "male"));
    val srcString = stringBuilder.toString();

    @Cleanup
    val documentReader = new RecordReader(Readers.getReader(srcString));
    documentReader.next();
  }

  @Test
  public void testNext_empty() throws Exception {
    val stringBuilder = Readers.getStringBuilder(FIELD_NAMES);
    stringBuilder.append(TAB);
    stringBuilder.append(TAB);
    val srcString = stringBuilder.toString();

    @Cleanup
    val documentReader = new RecordReader(Readers.getReader(srcString));
    val document = documentReader.next();
    assertThat(document.size()).isEqualTo(3);
    assertThat(document.get("id")).isEqualTo("");
    assertThat(document.get("sex")).isEqualTo("");
    assertThat(document.get("age")).isEqualTo("");
  }

  @Test
  public void testNext_noInput() throws Exception {
    val stringBuilder = Readers.getStringBuilder(FIELD_NAMES);
    val srcString = stringBuilder.toString();

    @Cleanup
    val documentReader = new RecordReader(Readers.getReader(srcString));
    // assertThat(documentReader.next()).isNull() does not work
    checkState(documentReader.next() == null);
  }

}
