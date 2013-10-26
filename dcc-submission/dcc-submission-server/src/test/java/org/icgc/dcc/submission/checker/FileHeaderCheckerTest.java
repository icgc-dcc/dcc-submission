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
package org.icgc.dcc.submission.checker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import lombok.val;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Util.class)
public class FileHeaderCheckerTest {

  private SubmissionDirectory submissionDir;
  private Dictionary dict;
  private DccFileSystem fs;

  @Before
  public void setup() {
    submissionDir = mock(SubmissionDirectory.class);
    dict = mock(Dictionary.class);
    fs = mock(DccFileSystem.class);
    when(submissionDir.listFile()).thenReturn(ImmutableList.of("testfile1", "testfile2"));

    FileSchema fileSchema = mock(FileSchema.class);
    Optional<FileSchema> option = Optional.of(fileSchema);
    when(fileSchema.getFieldNames()).thenReturn(ImmutableList.of("a", "b"));
    when(dict.fileSchema(anyString())).thenReturn(option);
  }

  @Test
  public void simpleValidation() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("a\tb\rf1\t\f2\r".getBytes()));
    PowerMockito.mockStatic(Util.class);
    when(Util.createInputStream(any(DccFileSystem.class), anyString())).thenReturn(fis);

    FileChecker checker = new FileHeaderChecker(new BaseFileChecker(dict, submissionDir), fs);
    val error = checker.check(anyString());
    assertTrue(error.isEmpty());
    assertTrue(checker.isValid());

  }

  @Test
  public void notValidMissingHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("a\tr".getBytes()));
    PowerMockito.mockStatic(Util.class);
    when(Util.createInputStream(any(DccFileSystem.class), anyString())).thenReturn(fis);

    FileChecker checker = new FileHeaderChecker(new BaseFileChecker(dict, submissionDir), fs);
    val error = checker.check(anyString());
    assertFalse(error.isEmpty());
    assertEquals(1, error.size());
    assertFalse(checker.isValid());

  }

  @Test
  public void notValidOutOfOrderHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("b\ta\rf1\t\f2\r".getBytes()));
    PowerMockito.mockStatic(Util.class);
    when(Util.createInputStream(any(DccFileSystem.class), anyString())).thenReturn(fis);

    FileChecker checker = new FileHeaderChecker(new BaseFileChecker(dict, submissionDir), fs);
    val error = checker.check(anyString());
    assertFalse(error.isEmpty());
    assertEquals(1, error.size());
    assertFalse(checker.isValid());
  }

  @Test
  public void notValidDuplicateHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("b\tb\rf1\t\f2\r".getBytes()));
    PowerMockito.mockStatic(Util.class);
    when(Util.createInputStream(any(DccFileSystem.class), anyString())).thenReturn(fis);

    FileChecker checker = new FileHeaderChecker(new BaseFileChecker(dict, submissionDir), fs);
    val error = checker.check(anyString());
    assertFalse(error.isEmpty());
    assertEquals(1, error.size());
    assertFalse(checker.isValid());
  }

  @Test
  public void notValidExtraHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("a\tb\tc\rf1\t\f2\r".getBytes()));
    PowerMockito.mockStatic(Util.class);
    when(Util.createInputStream(any(DccFileSystem.class), anyString())).thenReturn(fis);

    FileChecker checker = new FileHeaderChecker(new BaseFileChecker(dict, submissionDir), fs);
    val error = checker.check(anyString());
    assertFalse(error.isEmpty());
    assertEquals(1, error.size());
    assertFalse(checker.isValid());

  }

  @Test
  public void notValidWhiteSpacesInHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream(" a \tb\rf1\t\f2\r".getBytes()));
    PowerMockito.mockStatic(Util.class);
    when(Util.createInputStream(any(DccFileSystem.class), anyString())).thenReturn(fis);

    FileChecker checker = new FileHeaderChecker(new BaseFileChecker(dict, submissionDir), fs);
    val error = checker.check(anyString());
    assertFalse(error.isEmpty());
    assertEquals(1, error.size());
    assertFalse(checker.isValid());

  }

  @Test
  public void notValidCapitalHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("A\tB\rf1\t\f2\r".getBytes()));
    PowerMockito.mockStatic(Util.class);
    when(Util.createInputStream(any(DccFileSystem.class), anyString())).thenReturn(fis);

    FileChecker checker = new FileHeaderChecker(new BaseFileChecker(dict, submissionDir), fs);
    val error = checker.check(anyString());
    assertFalse(error.isEmpty());
    assertEquals(1, error.size());
    assertFalse(checker.isValid());

  }

  @Test
  public void notValidMispellHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("ab\tb\rf1\t\f2\r".getBytes()));
    PowerMockito.mockStatic(Util.class);
    when(Util.createInputStream(any(DccFileSystem.class), anyString())).thenReturn(fis);

    FileChecker checker = new FileHeaderChecker(new BaseFileChecker(dict, submissionDir), fs);
    val error = checker.check(anyString());
    assertFalse(error.isEmpty());
    assertEquals(1, error.size());
    assertFalse(checker.isValid());

  }

  @Test
  public void validLineFeedNewLineHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("a\tb\r\nf1\t\f2\r\n".getBytes()));
    PowerMockito.mockStatic(Util.class);
    when(Util.createInputStream(any(DccFileSystem.class), anyString())).thenReturn(fis);

    FileChecker checker = new FileHeaderChecker(new BaseFileChecker(dict, submissionDir), fs);
    val error = checker.check(anyString());
    assertTrue(error.isEmpty());
    assertTrue(checker.isValid());
  }

  @Test
  public void validNewLineHeader() throws Exception {
    DataInputStream fis = new DataInputStream(new ByteArrayInputStream("a\tb\nf1\t\f2\n".getBytes()));
    PowerMockito.mockStatic(Util.class);
    when(Util.createInputStream(any(DccFileSystem.class), anyString())).thenReturn(fis);

    FileChecker checker = new FileHeaderChecker(new BaseFileChecker(dict, submissionDir), fs);
    val error = checker.check(anyString());
    assertTrue(error.isEmpty());
    assertTrue(checker.isValid());
  }

}
