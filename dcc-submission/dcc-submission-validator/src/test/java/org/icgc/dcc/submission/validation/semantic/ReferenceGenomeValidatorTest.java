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
package org.icgc.dcc.submission.validation.semantic;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE;
import static org.icgc.dcc.submission.validation.core.Error.error;
import static org.icgc.dcc.submission.validation.core.ErrorType.REFERENCE_GENOME_INSERTION_ERROR;
import static org.icgc.dcc.submission.validation.core.ErrorType.REFERENCE_GENOME_MISMATCH_ERROR;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import lombok.val;
import net.sf.picard.PicardException;

import org.junit.Test;

public class ReferenceGenomeValidatorTest extends BaseReferenceGenomeValidatorTest {

  /**
   * @see http://genome.ucsc.edu/cgi-bin/hgGateway
   * <p>
   * Use chromosome, start, end, reference to check
   */
  private final String[] baseCorrect = new String[] { "21", "33031597", "33031597", "G" };
  private final String[] baseWrong = new String[] { "21", "33031597", "33031597", "C" };
  private final String[] basesCorrect = new String[] { "8", "50000", "50005", "CTAAGA" };
  private final String[] basesWrong = new String[] { "8", "50000", "50005", "AGAATC" };

  @Test
  public void testSingleSequenceCorrect() {
    String ref = validator.getReferenceGenomeSequence(baseCorrect[0], baseCorrect[1], baseCorrect[2]);
    assertThat(ref).isEqualTo(baseCorrect[3]);
  }

  @Test
  public void testSingleSequenceIncorrect() {
    String ref = validator.getReferenceGenomeSequence(baseWrong[0], baseWrong[1], baseWrong[2]);
    assertThat(ref).isNotEqualTo(baseWrong[3]);
  }

  @Test
  public void testLongSequenceCorrect() {
    String ref = validator.getReferenceGenomeSequence(basesCorrect[0], basesCorrect[1], basesCorrect[2]);
    assertThat(ref).isEqualTo(basesCorrect[3]);
  }

  @Test
  public void testLongSequenceInCorrect() {
    String ref = validator.getReferenceGenomeSequence(basesWrong[0], basesWrong[1], basesWrong[2]);
    assertThat(ref).isNotEqualTo(basesWrong[3]);
  }

  @Test(expected = PicardException.class)
  public void testSequenceOutOfRange() {
    String chromosome = "9";
    String start = "1135797205";
    String end = "1135797205";
    validator.getReferenceGenomeSequence(chromosome, start, end);
  }

  @Test
  public void testSsmSamplePrimaryFile() throws IOException {
    val testFile = TEST_FILE_NAME;
    val context = mockContext();

    // Execute
    validator.validate(context);

    // Verify
    verify(context, times(1)).reportError(eq(
        error()
            .fileName(testFile)
            .fieldNames(SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE)
            .lineNumber(2)
            .type(REFERENCE_GENOME_MISMATCH_ERROR)
            .value("Expected: A, Actual: C")
            .params("GRCh37")
            .build()));
    verify(context, times(1)).reportError(eq(
        error()
            .fileName(testFile)
            .fieldNames(SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE)
            .lineNumber(3)
            .type(REFERENCE_GENOME_MISMATCH_ERROR)
            .value("Expected: T, Actual: C")
            .params("GRCh37")
            .build()));
    verify(context, times(1)).reportError(eq(
        error()
            .fileName(testFile)
            .fieldNames(SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE)
            .lineNumber(4)
            .type(REFERENCE_GENOME_MISMATCH_ERROR)
            .value("Expected: T, Actual: G")
            .params("GRCh37")
            .build()));
    verify(context, times(1)).reportError(eq(
        error()
            .fileName(testFile)
            .fieldNames(SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE)
            .lineNumber(5)
            .type(REFERENCE_GENOME_INSERTION_ERROR)
            .value("Expected: -, Actual: A")
            .params("GRCh37")
            .build()));
  }

}
