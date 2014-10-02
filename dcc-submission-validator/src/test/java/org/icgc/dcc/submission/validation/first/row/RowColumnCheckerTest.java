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
package org.icgc.dcc.submission.validation.first.row;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.first.io.FPVFileSystem;
import org.icgc.dcc.submission.validation.first.row.NoOpRowChecker;
import org.icgc.dcc.submission.validation.first.row.RowColumnChecker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class RowColumnCheckerTest {

  @Mock
  private Dictionary dict;
  @Mock
  ValidationContext validationContext;
  @Mock
  FPVFileSystem fs;

  @Before
  public void setup() {
    FileSchema testSchema = mock(FileSchema.class);
    String paramString = "testfile1";
    when(testSchema.getPattern()).thenReturn(paramString);
    when(dict.getFileSchemaByName(anyString())).thenReturn(Optional.of(testSchema));
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(Optional.of(testSchema));

    Optional<FileSchema> option = Optional.of(testSchema);
    Field f1 = new Field();
    f1.setName("a");
    Field f2 = new Field();
    f2.setName("b");
    when(testSchema.getFields()).thenReturn(ImmutableList.of(f1, f2));
    when(dict.getFileSchemaByName(anyString())).thenReturn(option);

    when(validationContext.getDictionary()).thenReturn(dict);
  }

  @Test
  public void validColumns() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("a\tb\nf1\tf2\n".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);

    RowColumnChecker checker = new RowColumnChecker(new NoOpRowChecker(validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void invalidColumnsHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("a\nf1\t\f2\n".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);

    RowColumnChecker checker = new RowColumnChecker(new NoOpRowChecker(validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkRowColumnErrorReported(validationContext, 1);
  }

  @Test
  public void missingNewLine() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("a\tb\n\t\n\t\t".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);

    RowColumnChecker checker = new RowColumnChecker(new NoOpRowChecker(validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkRowColumnErrorReported(validationContext, 1);
  }

  @Test
  public void invalidColumnsContent() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("a\tb\nf2\n".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);

    RowColumnChecker checker = new RowColumnChecker(new NoOpRowChecker(validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkRowColumnErrorReported(validationContext, 1);
  }

  @Test
  public void invalidColumnsHeaderAndContent() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("a\nf2\n".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);

    RowColumnChecker checker = new RowColumnChecker(new NoOpRowChecker(validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkRowColumnErrorReported(validationContext, 2);
  }

  @Test
  public void invalidIrregularColumns() throws Exception {
    DataInputStream fis =
        new DataInputStream(new ByteArrayInputStream("a\tb\tc\nf1\tf2\tf3\tf3\tf4\n\f1\n".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);

    RowColumnChecker checker = new RowColumnChecker(new NoOpRowChecker(validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkRowColumnErrorReported(validationContext, 3);
  }

  @Test
  public void validEmptyColumns() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("\t\n\t\n".getBytes()));
    when(fs.getDecompressingInputStream(anyString())).thenReturn(fis);

    RowColumnChecker checker = new RowColumnChecker(new NoOpRowChecker(validationContext, fs));
    checker.checkFile(anyString());
    TestUtils.checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }
}
