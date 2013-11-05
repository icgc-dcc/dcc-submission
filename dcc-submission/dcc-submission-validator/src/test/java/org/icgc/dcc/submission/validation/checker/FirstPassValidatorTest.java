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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.validation.checker.step.CompositeFileChecker;
import org.icgc.dcc.submission.validation.checker.step.TestUtils;
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
  }

  @Test
  public void sanityValid() throws IOException {
    FileChecker dummyFileChecker = mock(FileChecker.class);
    when(dummyFileChecker.canContinue()).thenReturn(true);
    when(dummyFileChecker.isValid()).thenReturn(true);

    RowChecker dummyRowChecker = mock(RowChecker.class);
    when(dummyRowChecker.canContinue()).thenReturn(true);
    when(dummyRowChecker.isValid()).thenReturn(true);

    FirstPassValidator fpc = new FirstPassValidator(dummyFileChecker, dummyRowChecker);
    fpc.validate(validationContext);

    TestUtils.checkNoErrorsReported(validationContext);
    verify(dummyFileChecker, times(1)).check(anyString());
    verify(dummyRowChecker, times(1)).check(anyString());
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

    new FirstPassValidator(fileChecker, rowChecker)
        .validate(validationContext);

    verify(fileChecker, times(1)).check(anyString());
    verify(rowChecker, times(1)).check(anyString());
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

    new FirstPassValidator(fileChecker, rowChecker)
        .validate(validationContext);

    verify(fileChecker, times(1)).check(anyString());
    verify(moreChecker, never()).performSelfCheck(anyString());
    verify(rowChecker, never()).check(anyString());
  }

  @Test
  public void sanityNotValidFileLevelNotFailFast() throws IOException {
    FileChecker fileChecker = mock(FileChecker.class);
    when(fileChecker.isValid()).thenReturn(false);
    when(fileChecker.isFailFast()).thenReturn(false);
    when(fileChecker.canContinue()).thenReturn(true);

    RowChecker rowChecker = mock(RowChecker.class);
    when(rowChecker.isValid()).thenReturn(true);

    new FirstPassValidator(fileChecker, rowChecker)
        .validate(validationContext);

    verify(fileChecker, times(1)).check(anyString());
    verify(rowChecker, times(1)).check(anyString());
  }

  @Test
  public void sanityNotValidRowLevel() throws IOException {
    FileChecker fileChecker = mock(FileChecker.class);
    when(fileChecker.isValid()).thenReturn(true);
    when(fileChecker.isFailFast()).thenReturn(false);
    when(fileChecker.canContinue()).thenReturn(true);

    RowChecker rowChecker = mock(RowChecker.class);
    when(rowChecker.isValid()).thenReturn(false);

    new FirstPassValidator(fileChecker, rowChecker)
        .validate(validationContext);

    verify(fileChecker, times(1)).check(anyString());
    verify(rowChecker, times(1)).check(anyString());
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

}