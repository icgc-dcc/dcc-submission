package org.icgc.dcc.submission.validation.sample.core;

import static com.google.common.collect.ImmutableMap.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.EXP_SEQ_M_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.METH_ARRAY_M_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SGV_M_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SSM_M_TYPE;
import static org.icgc.dcc.submission.core.report.ErrorType.REFERENCE_SAMPLE_TYPE_MISMATCH;
import static org.icgc.dcc.submission.core.report.ErrorType.SAMPLE_TYPE_MISMATCH;
import static org.icgc.dcc.submission.validation.sample.util.SampleTypeFields.ANALYZED_SAMPLE_ID_FIELD_NAME;
import static org.icgc.dcc.submission.validation.sample.util.SampleTypeFields.MATCHED_SAMPLE_ID_FIELD_NAME;
import static org.icgc.dcc.submission.validation.sample.util.SampleTypeFields.REFERENCE_SAMPLE_TYPE_FIELD_NAME;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.core.report.Error;
import org.icgc.dcc.submission.validation.core.ReportContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import lombok.val;

@RunWith(MockitoJUnitRunner.class)
public class MetaFileSampleTypeProcessorTest {

  private static final long LINE_NUMBER = 1L;
  private static final Samples SAMPLES = new Samples(of(
      // Normal
      "normal-sample1", "101", // Normal - solid tissue
      "normal-sample2", "102", // Normal - blood derived

      // Tumor
      "tumor-sample3", "109", // Primary tumour - solid tissue
      "tumor-sample4", "125" // Cell line - derived from tumour
  ));

  @Mock
  ReportContext context;
  @Captor
  private ArgumentCaptor<Error> errors;

  @Test
  public void testValidSsmTumorNormal() throws IOException {
    val processor = createProcessor(SSM_M_TYPE);

    val record = of(
        ANALYZED_SAMPLE_ID_FIELD_NAME, "tumor-sample3",
        MATCHED_SAMPLE_ID_FIELD_NAME, "normal-sample1"
        );

    processor.process(LINE_NUMBER, record);

    verifyZeroInteractions(context);
  }

  @Test
  public void testInvalidSsmTumorTumor() throws IOException {
    val processor = createProcessor(SSM_M_TYPE);

    val record = of(
        ANALYZED_SAMPLE_ID_FIELD_NAME, "tumor-sample3",
        MATCHED_SAMPLE_ID_FIELD_NAME, "tumor-sample4"
        );

    processor.process(LINE_NUMBER, record);

    verify(context).reportError(errors.capture());

    val analyzedError = errors.getValue();
    assertThat(analyzedError.getType()).isEqualTo(SAMPLE_TYPE_MISMATCH);
    assertThat(analyzedError.getFieldNames()).containsOnly(MATCHED_SAMPLE_ID_FIELD_NAME);
  }

  @Test
  public void testInvalidSsmNormalNormal() throws IOException {
    val processor = createProcessor(SSM_M_TYPE);

    val record = of(
        ANALYZED_SAMPLE_ID_FIELD_NAME, "normal-sample1",
        MATCHED_SAMPLE_ID_FIELD_NAME, "normal-sample2"
        );

    processor.process(LINE_NUMBER, record);

    verify(context).reportError(errors.capture());

    val analyzedError = errors.getValue();
    assertThat(analyzedError.getType()).isEqualTo(SAMPLE_TYPE_MISMATCH);
    assertThat(analyzedError.getFieldNames()).containsOnly(ANALYZED_SAMPLE_ID_FIELD_NAME);
  }

  @Test
  public void testInvalidSsmNormalTumor() throws IOException {
    val processor = createProcessor(SSM_M_TYPE);

    val record = of(
        ANALYZED_SAMPLE_ID_FIELD_NAME, "normal-sample1",
        MATCHED_SAMPLE_ID_FIELD_NAME, "tumor-sample3"
        );

    processor.process(LINE_NUMBER, record);

    verify(context, times(2)).reportError(errors.capture());

    val analyzedError = errors.getAllValues().get(0);
    val matchedError = errors.getAllValues().get(1);

    assertThat(analyzedError.getType()).isEqualTo(SAMPLE_TYPE_MISMATCH);
    assertThat(analyzedError.getFieldNames()).containsOnly(ANALYZED_SAMPLE_ID_FIELD_NAME);

    assertThat(matchedError.getType()).isEqualTo(SAMPLE_TYPE_MISMATCH);
    assertThat(matchedError.getFieldNames()).containsOnly(MATCHED_SAMPLE_ID_FIELD_NAME);
  }

  @Test
  public void testValidSgvNormal() throws IOException {
    val processor = createProcessor(SGV_M_TYPE);

    val record = of(ANALYZED_SAMPLE_ID_FIELD_NAME, "normal-sample1");

    processor.process(LINE_NUMBER, record);

    verifyZeroInteractions(context);
  }

  @Test
  public void testInvalidSgvTumor() throws IOException {
    val processor = createProcessor(SGV_M_TYPE);

    val record = of(ANALYZED_SAMPLE_ID_FIELD_NAME, "tumor-sample3");

    processor.process(LINE_NUMBER, record);

    verify(context).reportError(errors.capture());

    val analyzedError = errors.getValue();
    assertThat(analyzedError.getType()).isEqualTo(SAMPLE_TYPE_MISMATCH);
    assertThat(analyzedError.getFieldNames()).containsOnly(ANALYZED_SAMPLE_ID_FIELD_NAME);
  }

  @Test
  public void testValidReferringSurveyNormalUnmatched() throws IOException {
    val processor = createProcessor(EXP_SEQ_M_TYPE);

    val record = of(
        ANALYZED_SAMPLE_ID_FIELD_NAME, "normal-sample1",
        REFERENCE_SAMPLE_TYPE_FIELD_NAME, "2"); // Unrelated normal

    processor.process(LINE_NUMBER, record);

    verifyZeroInteractions(context);
  }

  @Test
  public void testInvalidReferringSurveyNormalMatched() throws IOException {
    val processor = createProcessor(EXP_SEQ_M_TYPE);

    val record = of(
        ANALYZED_SAMPLE_ID_FIELD_NAME, "normal-sample1",
        REFERENCE_SAMPLE_TYPE_FIELD_NAME, "1"); // Matched normal

    processor.process(LINE_NUMBER, record);

    verify(context).reportError(errors.capture());

    val analyzedError = errors.getValue();
    assertThat(analyzedError.getType()).isEqualTo(REFERENCE_SAMPLE_TYPE_MISMATCH);
    assertThat(analyzedError.getFieldNames()).containsOnly(REFERENCE_SAMPLE_TYPE_FIELD_NAME);
  }

  @Test
  public void testValidNonReferringSurveyNormalUnmatched() throws IOException {
    val processor = createProcessor(METH_ARRAY_M_TYPE);

    val record = of(
        ANALYZED_SAMPLE_ID_FIELD_NAME, "normal-sample1",
        REFERENCE_SAMPLE_TYPE_FIELD_NAME, "1"); // Matched normal

    processor.process(LINE_NUMBER, record);

    verifyZeroInteractions(context);
  }

  private MetaFileSampleTypeProcessor createProcessor(FileType metaFileType) {
    val metaFile = new Path(metaFileType.getId() + ".txt");
    return new MetaFileSampleTypeProcessor(metaFileType, metaFile, SAMPLES, context);
  }

}
