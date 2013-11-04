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
package org.icgc.dcc.submission.validation.checker;

import static com.google.common.collect.Lists.newArrayList;
import static junit.framework.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.validation.cascading.TupleState.TupleError;
import org.icgc.dcc.submission.validation.checker.FileChecker.FileCheckers;
import org.icgc.dcc.submission.validation.checker.RowChecker.RowCheckers;
import org.icgc.dcc.submission.validation.checker.step.CompositeFileChecker;
import org.icgc.dcc.submission.validation.core.ErrorType;
import org.icgc.dcc.submission.validation.service.ValidationContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableList;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Util.class)
public class FirstPassValidatorTest {

  private SubmissionDirectory submissionDir;
  private Dictionary dict;
  private FirstPassValidator fpc;

  @Mock
  ValidationContext validationContext;

  @Before
  public void setup() throws IOException {
    submissionDir = mock(SubmissionDirectory.class);
    dict = mock(Dictionary.class);
    mockStatic(Util.class);

    when(submissionDir.listFile()).thenReturn(ImmutableList.of("anyfile"));
    when(submissionDir.getDataFilePath(anyString())).thenReturn("/tmp/anyfile");

    FileSchema schema = new FileSchema("anyfile");
    schema.setPattern("anyfile");
    when(dict.getFiles()).thenReturn(newArrayList(schema));

    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("JUST-A-TEST".getBytes()));
    when(Util.createInputStream(any(DccFileSystem.class), anyString())).thenReturn(fis);

    when(validationContext.getSubmissionDirectory()).thenReturn(submissionDir);
    when(validationContext.getDictionary()).thenReturn(dict);

