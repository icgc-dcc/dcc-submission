/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
import static org.icgc.dcc.common.core.model.ValueType.INTEGER;
import static org.icgc.dcc.submission.dictionary.model.SummaryType.AVERAGE;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.FileSchemaRole;
import org.icgc.dcc.submission.dictionary.model.Relation;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.first.core.FileChecker;
import org.icgc.dcc.submission.validation.first.io.FPVFileSystem;
import org.icgc.dcc.submission.validation.first.row.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import lombok.val;

@RunWith(MockitoJUnitRunner.class)
public class FileReferenceCheckerTest {

  // Build schemata: A<->B->C and D
  enum Schema {
    D("D"), C("C"), B("B", C, false), A("A", B, true);

    private final FileSchema fileSchema;

    Schema(String schemaName, Schema referencedSchema, boolean isBidirectional) {
      fileSchema = new FileSchema(schemaName);
      fileSchema.setPattern(schemaName);
      fileSchema.setRole(FileSchemaRole.SUBMISSION);
      fileSchema.addField(getTestField());
      Relation relation =
          new Relation(fileSchema.getFieldNames(), //
              referencedSchema.getSchema().getName(), //
              referencedSchema.getSchema().getFieldNames(), //
              isBidirectional);
      fileSchema.addRelation(relation);
    }

    private Field getTestField() {
      String fieldName = "testField";
      val field = new Field();
      field.setName(fieldName);
      field.setValueType(INTEGER);
      field.setLabel("Test field");
      field.setSummaryType(AVERAGE);
      return field;
    }

    Schema(String schemaName) {
      fileSchema = new FileSchema(schemaName);
      fileSchema.setPattern(schemaName);
      fileSchema.setRole(FileSchemaRole.SUBMISSION);
      fileSchema.addField(getTestField());
    }

    public FileSchema getSchema() {
      return fileSchema;
    }
  }

  @Spy
  Dictionary dict;

  @Mock
  ValidationContext validationContext;
  @Mock
  FPVFileSystem fs;

  @Before
  public void setup() {
    dict.addFile(Schema.A.getSchema());
    dict.addFile(Schema.B.getSchema());
    dict.addFile(Schema.C.getSchema());

    when(validationContext.getDictionary()).thenReturn(dict);
  }

