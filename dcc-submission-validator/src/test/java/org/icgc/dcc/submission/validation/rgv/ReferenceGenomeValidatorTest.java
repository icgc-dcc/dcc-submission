package org.icgc.dcc.submission.validation.rgv;

import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_REFERENCE_GENOME_ALLELE;
import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.core.report.ErrorType.REFERENCE_GENOME_INSERTION_ERROR;
import static org.icgc.dcc.submission.core.report.ErrorType.REFERENCE_GENOME_MISMATCH_ERROR;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import lombok.val;

import org.junit.Test;

public class ReferenceGenomeValidatorTest extends BaseReferenceGenomeValidatorTest {

  @Test
  public void testSsmSamplePrimaryFile() throws IOException, InterruptedException {
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
