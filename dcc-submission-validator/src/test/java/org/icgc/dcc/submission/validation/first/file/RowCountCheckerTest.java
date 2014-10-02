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
package org.icgc.dcc.submission.validation.first.file;

import static org.icgc.dcc.submission.validation.first.row.TestUtils.checkErrorReported;
import static org.icgc.dcc.submission.validation.first.row.TestUtils.checkNoErrorsReported;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import lombok.val;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.first.io.FPVFileSystem;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class RowCountCheckerTest {

  static final String TEST_FILE_NAME = "testfile1";

  @Mock
  private Dictionary dictionay;
  @Mock
  ValidationContext validationContext;
  @Mock
  FPVFileSystem fs;

  RowCountChecker checker;

  @Before
  public void setup() {
    Field f1 = new Field();
    f1.setName("a");
    Field f2 = new Field();
    f2.setName("b");

    FileSchema testSchema = mock(FileSchema.class);
    Optional<FileSchema> option = Optional.of(testSchema);
    when(testSchema.getPattern()).thenReturn(TEST_FILE_NAME);
    when(testSchema.getFields()).thenReturn(ImmutableList.of(f1, f2));

    when(dictionay.getFileSchemaByName(anyString())).thenReturn(Optional.of(testSchema));
    when(dictionay.getFileSchemaByFileName(anyString())).thenReturn(Optional.of(testSchema));
    when(dictionay.getFileSchemaByName(anyString())).thenReturn(option);

    when(validationContext.getDictionary()).thenReturn(dictionay);

    this.checker = new RowCountChecker(new RowNoOpChecker(validationContext, fs));
  }

  @Test
  public void testRowsPresent() throws Exception {
    val input = new DataInputStream(new ByteArrayInputStream("a\tb\nf1\tf2\n".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(input);

    checker.checkFile(TEST_FILE_NAME);

    checkNoErrorsReported(validationContext);
  }

  @Test
  public void testRowsMissing() throws Exception {
    val input = new DataInputStream(new ByteArrayInputStream("a\tb\n".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(input);

    checker.checkFile(TEST_FILE_NAME);

    checkErrorReported(validationContext, 1);
  }

}
