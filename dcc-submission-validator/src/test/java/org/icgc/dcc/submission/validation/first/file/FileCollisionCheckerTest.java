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

import static org.icgc.dcc.submission.validation.first.row.TestUtils.checkFileCollisionErrorReported;
import static org.icgc.dcc.submission.validation.first.row.TestUtils.checkNoErrorsReported;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import lombok.val;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;
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
public class FileCollisionCheckerTest {

  /**
   * Class under test.
   */
  FileCollisionChecker checker;

  /**
   * Collaborators.
   */
  @Mock
  DccFileSystem dccFileSystem;
  @Mock
  Dictionary dictionary;
  @Mock
  FileSchema fileSchema;
  @Mock
  ValidationContext context;
  @Mock
  FPVFileSystem fs;

  @Before
  public void setup() {
    val pattern = "testfile1";
    when(fileSchema.getPattern()).thenReturn(pattern);
    when(dictionary.getFileSchemaByName(anyString())).thenReturn(Optional.of(fileSchema));
    when(dictionary.getFileSchemaByFileName(anyString())).thenReturn(Optional.of(fileSchema));

    when(context.getDccFileSystem()).thenReturn(dccFileSystem);
    when(context.getDictionary()).thenReturn(dictionary);

    checker = new FileCollisionChecker(new NoOpFileChecker(context, fs));
  }

  @Test
  public void matchNone() throws Exception {
    when(fs.getMatchingFileNames(anyString()))
        .thenReturn(fileNames());

    checker.checkFile("testfile1");

    checkNoErrorsReported(context);
    assertTrue(checker.isValid());
  }

  @Test
  public void matchOne() throws Exception {
    when(fs.getMatchingFileNames(anyString()))
        .thenReturn(fileNames("testfile1"));

    checker.checkFile("testfile1");

    checkNoErrorsReported(context);
    assertTrue(checker.isValid());

  }

  @Test
  public void matchTwo_coexsit() throws Exception {
    when(fs.getMatchingFileNames(anyString()))
        .thenReturn(fileNames("testfile1", "testfile2"));

    checker.checkFile("testfile1");

    checkFileCollisionErrorReported(context, 0);
  }

  @Test
  public void matchTwo_collide() throws Exception {
    when(fs.getMatchingFileNames(anyString()))
        .thenReturn(fileNames("testfile1", "testfile1.gz"));

    checker.checkFile("testfile1");

    checkFileCollisionErrorReported(context, 1);
  }

  private static List<String> fileNames(String... fileNames) {
    return ImmutableList.copyOf(fileNames);
  }

}
