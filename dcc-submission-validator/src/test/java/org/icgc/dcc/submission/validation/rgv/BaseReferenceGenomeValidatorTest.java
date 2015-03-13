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
package org.icgc.dcc.submission.validation.rgv;

import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME_END;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME_START;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_MUTATION_TYPE;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SSM_P_TYPE;
import static org.icgc.dcc.submission.fs.DccFileSystem.VALIDATION_DIRNAME;
import static org.icgc.dcc.submission.validation.ValidationTests.getTestFieldNames;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import lombok.Builder;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.DataType.DataTypes;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.rgv.reference.PicardReferenceGenome;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class BaseReferenceGenomeValidatorTest {

  /**
   * Test data.
   */
  protected static final String TEST_FILE_NAME = "ssm_p.txt";

  /**
   * Scratch space.
   */
  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  /**
   * Class under test.
   */
  protected ReferenceGenomeValidator validator;

  @Before
  public void setup() {
    validator = new ReferenceGenomeValidator(new PicardReferenceGenome("/tmp/GRCh37.fasta"));
  }

  @SneakyThrows
  protected ValidationContext mockContext() {
    // Setup: Use local file system
    val fileSystem = FileSystem.getLocal(new Configuration());

    // Setup: Establish input for the test
    val directory = new Path(tmp.newFolder().getAbsolutePath());
    val path = new Path(directory, "ssm_p.txt");
    val ssmPrimaryFile = path;
    val ssmPrimaryFileSchema = createSsmPrimaryFileSchema();
    val validationDir = new Path(directory, VALIDATION_DIRNAME).toUri().toString();

    // Setup: Mock
    val context = mock(ValidationContext.class);
    val submissionDirectory = mock(SubmissionDirectory.class);
    when(context.getProjectKey()).thenReturn("project.test");
    when(context.getDataTypes()).thenReturn(DataTypes.values());
    when(context.getFileSystem()).thenReturn(fileSystem);
    when(context.getFiles(SSM_P_TYPE)).thenReturn(ImmutableList.<Path> of(ssmPrimaryFile));
    when(context.getFileSchema(SSM_P_TYPE)).thenReturn(ssmPrimaryFileSchema);
    when(context.getSubmissionDirectory()).thenReturn(submissionDirectory);
    when(submissionDirectory.getValidationDirPath()).thenReturn(validationDir);

    // Setup: "Submit" file
    @Cleanup
    val outputStream = fileSystem.create(path);
    outputStream.writeBytes(createSsmPrimaryLines());

    return context;
  }

  private FileSchema createSsmPrimaryFileSchema() {
    val fileSchema = mock(FileSchema.class);
    when(fileSchema.getFieldNames()).thenReturn(getSsmPrimaryFieldNames());

    return fileSchema;
  }

  private String createSsmPrimaryLines() {
    return createSsmPrimaryLines(
        record().mutationType("1").chromosomeCode("5").start("106706335").end("106706335").referenceAllele("C"),
        record().mutationType("3").chromosomeCode("4").start("114381846").end("114381846").referenceAllele("C"),
        record().mutationType("4").chromosomeCode("12").start("129638975").end("129638975").referenceAllele("G"),
        record().mutationType("2").chromosomeCode("20").start("16016117").end("16016117").referenceAllele("A"),
        record().mutationType("1").chromosomeCode("5").start("106706335").end("106706335").referenceAllele("A"),
        record().mutationType("3").chromosomeCode("4").start("114381846").end("114381846").referenceAllele("T"),
        record().mutationType("4").chromosomeCode("12").start("129638975").end("129638975").referenceAllele("T"),
        record().mutationType("2").chromosomeCode("20").start("16016117").end("16016117").referenceAllele("-"));
  }

  private String createSsmPrimaryLines(SsmPrimaryRecord.SsmPrimaryRecordBuilder... records) {
    val contents = new StringBuilder();

    // Header
    contents.append(Joiner.on('\t').join(getSsmPrimaryFieldNames())).append("\n");
    for (val record : records) {
      // Row
      contents.append(createSsmPrimaryLine(record.build())).append("\n");
    }

    return contents.toString();
  }

  private String createSsmPrimaryLine(SsmPrimaryRecord record) {
    val map = new ImmutableMap.Builder<String, String>();
    for (val fieldName : getSsmPrimaryFieldNames()) {
      if (fieldName.equals(SUBMISSION_OBSERVATION_CHROMOSOME)) {
        map.put(fieldName, record.getChromosomeCode());
        continue;
      }
      if (fieldName.equals(SUBMISSION_OBSERVATION_CHROMOSOME_START)) {
        map.put(fieldName, record.getStart());
        continue;
      }
      if (fieldName.equals(SUBMISSION_OBSERVATION_CHROMOSOME_END)) {
        map.put(fieldName, record.getEnd());
        continue;
      }
      if (fieldName.equals(SUBMISSION_OBSERVATION_MUTATION_TYPE)) {
        map.put(fieldName, record.getMutationType());
        continue;
      }
      if (fieldName.equals(SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE)) {
        map.put(fieldName, record.getReferenceAllele());
        continue;
      }

      map.put(fieldName, "");
    }

    return Joiner.on('\t').join(map.build().values());
  }

  private List<String> getSsmPrimaryFieldNames() {
    return getTestFieldNames(SSM_P_TYPE);
  }

  private SsmPrimaryRecord.SsmPrimaryRecordBuilder record() {
    return SsmPrimaryRecord.builder();
  }

  @Builder
  @Value
  private static class SsmPrimaryRecord {

    String mutationType;
    String chromosomeCode;
    String start;
    String end;
    String referenceAllele;

  }

}
