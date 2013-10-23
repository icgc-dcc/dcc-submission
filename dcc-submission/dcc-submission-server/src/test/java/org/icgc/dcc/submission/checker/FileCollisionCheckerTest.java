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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.regex.Pattern;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

/**
 * 
 */
public class FileCollisionCheckerTest {

  private SubmissionDirectory submissionDir;
  private Dictionary dict;

  @Before
  public void setup() {
    submissionDir = mock(SubmissionDirectory.class);
    dict = mock(Dictionary.class);

    FileSchema testSchema = mock(FileSchema.class);
    String paramString = "testfile1";
    when(testSchema.getPattern()).thenReturn(paramString);
    when(dict.fileSchema(anyString())).thenReturn(Optional.of(testSchema));
    when(submissionDir.listFile()).thenReturn(ImmutableList.of("testfile1", "testfile2"));
  }

  @Test
  public void matchNone() throws Exception {
    when(submissionDir.listFile(any(Pattern.class))).thenReturn(ImmutableList.<String> of());

    FileCollisionChecker checker = new FileCollisionChecker(new BaseFileChecker(dict, submissionDir));
    List<FirstPassValidationError> errors = checker.check("testfile1");
    assertTrue(errors.isEmpty());
    assertTrue(checker.isValid());
  }

  @Test
  public void matchOne() throws Exception {
    when(submissionDir.listFile(any(Pattern.class))).thenReturn(ImmutableList.of("testfile1"));

    FileCollisionChecker checker = new FileCollisionChecker(new BaseFileChecker(dict, submissionDir));
    List<FirstPassValidationError> errors = checker.check("testfile1");
    assertTrue(errors.isEmpty());
    assertTrue(checker.isValid());

  }

  @Test
  public void matchTwo() throws Exception {
    when(submissionDir.listFile(any(Pattern.class))).thenReturn(ImmutableList.of("testfile1", "testfile2"));

    FileCollisionChecker checker = new FileCollisionChecker(new BaseFileChecker(dict, submissionDir));
    List<FirstPassValidationError> errors = checker.check("testfile1");
    assertTrue(errors.size() == 1);
    assertFalse(checker.isValid());

  }
}
