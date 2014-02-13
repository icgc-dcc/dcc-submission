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

import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.submission.dictionary.model.SummaryType.AVERAGE;
import static org.icgc.dcc.submission.dictionary.model.ValueType.INTEGER;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.IOException;

import lombok.Cleanup;
import lombok.val;

import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.FileSchemaRole;
import org.icgc.dcc.submission.dictionary.model.Relation;
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
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class FPVSubmissionProcessorClientTest {

  private final static String VALID_CONTENT = "H1\tH2\tH3\nf1\tf2\tf3";

  enum Schema {
    TESTFILE3("testfile3.txt"), TESTFILE2("testfile2.gz", TESTFILE3, false), TESTFILE1("testfile1.bz2", TESTFILE2, true);

    private final FileSchema fileSchema;

    Schema(String schemaName, Schema referencedSchema, boolean isBidirectional) {
      fileSchema = new FileSchema(schemaName);
      fileSchema.setPattern(schemaName);
      fileSchema.setRole(FileSchemaRole.SUBMISSION);
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
      fileSchema.setRole(FileSchemaRole.SUBMISSION);
      fileSchema.setPattern(schemaName);
      fileSchema.addField(getTestField("H1"));
      fileSchema.addField(getTestField("H2"));
      fileSchema.addField(getTestField("H3"));
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
  public void setup() throws IOException {
    val schema1 = Schema.TESTFILE1.getSchema();
    val schema2 = Schema.TESTFILE2.getSchema();
    val schema3 = Schema.TESTFILE3.getSchema();

    dict.addFile(schema1);
    dict.addFile(schema2);
    dict.addFile(schema3);
    doReturn(Optional.of(schema1)).when(dict).getFileSchemaByFileName(schema1.getName());
    doReturn(Optional.of(schema2)).when(dict).getFileSchemaByFileName(schema2.getName());
    doReturn(Optional.of(schema3)).when(dict).getFileSchemaByFileName(schema3.getName());
    doReturn(ImmutableList.<FileSchema> of(schema1, schema2, schema3))
        .when(dict).getFileSchemata(anyDataTypeIterable());

    ImmutableList<String> files = ImmutableList.of(schema1.getName(), schema2.getName(), schema3.getName());
    when(fs.listMatchingSubmissionFiles(Mockito.anyListOf(String.class))).thenReturn(files);

    when(fs.getMatchingFileNames(schema1.getName())).thenReturn(newArrayList(schema1.getName()));
    when(fs.getMatchingFileNames(schema2.getName())).thenReturn(newArrayList(schema2.getName()));
    when(fs.getMatchingFileNames(schema3.getName())).thenReturn(newArrayList(schema3.getName()));

    // We must use a trick here, the 3rd call returns plain text because mocking the correct CompressionCodec in
    // FPVFileSystem#getCompressionCodec() is a bit difficult. What needs to happen is that Util (where
    // createInputStream() lives) needs to be merged with FPVSubmissionProcessor. Until then, we'll have to use this
    // trick.

    @Cleanup
    val plainStream1 = FileCorruptionCheckerTest.getTestInputStream(VALID_CONTENT, CodecType.PLAIN_TEXT);
    @Cleanup
    val plainStream2 = FileCorruptionCheckerTest.getTestInputStream(VALID_CONTENT, CodecType.PLAIN_TEXT);
    @Cleanup
    val plainStream3 = FileCorruptionCheckerTest.getTestInputStream(VALID_CONTENT, CodecType.PLAIN_TEXT);
    @Cleanup
    val plainStream4 = FileCorruptionCheckerTest.getTestInputStream(VALID_CONTENT, CodecType.PLAIN_TEXT);
    @Cleanup
    val plainStream5 = FileCorruptionCheckerTest.getTestInputStream(VALID_CONTENT, CodecType.PLAIN_TEXT);
    when(fs.getCompressionInputStream(schema3.getName()))
        .thenReturn(plainStream1)
        .thenReturn(plainStream2)
        .thenReturn(plainStream3); // See comment above

    @Cleanup
    val bzip2Stream1 = FileCorruptionCheckerTest.getTestInputStream(VALID_CONTENT, CodecType.BZIP2);
    @Cleanup
    val bzip2Stream2 = FileCorruptionCheckerTest.getTestInputStream(VALID_CONTENT, CodecType.BZIP2);
    when(fs.getCompressionInputStream(schema1.getName()))
        .thenReturn(bzip2Stream1)
        .thenReturn(bzip2Stream2)
        .thenReturn(plainStream4); // See comment above

    @Cleanup
    val gzipStream1 = FileCorruptionCheckerTest.getTestInputStream(VALID_CONTENT, CodecType.GZIP);
    @Cleanup
    val gzipStream2 = FileCorruptionCheckerTest.getTestInputStream(VALID_CONTENT, CodecType.GZIP);
    when(fs.getCompressionInputStream(schema2.getName()))
        .thenReturn(gzipStream1)
        .thenReturn(gzipStream2)
        .thenReturn(plainStream5); // See comment above

    when(validationContext.getDictionary()).thenReturn(dict);
  }

  @Test
  public void sanity() throws IOException {
    val fpv = new FPVSubmissionProcessor();
    fpv.process("mystepname", validationContext, fs);
    TestUtils.checkNoErrorsReported(validationContext);
  }

  private static Iterable<DataType> anyDataTypeIterable() {
    return any();
  }

}
