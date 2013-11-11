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
package org.icgc.dcc.submission.normalization;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Files.readLines;
import static java.lang.String.format;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.icgc.dcc.submission.normalization.NormalizationValidator.COMPONENT_NAME;
import static org.icgc.dcc.submission.validation.platform.PlatformStrategy.FIELD_SEPARATOR;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.UUID;

import lombok.SneakyThrows;

import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.hadoop.fs.DccFileSystem2;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter;
import org.icgc.dcc.submission.normalization.steps.PrimaryKeyGeneration;
import org.icgc.dcc.submission.normalization.steps.PrimaryKeyGenerationTest;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cascading.flow.local.LocalFlowConnector;
import cascading.scheme.local.TextDelimited;
import cascading.tap.Tap;
import cascading.tap.local.FileTap;

import com.google.common.base.Optional;
import com.google.common.io.Resources;
import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PrimaryKeyGeneration.class })
public class NormalizationValidatorTest {

  private static final String RELEASE_NAME = "dummy_release";
  private static final String PROJECT_NAME = "dummy_project";

  private static final String INPUT_FILE_NAME = "input.tsv";
  private static final String OUTPUT_FILE_NAME = "output.tsv";
  private static final String REFERENCE_FILE_NAME = "reference.tsv";

  private static final String INPUT_FILE =
      Resources.getResource(format("fixtures/validation/%s/%s", COMPONENT_NAME, INPUT_FILE_NAME)).getFile();
  private static final String REFERENCE_FILE =
      Resources.getResource(format("fixtures/validation/%s/%s", COMPONENT_NAME, REFERENCE_FILE_NAME)).getFile();
  private static final String OUTPUT_FILE = format("/tmp/dcc_root_dir/%s/%s", COMPONENT_NAME, OUTPUT_FILE_NAME);

  private NormalizationValidator normalizationValidator;

  @Mock
  private ValidationContext mockValidationContext;

  @Mock
  private SubmissionDirectory mockSubmissionDirectory;

  @Mock
  private PlatformStrategy mockPlatformStrategy;

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
    when(mockConfig.hasPath(Mockito.anyString()))
        .thenReturn(true);
    when(mockConfig.getBoolean("masking.enabled"))
        .thenReturn(true);
    when(mockConfig.getBoolean("masking.marking_only"))
        .thenReturn(false);
    when(mockConfig.getNumber("masking.error_threshold"))
        .thenReturn(0.5f); // instead of 10% normally
    when(mockConfig.getBoolean("duplicates.enabled"))
        .thenReturn(true);

    when(mockRelease.getName())
        .thenReturn(RELEASE_NAME);
    when(mockFileSchema.getFieldNames())
        .thenReturn(
            newArrayList(
                "analysis_id", "analyzed_sample_id", "chromosome", "chromosome_end", "chromosome_start",
                "chromosome_strand", "control_genotype", "db_xref", "expressed_allele", "is_annotated", "mutation",
                "mutation_id", "mutation_type", "note", "probability", "quality_score", "read_count",
                "reference_genome_allele", "refsnp_allele", "refsnp_strand", "tumour_genotype", "uri",
                "verification_platform", "verification_status", "xref_ensembl_var_id")
        );
    when(mockDictionary.getFileSchema(SubmissionFileType.SSM_P_TYPE))
        .thenReturn(
            Optional.<FileSchema> of(mockFileSchema));
    when(mockSubmissionDirectory.getFile(Mockito.anyString()))
        .thenReturn(Optional.<String> of(OUTPUT_FILE_NAME));

    mockInputTap();
    mockOutputTap();
    when(mockDccFileSystem2.getFlowConnector())
        .thenReturn(new LocalFlowConnector());

    when(mockValidationContext.getDictionary())
        .thenReturn(mockDictionary);
    when(mockValidationContext.getSubmissionDirectory())
        .thenReturn(mockSubmissionDirectory);
    when(mockValidationContext.getRelease())
        .thenReturn(mockRelease);
    when(mockValidationContext.getProjectKey())
        .thenReturn(PROJECT_NAME);
    when(mockValidationContext.getPlatformStrategy())
        .thenReturn(mockPlatformStrategy);

    mockUUID();
  }

  @SneakyThrows
  @Test
  public void test_normalize() {
    new File(OUTPUT_FILE).delete();

    normalizationValidator = NormalizationValidator
        .getDefaultInstance(mockDccFileSystem2, mockConfig);
    normalizationValidator.validate(mockValidationContext);

    // Check data output
    assertThat(readLines(new File(OUTPUT_FILE), UTF_8))
        .isEqualTo(
            readLines(new File(REFERENCE_FILE), UTF_8)
        );

    // Check internal report
    Mockito.verify(mockDccFileSystem2, Mockito.times(1))
        .writeNormalizationReport(
            Mockito.anyString(),
            Mockito.anyString(),
            Mockito.eq(
                NormalizationReporter.MESSAGE + "\n" +
                    "4\t" + NormalizationCounter.DROPPED.getDisplayName() + "\n" +
                    "5\t" + NormalizationCounter.UNIQUE_FILTERED.getDisplayName() + "\n"
                ));
  }

  /**
   * Copied from {@link PrimaryKeyGenerationTest#mockUUID()}, somehow we can't seem to be able to use it directly
   * (probably because of powermock).
   */
  public void mockUUID() {
    PowerMockito.mockStatic(UUID.class);
    UUID mockUuid = PowerMockito.mock(UUID.class);
    PowerMockito.when(mockUuid.toString())
        .thenReturn("v1")
        .thenReturn("v2")
        .thenReturn("v3")
        .thenReturn("v4")
        .thenReturn("v5")
        .thenReturn("v6")
        .thenReturn("v7")
        .thenReturn("v8");

    PowerMockito.when(UUID.randomUUID())
        .thenReturn(mockUuid);
  }

  // ===========================================================================

  // TODO: Shouldn't have to do that
  @SuppressWarnings("unchecked")
  private void mockInputTap() {
    when(mockPlatformStrategy.getSourceTap2(mockFileSchema))
        .thenReturn(getInputTap());
  }

  // TODO: Shouldn't have to do that
  @SuppressWarnings("rawtypes")
  private Tap getInputTap() {
    return new FileTap(
        new TextDelimited(
            true, // headers
            FIELD_SEPARATOR),
        INPUT_FILE);
  }

  // TODO: Shouldn't have to do that
  @SuppressWarnings("unchecked")
  private void mockOutputTap() {
    when(mockDccFileSystem2.getNormalizationDataOutputTap(RELEASE_NAME, PROJECT_NAME))
        .thenReturn(getOutputTap());
  }

  // TODO: Shouldn't have to do that
  @SuppressWarnings("rawtypes")
  private Tap getOutputTap() {
    return new FileTap(
        new TextDelimited(
            true, // headers
            FIELD_SEPARATOR),
        OUTPUT_FILE);
  }

}