    fpc = new FirstPassValidator();
  }

  @Test
  public void sanityValid() throws IOException {
    FileChecker fileChecker = mock(FileChecker.class);
    when(fileChecker.canContinue()).thenReturn(true);
    when(fileChecker.isValid()).thenReturn(true);

    RowChecker rowChecker = mock(RowChecker.class);
    when(rowChecker.canContinue()).thenReturn(true);
    when(rowChecker.isValid()).thenReturn(true);

    mockStatic(FileCheckers.class);
    when(FileCheckers.getDefaultFileChecker(validationContext)).thenReturn(fileChecker);
    mockStatic(RowCheckers.class);
    when(RowCheckers.getDefaultRowChecker(validationContext)).thenReturn(rowChecker);

    fpc.validate(validationContext);

    checkNoErrorsReported(validationContext);
    verify(fileChecker, times(1)).check(anyString());
    verify(rowChecker, times(1)).check(anyString());
  }

  @Test
  public void sanityNotValid() throws Exception {
    FileChecker fileChecker = mock(FileChecker.class);
    when(fileChecker.check(anyString())).thenReturn(ImmutableList.of(DUMMY_FILE_ERROR));
    when(fileChecker.isValid()).thenReturn(false);
    when(fileChecker.isFailFast()).thenReturn(false);
    when(fileChecker.canContinue()).thenReturn(true);

    RowChecker rowChecker = mock(RowChecker.class);
    when(rowChecker.check(anyString())).thenReturn(ImmutableList.<FirstPassValidationError> of());
    when(rowChecker.isValid()).thenReturn(false);
    when(rowChecker.isFailFast()).thenReturn(false);
    when(rowChecker.canContinue()).thenReturn(true);

    FirstPassValidator fpc = new FirstPassValidator(dict, submissionDir, fileChecker, rowChecker);
    assertFalse(fpc.isValid());

    verify(fileChecker, times(1)).check(anyString());
    verify(rowChecker, times(1)).check(anyString());
  }

  @Test
  public void sanityNotValidFileLevelFailFast() throws IOException {
    FileChecker fileChecker = mock(FileChecker.class);
    when(fileChecker.isValid()).thenReturn(false);
    when(fileChecker.check(anyString())).thenReturn(ImmutableList.of(DUMMY_FILE_ERROR));
    when(fileChecker.isFailFast()).thenReturn(true); // fail it right away

    CompositeFileChecker moreChecker =
        PowerMockito.spy(new DummyFileCheckerUnderTest(new DummyFileCheckerUnderTest(fileChecker, false), false));

    RowChecker rowChecker = mock(RowChecker.class);
    when(rowChecker.check(anyString())).thenReturn(ImmutableList.<FirstPassValidationError> of());
    when(rowChecker.isValid()).thenReturn(true);

    FirstPassValidator fpc = new FirstPassValidator(dict, submissionDir, moreChecker, rowChecker);
    assertFalse(fpc.isValid());

    verify(fileChecker, times(1)).check(anyString());
    verify(moreChecker, times(0)).performSelfCheck(anyString());
    verify(rowChecker, times(0)).check(anyString());
  }

  @Test
  public void sanityNotValidFileLevelNotFailFast() throws IOException {
    FileChecker fileChecker = mock(FileChecker.class);
    when(fileChecker.isValid()).thenReturn(false);
    when(fileChecker.check(anyString())).thenReturn(ImmutableList.of(DUMMY_FILE_ERROR));
    when(fileChecker.isFailFast()).thenReturn(false);
    when(fileChecker.canContinue()).thenReturn(true);

    RowChecker rowChecker = mock(RowChecker.class);
    when(rowChecker.check(anyString())).thenReturn(ImmutableList.<FirstPassValidationError> of());
    when(rowChecker.isValid()).thenReturn(true);

    FirstPassValidator fpc = new FirstPassValidator(dict, submissionDir, fileChecker, rowChecker);
    assertFalse(fpc.isValid());

    verify(fileChecker, times(1)).check(anyString());
    verify(rowChecker, times(1)).check(anyString());
  }

  @Test
  public void sanityNotValidRowLevel() throws IOException {
    FileChecker fileChecker = mock(FileChecker.class);
    when(fileChecker.isValid()).thenReturn(true);
    when(fileChecker.check(anyString())).thenReturn(ImmutableList.<FirstPassValidationError> of());
    when(fileChecker.isFailFast()).thenReturn(false);
    when(fileChecker.canContinue()).thenReturn(true);

    RowChecker rowChecker = mock(RowChecker.class);
    when(rowChecker.check(anyString())).thenReturn(ImmutableList.of(DUMMY_ROW_ERROR));
    when(rowChecker.isValid()).thenReturn(false);

    FirstPassValidator fpc = new FirstPassValidator(dict, submissionDir, fileChecker, rowChecker);
    assertFalse(fpc.isValid());

    verify(fileChecker, times(1)).check(anyString());
    verify(rowChecker, times(1)).check(anyString());
  }

  /**
   * 
   */
  private void checkNoErrorsReported(ValidationContext validationContext) {
    verify(validationContext, times(0)).reportError(anyString(), any(ErrorType.class));
    verify(validationContext, times(0)).reportError(anyString(), any(TupleError.class));
    verify(validationContext, times(0)).reportError(anyString(), any(ErrorType.class), any());
    verify(validationContext, times(0)).reportError(anyString(), any(), any(ErrorType.class));
    verify(validationContext, times(0)).reportError(anyString(), anyLong(), any(), any(ErrorType.class));
    verify(validationContext, times(0)).reportError(anyString(), anyLong(), anyString(), any(), any(ErrorType.class));
  }

  private static class DummyFileCheckerUnderTest extends CompositeFileChecker {

    public DummyFileCheckerUnderTest(FileChecker nestedChecker) {
      super(nestedChecker);
    }

    public DummyFileCheckerUnderTest(FileChecker nestedChecker, boolean failsafe) {
      super(nestedChecker, failsafe);
    }

    @Override
    public List<FirstPassValidationError> performSelfCheck(String filename) {
      return ImmutableList.of();
    }

  }

}