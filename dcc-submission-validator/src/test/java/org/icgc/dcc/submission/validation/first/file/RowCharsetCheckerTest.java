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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.first.io.FPVFileSystem;
import org.icgc.dcc.submission.validation.first.row.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;

@RunWith(MockitoJUnitRunner.class)
public class RowCharsetCheckerTest {

  @Mock
  private RowNoOpChecker baseChecker;

  @Mock
  private FileSchema fileSchema;

  @Mock
  ValidationContext validationContext;
  @Mock
  FPVFileSystem fs;

  @Mock
  private Dictionary dict;

  @Before
  public void setup() {
    FileSchema testSchema = mock(FileSchema.class);
    String paramString = "testfile1";
    when(testSchema.getPattern()).thenReturn(paramString);
    when(dict.getFileSchemaByName(anyString())).thenReturn(Optional.of(testSchema));
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(Optional.of(testSchema));
    when(validationContext.getDictionary()).thenReturn(dict);
    when(baseChecker.getReportContext()).thenReturn(validationContext);
  }

  @Test
  public void sanity() throws Exception {
    String test_text = "<Hello-World> \t a b c F G Z 0 1 6 9 !?";
    RowCharsetChecker checker = new RowCharsetChecker(baseChecker);
    checker.performSelfCheck("myfile", fileSchema, test_text, 1);
    TestUtils.checkNoErrorsReported(validationContext);
  }

  @Test
  public void invalidLineSeparator() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("f1\tf2\r\na\tb\r\n".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);

    RowCharsetChecker checker = new RowCharsetChecker(new RowNoOpChecker(validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkRowCharsetErrorReported(validationContext, 2);
  }

  @Test
  public void invalidContentWithCarriageReturn() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("f1\t\rf2\na\r\tb\n".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);

    RowCharsetChecker checker = new RowCharsetChecker(new RowNoOpChecker(validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkRowCharsetErrorReported(validationContext, 2);
  }

  @Test
  public void validCharacters() throws Exception {
    StringBuilder sb = new StringBuilder(127 - 32);
    for (int i = 32; i < 127; ++i) {
      sb.append((char) i);
    }
    RowCharsetChecker checker = new RowCharsetChecker(baseChecker);
    checker.performSelfCheck("myfile", fileSchema, sb.toString(), 1);
    TestUtils.checkNoErrorsReported(validationContext);
  }

  @Test
  public void invalidCharacters() throws Exception {
    StringBuilder sb = new StringBuilder(9 - 0 + 32 - 10);
    for (int i = 0; i < 9; ++i) {
      sb.append((char) i);
    }
    for (int i = 10; i < 32; ++i) {
      sb.append((char) i);
    }
    RowCharsetChecker checker = new RowCharsetChecker(baseChecker);
    checker.performSelfCheck("myfile", fileSchema, sb.toString(), 1);
    TestUtils.checkRowCharsetErrorReported(validationContext, 1);
  }

  @Test
  public void validTabCharacter() throws Exception {
    char tabChar = 9;
    String test_string = Character.toString(tabChar);
    RowCharsetChecker checker = new RowCharsetChecker(baseChecker);
    checker.performSelfCheck("myfile", fileSchema, test_string, 1);
    TestUtils.checkNoErrorsReported(validationContext);
  }

  @Test
  public void invalidNullCharacter() throws Exception {
    char nullChar = 0;
    String test_string = Character.toString(nullChar);
    RowCharsetChecker checker = new RowCharsetChecker(baseChecker);
    checker.performSelfCheck("myfile", fileSchema, test_string, 1);
    TestUtils.checkRowCharsetErrorReported(validationContext, 1);
  }

  @Test
  public void invalidCarriageReturnCharacter() throws Exception {
    char carriageReturnChar = 13;
    String test_string = Character.toString(carriageReturnChar);
    RowCharsetChecker checker = new RowCharsetChecker(baseChecker);
    checker.performSelfCheck("myfile", fileSchema, test_string, 1);
    TestUtils.checkRowCharsetErrorReported(validationContext, 1);
  }

  @Test
  public void invalidCedillaCharacter() throws Exception {
    byte[] invalidBytes = new byte[] { (byte) 0xc3, (byte) 0xa7 };
    String test_string = new String(invalidBytes);
    // System.out.println(test_string);
    RowCharsetChecker checker = new RowCharsetChecker(baseChecker);
    checker.performSelfCheck("myfile", fileSchema, test_string, 1);
    TestUtils.checkRowCharsetErrorReported(validationContext, 1);
  }
}