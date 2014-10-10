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
package org.icgc.dcc.submission.validation.norm;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Files.readLines;
import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SSM_P_TYPE;
import static org.icgc.dcc.common.core.util.Joiners.NEWLINE;
import static org.icgc.dcc.submission.validation.norm.NormalizationValidator.COMPONENT_NAME;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.UUID;

import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.DataType.DataTypes;
import org.icgc.dcc.common.hadoop.cascading.CascadingContext;
import org.icgc.dcc.common.hadoop.fs.DccFileSystem2;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.norm.core.NormalizationReport.NormalizationCounter;
import org.icgc.dcc.submission.validation.norm.core.NormalizationReporter;
import org.icgc.dcc.submission.validation.norm.steps.PrimaryKeyGeneration;
import org.icgc.dcc.submission.validation.norm.steps.PrimaryKeyGenerationTest;
import org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cascading.tap.Tap;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PrimaryKeyGeneration.class })
public class NormalizationValidatorTest {

  private static final CascadingContext ctx = CascadingContext.getLocal();

  private static final String RELEASE_NAME = "dummy_release";
  private static final String PROJECT_NAME = "dummy_project";

  private static final String FILE_NAME = SSM_P_TYPE.getHarmonizedOutputFileName();
  private static final String INPUT = "input";
  private static final String OUTPUT = "output";
  private static final String REFERENCE = "reference";

  private static String EXECUTABLE_SPEC_FILE = getResource(
      format("fixtures/validation/%s/executable_specification.txt", COMPONENT_NAME)).getFile();
  private static final String BASIC_INPUT_FILE =
      getResource(format("fixtures/validation/%s/%s/%s", COMPONENT_NAME, INPUT, FILE_NAME)).getFile();
  private static final String BASIC_REFERENCE_FILE =
      getResource(format("fixtures/validation/%s/%s/%s", COMPONENT_NAME, REFERENCE, FILE_NAME)).getFile();
  private static final String SPEC_DERIVED_INPUT_FILE =
      format("/tmp/submission/%s/%s/%s", COMPONENT_NAME, INPUT, FILE_NAME);
  private static final String SPEC_DERIVED_REFERENCE_FILE =
      format("/tmp/submission/%s/%s/%s", COMPONENT_NAME, REFERENCE, FILE_NAME);
  private static final String OUTPUT_FILE =
      format("/tmp/submission/%s/%s/%s", COMPONENT_NAME, OUTPUT, FILE_NAME);

  public static final String OBSERVATION_ID_DEFAULT_VALUE = "v1";

  private NormalizationValidator normalizationValidator;

  @Mock
  private ValidationContext mockValidationContext;

  @Mock
  private SubmissionDirectory mockSubmissionDirectory;

  @Mock
  private SubmissionPlatformStrategy mockPlatformStrategy;

  @Mock
  private DccFileSystem2 mockDccFileSystem2;

  @Mock
  private Release mockRelease;

  @Mock
  private Dictionary mockDictionary;

  @Mock
  private FileSchema mockFileSchema;

  @Mock
  private Config mockConfig;

  @Before
  public void setUp() {
    when(mockConfig.hasPath(anyString()))
        .thenReturn(true);
    when(mockConfig.getBoolean("mask.enabled"))
        .thenReturn(true);
    when(mockConfig.getNumber("error_threshold"))
        .thenReturn(0.5f); // instead of 10% normally

    when(mockRelease.getName())
        .thenReturn(RELEASE_NAME);
    when(mockFileSchema.getFieldNames())
        .thenReturn(NormalizationTestUtils.getFieldNames(SSM_P_TYPE));
    when(mockDictionary.getFileSchema(SSM_P_TYPE))
        .thenReturn(mockFileSchema);

    when(mockValidationContext.getDictionary())
        .thenReturn(mockDictionary);
    when(mockValidationContext.getSubmissionDirectory())
        .thenReturn(mockSubmissionDirectory);
    when(mockValidationContext.getRelease())
        .thenReturn(mockRelease);
    when(mockValidationContext.getProjectKey())
        .thenReturn(PROJECT_NAME);
    when(mockValidationContext.getDataTypes())
        .thenReturn(DataTypes.values());
    when(mockValidationContext.getPlatformStrategy())
        .thenReturn(mockPlatformStrategy);
    when(mockValidationContext.getOutputDirPath())
        .thenReturn(FILE_NAME);
    when(mockSubmissionDirectory.getSampleToDonorMap(mockDictionary)).thenReturn(
        new ImmutableMap.Builder<String, String>()
            .put("00302", "dr1")
            .put("00312", "dr1")
            .put("00322", "dr2")
            .put("00352", "dr2")
            .put("00372", "dr3")
            .build());

    when(mockPlatformStrategy.getFlowConnector())
        .thenReturn(ctx.getConnectors().getFlowConnector());
  }

