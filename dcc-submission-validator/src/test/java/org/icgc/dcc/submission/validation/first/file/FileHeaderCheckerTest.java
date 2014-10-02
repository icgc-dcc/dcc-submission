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

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.first.core.FileChecker;
import org.icgc.dcc.submission.validation.first.file.FileHeaderChecker;
import org.icgc.dcc.submission.validation.first.file.NoOpFileChecker;
import org.icgc.dcc.submission.validation.first.io.FPVFileSystem;import org.icgc.dcc.submission.validation.first.row.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class FileHeaderCheckerTest {

  private Dictionary dict;

  @Mock
  ValidationContext validationContext;
  @Mock
  FPVFileSystem fs;

  @Before
  public void setup() {
    dict = mock(Dictionary.class);

    FileSchema fileSchema = mock(FileSchema.class);
    Optional<FileSchema> option = Optional.of(fileSchema);
    when(fileSchema.getFieldNames()).thenReturn(ImmutableList.of("a", "b"));
    when(dict.getFileSchemaByName(anyString())).thenReturn(option);
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(
        Optional.of(fileSchema));

    when(validationContext.getDictionary()).thenReturn(dict);
  }

  @Test
  public void simpleValidation() throws Exception {
    when(fs.peekFileHeader(anyString())).thenReturn(newArrayList("a", "b"));

    FileChecker checker = new FileHeaderChecker(new NoOpFileChecker(
        validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkNoErrorsReported(validationContext);
  }

  @Test
  public void notValidMissingHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream(
        "a\tr".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);
    FileChecker checker = new FileHeaderChecker(new NoOpFileChecker(
        validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkFileHeaderErrorReported(validationContext, 1);
  }

  @Test
  public void notValidOutOfOrderHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream(
        "b\ta\rf1\t\f2\r".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);
    FileChecker checker = new FileHeaderChecker(new NoOpFileChecker(
        validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkFileHeaderErrorReported(validationContext, 1);
  }

  @Test
  public void notValidDuplicateHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream(
        "b\tb\rf1\t\f2\r".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);
    FileChecker checker = new FileHeaderChecker(new NoOpFileChecker(
        validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkFileHeaderErrorReported(validationContext, 1);
  }

  @Test
  public void notValidExtraHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream(
        "a\tb\tc\rf1\t\f2\r".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);
    FileChecker checker = new FileHeaderChecker(new NoOpFileChecker(
        validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkFileHeaderErrorReported(validationContext, 1);

  }

  @Test
  public void notValidWhiteSpacesInHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream(
        " a \tb\rf1\t\f2\r".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);
    FileChecker checker = new FileHeaderChecker(new NoOpFileChecker(
        validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkFileHeaderErrorReported(validationContext, 1);

  }

  @Test
  public void notValidCapitalHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream(
        "A\tB\rf1\t\f2\r".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);
    FileChecker checker = new FileHeaderChecker(new NoOpFileChecker(
        validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkFileHeaderErrorReported(validationContext, 1);

  }

  @Test
  public void notValidMispellHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream(
        "ab\tb\rf1\t\f2\r".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);
    FileChecker checker = new FileHeaderChecker(new NoOpFileChecker(
        validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkFileHeaderErrorReported(validationContext, 1);

  }

  @Test
  public void notValidNoContentHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream(
        new byte[0]));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);
    FileChecker checker = new FileHeaderChecker(new NoOpFileChecker(
        validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkFileHeaderErrorReported(validationContext, 1);
  }

}
