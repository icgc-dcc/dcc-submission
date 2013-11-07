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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Files.readLines;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.UUID;

import lombok.SneakyThrows;

import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.normalization.steps.PrimaryKeyGeneration;
import org.icgc.dcc.submission.normalization.steps.PrimaryKeyGenerationTest;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Optional;
import com.typesafe.config.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ PrimaryKeyGeneration.class })
public class NormalizationValidatorTest {

  @Mock
  private ValidationContext mockValidationContext;

  @Mock
  private SubmissionDirectory mockSubmissionDirectory;

  @Mock
  private Dictionary mockDictionary;

  @Mock
  private FileSchema mockFileSchema;

  @Mock
  Config config;

  @Before
  public void setUp() {
    when(config.hasPath(Mockito.anyString()))
        .thenReturn(false);
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
        .thenReturn(Optional.<String> of("deleteme"));
    when(mockValidationContext.getDictionary())
        .thenReturn(mockDictionary);
    when(mockValidationContext.getSubmissionDirectory())
        .thenReturn(mockSubmissionDirectory);
    when(mockValidationContext.getProjectKey())
        .thenReturn("dummy_project");

    mockUUID();
  }

  @SneakyThrows
  @Test
  public void test_normalize() {
    new File("/tmp/deleteme").delete(); // TODO: improve

    NormalizationValidator
        .getDefaultInstance(config)
        .validate(mockValidationContext);

    List<String> result = readLines(new File("/tmp/deleteme"), UTF_8); // TODO: improve
    List<String> ref = readLines(new File("/home/tony/git/git0/data-submission/ref"), UTF_8); // TODO: improve
    int refSize = ref.size();
    int resultSize = result.size();

    checkState(resultSize == refSize, resultSize + ", " + refSize);
    for (int i = 0; i < refSize; i++) {
      String resultLine = removeRandomUUID(result.get(i));
      String refLine = removeRandomUUID(ref.get(i));
      checkState(resultLine.equals(refLine), "\n\t" + resultLine + "\n\t" + refLine);
    }
  }

  private String removeRandomUUID(String row) {
    return row;// .replaceAll("\t[^\t]*$", ""); // TODO: improve
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

}