  @SneakyThrows
  @Test
  public void test_normalization_basic() {

    mockUUID(true);
    when(mockConfig.getBoolean("duplicates.enabled"))
        .thenReturn(true);

    test(BASIC_INPUT_FILE, BASIC_REFERENCE_FILE);

    // Check internal report
    verify(mockDccFileSystem2, times(1))
        .writeNormalizationReport(
            anyString(),
            anyString(),
            eq(NormalizationReporter.INTERNAL_REPORT_MESSAGE + "\n" +
                "11\t" + NormalizationCounter.TOTAL_START.getInternalReportDisplayName() + "\n" +
                "9\t" + NormalizationCounter.UNIQUE_START.getInternalReportDisplayName() + "\n" +
                "3\t" + NormalizationCounter.MARKED_AS_CONTROLLED.getInternalReportDisplayName() + "\n" +
                "3\t" + NormalizationCounter.MASKED.getInternalReportDisplayName() + "\n" +
                "0\t" + NormalizationCounter.DROPPED.getInternalReportDisplayName() + "\n" +
                "9\t" + NormalizationCounter.UNIQUE_REMAINING.getInternalReportDisplayName() + "\n" +
                "14\t" + NormalizationCounter.TOTAL_END.getInternalReportDisplayName() + "\n" // 10+1-4
            ));
  }

  @SneakyThrows
  @Test
  @Ignore
  public void test_normalization_enforceable_spec() {

    ExecutableSpecConverter.convert(
        EXECUTABLE_SPEC_FILE,
        SPEC_DERIVED_INPUT_FILE, SPEC_DERIVED_REFERENCE_FILE);
    mockUUID(false);
    when(mockConfig.getBoolean("duplicates.enabled"))
        .thenReturn(false);

    test(SPEC_DERIVED_INPUT_FILE, SPEC_DERIVED_REFERENCE_FILE);

  }

  @SneakyThrows
  private void test(String inputFile, String referenceFile) {
    mockInputTap(inputFile);
    mockOutputTap(OUTPUT_FILE);
    when(mockPlatformStrategy.getFlowConnector())
        .thenReturn(ctx.getConnectors().getFlowConnector());
    when(mockValidationContext.getFiles(SSM_P_TYPE))
        .thenReturn(newArrayList(new Path(inputFile)));

    new File(OUTPUT_FILE).delete();
    normalizationValidator = NormalizationValidator.getDefaultInstance(mockDccFileSystem2, mockConfig);
    normalizationValidator.validate(mockValidationContext);

    List<String> outputLines = readLines(new File(OUTPUT_FILE), UTF_8);
    List<String> referenceLines = readLines(
        new File(referenceFile),
        UTF_8);

    // Check data output
    assertThat(NEWLINE.join(outputLines))
        .isEqualTo(NEWLINE.join(referenceLines));
  }

  /**
   * Copied from {@link PrimaryKeyGenerationTest#mockUUID()}, somehow we can't seem to be able to use it directly
   * (probably because of powermock).
   */
  public void mockUUID(boolean incremental) {
    PowerMockito.mockStatic(UUID.class);
    UUID mockUuid = PowerMockito.mock(UUID.class);
    val stub = PowerMockito.when(mockUuid.toString()).thenReturn(OBSERVATION_ID_DEFAULT_VALUE);
    if (incremental) {
      stub.thenReturn("v2")
          .thenReturn("v3")
          .thenReturn("v4")
          .thenReturn("v5")
          .thenReturn("v6")
          .thenReturn("v7")
          .thenReturn("v8")
          .thenReturn("v9")
          .thenReturn("v10");
    }

    PowerMockito.when(UUID.randomUUID())
        .thenReturn(mockUuid);
  }

  // TODO: Shouldn't have to do that
  @SuppressWarnings("unchecked")
  private void mockInputTap(String inputFile) {
    val fileName = new File(inputFile).getName();
    when(mockPlatformStrategy.getNormalizerSourceTap(fileName))
        .thenReturn(getInputTap(inputFile));
  }

  // TODO: Shouldn't have to do that
  @SuppressWarnings("unchecked")
  private void mockOutputTap(String outputFile) {
    when(mockDccFileSystem2.getNormalizationDataOutputTap(anyString()))
        .thenReturn(getOutputTap(outputFile));
  }

  // TODO: Shouldn't have to do that
  @SuppressWarnings("rawtypes")
  private Tap getInputTap(String inputFile) {
    return ctx.getTaps().getNoCompressionTsvWithHeader(inputFile);
  }

  // TODO: Shouldn't have to do that
  @SuppressWarnings("rawtypes")
  private Tap getOutputTap(String outputFile) {
    return ctx.getTaps().getNoCompressionTsvWithHeader(outputFile);
  }

}