  @Test
  public void validReferentialCheckSchemaB() throws Exception {
    FileChecker baseChecker = spy(new FileNoOpChecker(validationContext, fs));
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(Optional.<FileSchema> of(Schema.B.getSchema()));
    FileReferenceChecker checker = new FileReferenceChecker(baseChecker);
    // regardless of the listfile, the file exists
    when(fs.getMatchingFileNames(anyString())).thenReturn(newArrayList(anyString()));

    checker.checkFile("testfile1");
    verify(fs, times(2)).getMatchingFileNames(anyString());
    TestUtils.checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void invalidReferentialCheckSchemaB() throws Exception {
    FileChecker baseChecker = spy(new FileNoOpChecker(validationContext, fs));
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(Optional.<FileSchema> of(Schema.B.getSchema()));
    FileReferenceChecker checker = new FileReferenceChecker(baseChecker);
    // no referencing and referenced file exists
    when(fs.getMatchingFileNames(anyString())).thenReturn(Lists.<String> newArrayList());
    checker.checkFile("testfile1");
    verify(fs, times(2)).getMatchingFileNames(anyString());
    TestUtils.checkReferentialErrorReported(validationContext, 2);
  }

  @Test
  public void invalidReferencedCheckSchemaB() throws Exception {
    FileChecker baseChecker = spy(new FileNoOpChecker(validationContext, fs));
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(Optional.<FileSchema> of(Schema.B.getSchema()));
    FileReferenceChecker checker = new FileReferenceChecker(baseChecker);

    // no referenced file
    when(fs.getMatchingFileNames(anyString())).thenAnswer(new NoSchemaFound("C"));
    checker.checkFile("testfile1");
    verify(fs, times(2)).getMatchingFileNames(anyString());
    TestUtils.checkReferentialErrorReported(validationContext, 1);
  }

  @Test
  public void invalidReferencingCheckSchemaB() throws Exception {
    FileChecker baseChecker = spy(new FileNoOpChecker(validationContext, fs));
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(Optional.<FileSchema> of(Schema.B.getSchema()));
    FileReferenceChecker checker = new FileReferenceChecker(baseChecker);

    // no referenced file
    when(fs.getMatchingFileNames(anyString())).thenAnswer(new NoSchemaFound("A"));
    checker.checkFile("testfile1");
    verify(fs, times(2)).getMatchingFileNames(anyString());
    TestUtils.checkReferentialErrorReported(validationContext, 1);
  }

  @Test
  public void validReferentialCheckSchemaA() throws Exception {
    FileChecker baseChecker = spy(new FileNoOpChecker(validationContext, fs));
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(Optional.<FileSchema> of(Schema.A.getSchema()));
    FileReferenceChecker checker = new FileReferenceChecker(baseChecker);
    // regardless of the listfile, the file exists
    when(fs.getMatchingFileNames(anyString())).thenReturn(newArrayList(anyString()));
    checker.checkFile("testfile1");
    verify(fs, times(1)).getMatchingFileNames(anyString());
    TestUtils.checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void invalidReferentialCheckSchemaA() throws Exception {
    FileChecker baseChecker = spy(new FileNoOpChecker(validationContext, fs));
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(Optional.<FileSchema> of(Schema.A.getSchema()));
    FileReferenceChecker checker = new FileReferenceChecker(baseChecker);
    when(fs.getMatchingFileNames(anyString())).thenAnswer(new NoSchemaFound("B"));
    checker.checkFile("testfile1");
    verify(fs, times(1)).getMatchingFileNames(anyString());
    TestUtils.checkReferentialErrorReported(validationContext, 1);
  }

  @Test
  public void validReferentialCheckSchemaANoC() throws Exception {
    FileChecker baseChecker = spy(new FileNoOpChecker(validationContext, fs));
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(Optional.<FileSchema> of(Schema.A.getSchema()));
    FileReferenceChecker checker = new FileReferenceChecker(baseChecker);
    when(fs.getMatchingFileNames(anyString())).thenAnswer(new NoSchemaFound("C"));
    checker.checkFile("testfile1");
    verify(fs, times(1)).getMatchingFileNames(anyString());
    TestUtils.checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void validReferentialCheckSchemaC() throws Exception {
    FileChecker baseChecker = spy(new FileNoOpChecker(validationContext, fs));
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(Optional.<FileSchema> of(Schema.C.getSchema()));
    FileReferenceChecker checker = new FileReferenceChecker(baseChecker);
    when(fs.getMatchingFileNames(anyString())).thenReturn(newArrayList(anyString()));
    checker.checkFile("testfile1");
    verify(fs, times(0)).getMatchingFileNames(anyString());
    TestUtils.checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void validReferentialCheckSchemaCNoB() throws Exception {
    FileChecker baseChecker = spy(new FileNoOpChecker(validationContext, fs));
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(Optional.<FileSchema> of(Schema.C.getSchema()));
    FileReferenceChecker checker = new FileReferenceChecker(baseChecker);
    when(fs.getMatchingFileNames(anyString())).thenAnswer(new NoSchemaFound("B"));
    checker.checkFile("testfile1");
    verify(fs, times(0)).getMatchingFileNames(anyString());
    TestUtils.checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void validReferentialCheckSchemaD() throws Exception {
    FileChecker baseChecker = spy(new FileNoOpChecker(validationContext, fs));
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(Optional.<FileSchema> of(Schema.C.getSchema()));
    FileReferenceChecker checker = new FileReferenceChecker(baseChecker);
    when(fs.getMatchingFileNames(anyString())).thenReturn(newArrayList(anyString()));
    checker.checkFile("testfile1");
    verify(fs, times(0)).getMatchingFileNames(anyString());
    TestUtils.checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  private static class NoSchemaFound implements Answer<Iterable<String>> {

    private final String schemaName;

    public NoSchemaFound(String schemaName) {
      this.schemaName = schemaName;
    }

    @Override
    public Iterable<String> answer(InvocationOnMock invocation) throws Throwable {
      String pattern = (String) invocation.getArguments()[0];
      if (pattern.equals(schemaName)) return ImmutableList.of();
      return ImmutableList.of(schemaName);
    }

  }

}
