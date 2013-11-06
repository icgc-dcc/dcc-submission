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
package org.icgc.dcc.submission.validation.first;

import static org.icgc.dcc.submission.dictionary.model.SummaryType.AVERAGE;
import static org.icgc.dcc.submission.dictionary.model.ValueType.INTEGER;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import lombok.val;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Relation;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.FsConfig;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.first.Util.CodecType;
import org.icgc.dcc.submission.validation.first.step.FileCorruptionCheckerTest;
import org.icgc.dcc.submission.validation.first.step.TestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;

@RunWith(MockitoJUnitRunner.class)
public class FirstPassValidatorClientTest {

  private final static String VALID_CONTENT = "H1\tH2\tH3\nf1\tf2\tf3";

  enum Schema {
    TESTFILE3("testfile3.txt"), TESTFILE2("testfile2.gz", TESTFILE3, false), TESTFILE1("testfile1.bz2", TESTFILE2, true);

    private final FileSchema fileSchema;

    Schema(String schemaName, Schema referencedSchema, boolean isBidirectional) {
      fileSchema = new FileSchema(schemaName);
      fileSchema.setPattern(schemaName);
      fileSchema.addField(getTestField("H1"));
      fileSchema.addField(getTestField("H2"));
      fileSchema.addField(getTestField("H3"));
      Relation relation =
          new Relation(fileSchema.getFieldNames(), //
              referencedSchema.getSchema().getName(), //
              referencedSchema.getSchema().getFieldNames(), //
              isBidirectional);
      fileSchema.addRelation(relation);
    }

    private Field getTestField(String fieldName) {
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
      fileSchema.addField(getTestField("H1"));
      fileSchema.addField(getTestField("H2"));
      fileSchema.addField(getTestField("H3"));
    }

    public FileSchema getSchema() {
      return fileSchema;
    }
  }

  @Mock
  Config config;

  @Spy
  Dictionary dict;

  DccFileSystem fs;

  @Mock
  SubmissionDirectory submissionDir;

  @Mock
  ValidationContext validationContext;

  @Before
  public void setup() throws IOException {
    final File file1 = File.createTempFile("testfile1", ".bz2");
    final File file2 = File.createTempFile("testfile2", ".gz", file1.getParentFile());
    final File file3 = File.createTempFile("testfile3", ".txt", file1.getParentFile());

    file1.deleteOnExit();
    file2.deleteOnExit();
    file3.deleteOnExit();

    IOUtils.copy(FileCorruptionCheckerTest.getTestInputStream(VALID_CONTENT, CodecType.BZIP2), new FileOutputStream(
        file1));
    IOUtils.copy(FileCorruptionCheckerTest.getTestInputStream(VALID_CONTENT, CodecType.GZIP), new FileOutputStream(
        file2));
    IOUtils.copy(FileCorruptionCheckerTest.getTestInputStream(VALID_CONTENT, CodecType.PLAIN_TEXT),
        new FileOutputStream(
            file3));

    Schema.TESTFILE1.getSchema().setPattern(file1.getName());
    Schema.TESTFILE2.getSchema().setPattern(file2.getName());
    Schema.TESTFILE3.getSchema().setPattern(file3.getName());
    dict.addFile(Schema.TESTFILE1.getSchema());
    dict.addFile(Schema.TESTFILE2.getSchema());
    dict.addFile(Schema.TESTFILE3.getSchema());
    when(dict.getFileSchema(anyString()))
        .thenReturn(Optional.of(Schema.TESTFILE1.getSchema()))
        .thenReturn(Optional.of(Schema.TESTFILE2.getSchema()))
        .thenReturn(Optional.of(Schema.TESTFILE3.getSchema()));

    when(config.getString(FsConfig.FS_ROOT)).thenReturn(file1.getParent());
    fs = new DccFileSystem(config, FileSystem.getLocal(new Configuration()));

    ImmutableList<String> files = ImmutableList.of(file1.getName(), file2.getName(), file3.getName());
    when(submissionDir.listFile()).thenReturn(files);
    when(submissionDir.listFiles(Mockito.anyListOf(String.class))).thenReturn(files);
    when(submissionDir.listFile(any(Pattern.class))).thenAnswer(new Answer<Iterable<String>>() {

      @Override
      public Iterable<String> answer(InvocationOnMock invocation) throws Throwable {
        Pattern pattern = (Pattern) invocation.getArguments()[0];
        return ImmutableList.of(pattern.pattern());
      }
    });
    when(submissionDir.getDataFilePath(anyString())).thenAnswer(new Answer<String>() {

      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        String filename = (String) invocation.getArguments()[0];
        File file = new File(file1.getParentFile(), filename);
        return file.getAbsolutePath();
      }
    });

    when(validationContext.getDccFileSystem()).thenReturn(fs);
    when(validationContext.getSubmissionDirectory()).thenReturn(submissionDir);
    when(validationContext.getDictionary()).thenReturn(dict);
  }

  @Test
  public void sanity() throws IOException {
    FirstPassValidator fpc = new FirstPassValidator();
    fpc.validate(validationContext);
    TestUtils.checkNoErrorsReported(validationContext);
  }

}
