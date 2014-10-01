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
package org.icgc.dcc.submission.validation.first.io;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import lombok.val;

import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.first.core.FileChecker;
import org.icgc.dcc.submission.validation.first.core.RowChecker;
import org.icgc.dcc.submission.validation.first.step.CompositeFileChecker;
import org.icgc.dcc.submission.validation.first.step.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class FPVSubmissionProcessorTest {

  private Dictionary dict;

  @Mock
  ValidationContext validationContext;
  @Mock
  FPVFileSystem fs;

  @Before
  public void setup() throws IOException {
    dict = mock(Dictionary.class);

    when(fs.listMatchingSubmissionFiles(Mockito.anyListOf(String.class))).thenReturn(ImmutableList.of("anyfile"));
    when(fs.getMatchingFileNames(anyString())).thenReturn(ImmutableList.of("anyfile"));

    FileSchema schema = new FileSchema("anyfile");
    schema.setPattern("anyfile");
    when(dict.getFiles()).thenReturn(newArrayList(schema));
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(Optional.of(schema));
    when(dict.getFileSchemata(anyDataTypeIterable())).thenReturn(ImmutableList.<FileSchema> of(schema));

    when(validationContext.getDictionary()).thenReturn(dict);
  }

  @Test
  public void sanityValid() throws IOException {
    FileChecker dummyFileChecker = mock(FileChecker.class);
    when(dummyFileChecker.canContinue()).thenReturn(true);
    when(dummyFileChecker.isValid()).thenReturn(true);

    RowChecker dummyRowChecker = mock(RowChecker.class);
    when(dummyRowChecker.canContinue()).thenReturn(true);
    when(dummyRowChecker.isValid()).thenReturn(true);

    val fpv = new FPVSubmissionProcessor();
    fpv.setFileChecker(dummyFileChecker);
    fpv.setRowChecker(dummyRowChecker);
    fpv.process("mystepname", validationContext, fs);

    TestUtils.checkNoErrorsReported(validationContext);
    verify(dummyFileChecker, times(1)).checkFile(anyString());
    verify(dummyRowChecker, times(1)).checkFile(anyString());
  }

  @Test
  public void sanityNotValid() throws Exception {
    FileChecker fileChecker = mock(FileChecker.class);
    when(fileChecker.isValid()).thenReturn(false);
    when(fileChecker.isFailFast()).thenReturn(false);
    when(fileChecker.canContinue()).thenReturn(true);

    RowChecker rowChecker = mock(RowChecker.class);
    when(rowChecker.isValid()).thenReturn(false);
    when(rowChecker.isFailFast()).thenReturn(false);
    when(rowChecker.canContinue()).thenReturn(true);

    val fpv = new FPVSubmissionProcessor();
    fpv.setFileChecker(fileChecker);
    fpv.setRowChecker(rowChecker);
    fpv.process("mystepname", validationContext, fs);

    verify(fileChecker, times(1)).checkFile(anyString());
    verify(rowChecker, times(1)).checkFile(anyString());
  }

  @Test
  public void sanityNotValidFileLevelFailFast() throws IOException {
    FileChecker fileChecker = mock(FileChecker.class);
    when(fileChecker.isValid()).thenReturn(false);
    when(fileChecker.isFailFast()).thenReturn(true); // fail it right away

    CompositeFileChecker moreChecker = PowerMockito.spy(
        new DummyFileCheckerUnderTest(
            new DummyFileCheckerUnderTest(
                fileChecker,
                false),
            false));

    RowChecker rowChecker = mock(RowChecker.class);
    when(rowChecker.isValid()).thenReturn(true);

    val fpv = new FPVSubmissionProcessor();
    fpv.setFileChecker(fileChecker);
    fpv.setRowChecker(rowChecker);
    fpv.process("mystepname", validationContext, fs);

    verify(fileChecker, times(1)).checkFile(anyString());
    verify(moreChecker, never()).performSelfCheck(anyString());
    verify(rowChecker, never()).checkFile(anyString());
  }

  @Test
  public void sanityNotValidFileLevelNotFailFast() throws IOException {
    FileChecker fileChecker = mock(FileChecker.class);
    when(fileChecker.isValid()).thenReturn(false);
    when(fileChecker.isFailFast()).thenReturn(false);
    when(fileChecker.canContinue()).thenReturn(true);

    RowChecker rowChecker = mock(RowChecker.class);
    when(rowChecker.isValid()).thenReturn(true);

    val fpv = new FPVSubmissionProcessor();
    fpv.setFileChecker(fileChecker);
    fpv.setRowChecker(rowChecker);
    fpv.process("mystepname", validationContext, fs);

    verify(fileChecker, times(1)).checkFile(anyString());
    verify(rowChecker, times(1)).checkFile(anyString());
  }

  @Test
  public void sanityNotValidRowLevel() throws IOException {
    FileChecker fileChecker = mock(FileChecker.class);
    when(fileChecker.isValid()).thenReturn(true);
    when(fileChecker.isFailFast()).thenReturn(false);
    when(fileChecker.canContinue()).thenReturn(true);

    RowChecker rowChecker = mock(RowChecker.class);
    when(rowChecker.isValid()).thenReturn(false);

    val fpv = new FPVSubmissionProcessor();
    fpv.setFileChecker(fileChecker);
    fpv.setRowChecker(rowChecker);
    fpv.process("mystepname", validationContext, fs);

    verify(fileChecker, times(1)).checkFile(anyString());
    verify(rowChecker, times(1)).checkFile(anyString());
  }

  private static class DummyFileCheckerUnderTest extends CompositeFileChecker {

    public DummyFileCheckerUnderTest(FileChecker nestedChecker) {
      super(nestedChecker);
    }

    public DummyFileCheckerUnderTest(FileChecker nestedChecker, boolean failsafe) {
      super(nestedChecker, failsafe);
    }

    @Override
    public void performSelfCheck(String filename) {

    }

  }

  @SuppressWarnings("unchecked")
  private static Iterable<DataType> anyDataTypeIterable() {
    return any(Iterable.class);
  }

}