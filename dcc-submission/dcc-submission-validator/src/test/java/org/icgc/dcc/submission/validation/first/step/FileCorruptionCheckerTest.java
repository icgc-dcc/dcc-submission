package org.icgc.dcc.submission.validation.first.step;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.validation.cascading.TupleState.TupleError;
import org.icgc.dcc.submission.validation.core.ErrorType;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.first.Util;
import org.icgc.dcc.submission.validation.first.Util.CodecType;
import org.icgc.dcc.submission.validation.first.step.FileCorruptionChecker;
import org.icgc.dcc.submission.validation.first.step.NoOpFileChecker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

@RunWith(MockitoJUnitRunner.class)
public class FileCorruptionCheckerTest {

  private static final String TEST_TEXT = "THIS_IS_A_TEST";
  private SubmissionDirectory submissionDir;
  private Dictionary dict;
  private DccFileSystem fs;

  @Mock
  ValidationContext validationContext;

  @Before
  public void setup() {
    submissionDir = mock(SubmissionDirectory.class);
    dict = mock(Dictionary.class);
    fs = mock(DccFileSystem.class);

    FileSchema testSchema = mock(FileSchema.class);
    String paramString = "testfile1";
    when(testSchema.getPattern()).thenReturn(paramString);
    when(dict.getFileSchemaByName(anyString())).thenReturn(Optional.of(testSchema));
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(Optional.of(testSchema));
    when(submissionDir.listFile()).thenReturn(ImmutableList.of("testfile1", "testfile2"));

    when(validationContext.getDccFileSystem()).thenReturn(fs);
    when(validationContext.getSubmissionDirectory()).thenReturn(submissionDir);
    when(validationContext.getDictionary()).thenReturn(dict);
  }

  @Test
  public void testTextInputValid() throws Exception {
    DataInputStream textInputStream = getTestInputStream(CodecType.PLAIN_TEXT);
    when(fs.open(anyString())).thenReturn(textInputStream);
    FileCorruptionChecker checker =
        new FileCorruptionChecker(
            new NoOpFileChecker(validationContext));
    checker.check(anyString());
    checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void testGZipInputValid() throws Exception {
    when(fs.open(anyString())).thenReturn(getTestInputStream(CodecType.GZIP), getTestInputStream(CodecType.GZIP));
    FileCorruptionChecker checker =
        new FileCorruptionChecker(
            new NoOpFileChecker(validationContext));
    checker.check("file1.gz");
    checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void testBZip2InputValid() throws Exception {
    when(fs.open(anyString())).thenReturn(getTestInputStream(CodecType.BZIP2), getTestInputStream(CodecType.BZIP2));

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext));
    checker.check("file.bz2");
    checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void testGZipInputNotValid() throws Exception {
    when(fs.open(anyString())).thenReturn(getTestInputStream(CodecType.GZIP),
        corruptInputStream(getTestInputStream(CodecType.GZIP)));

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext));
    checker.check("file.gz");
    verify(fs, times(2)).open(anyString());
    checkErrorReported();
  }

  @Test
  public void testBZip2InputNotValid() throws Exception {
    when(fs.open(anyString())).thenReturn(getTestInputStream(CodecType.BZIP2),
        corruptInputStream(getTestInputStream(CodecType.BZIP2)));

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext));
    checker.check("file.bz2");
    verify(fs, times(2)).open(anyString());
    checkErrorReported();
  }

  @Test
  public void testFilenameBzCodecMismatch() throws Exception {
    when(fs.open(anyString())).thenReturn(getTestInputStream(CodecType.BZIP2));

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext));
    checker.check("file.gz");
    verify(fs, times(1)).open(anyString());
    checkErrorReported();

  }

  @Test
  public void testFilenameTextCodecMismatch() throws Exception {
    when(fs.open(anyString())).thenReturn(getTestInputStream(CodecType.PLAIN_TEXT));

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext));
    checker.check("file.gz");
    verify(fs, times(1)).open(anyString());
    checkErrorReported();

  }

  @Test
  public void testFilenameGzCodecMismatch() throws Exception {
    when(fs.open(anyString())).thenReturn(getTestInputStream(CodecType.GZIP));

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext));
    checker.check("file.txt");
    verify(fs, times(1)).open(anyString());
    checkErrorReported();
  }

  private static DataInputStream corruptInputStream(DataInputStream is) {
    return new DataInputStream(new DataCorruptionInputStream(is));
  }

  private final static class DataCorruptionInputStream extends FilterInputStream {

    public DataCorruptionInputStream(InputStream is) {
      super(is);
    }

    @Override
    public int read() {
      return 0;
    }

    @Override
    public int read(byte[] paramArrayOfByte, int paramInt1, int paramInt2)
        throws IOException
    {
      int returnCode = this.in.read(paramArrayOfByte, paramInt1, paramInt2);
      Random ran = new Random();
      ran.nextBytes(paramArrayOfByte);
      return returnCode;
    }
  }

  public static DataInputStream getTestInputStream(String content, CodecType type) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    OutputStream out = bytes;
    switch (type) {
    case GZIP:
      out = new GZIPOutputStream(bytes);
      break;
    case BZIP2:
      out = new BZip2CompressorOutputStream(bytes);
      break;
    }
    IOUtils.write(content.getBytes(), out);
    IOUtils.closeQuietly(out);
    return new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
  }

  private static DataInputStream getTestInputStream(CodecType type) throws IOException {
    return getTestInputStream(TEST_TEXT, type);
  }

  @Test
  public void testTextInputDetection() throws Exception {
    DataInputStream textInputStream = getTestInputStream(CodecType.PLAIN_TEXT);
    when(fs.open(anyString())).thenReturn(textInputStream);
    assertEquals(CodecType.PLAIN_TEXT, Util.determineCodecFromContent(fs, submissionDir, anyString()));
  }

  @Test
  public void testGZipInputDetection() throws Exception {
    DataInputStream inputStream = getTestInputStream(CodecType.GZIP);
    when(fs.open(anyString())).thenReturn(inputStream);
    assertEquals(CodecType.GZIP, Util.determineCodecFromContent(fs, submissionDir, anyString()));
  }

  @Test
  public void testBZipInputDetection() throws Exception {
    DataInputStream inputStream = getTestInputStream(CodecType.BZIP2);
    when(fs.open(anyString())).thenReturn(inputStream);
    assertEquals(CodecType.BZIP2, Util.determineCodecFromContent(fs, submissionDir, anyString()));
  }

  private void checkErrorReported() {
    verify(validationContext, times(1)).reportError(anyString(), any(ErrorType.class), any());
  }

  private void checkNoErrorsReported(ValidationContext validationContext) {
    verify(validationContext, times(0)).reportError(anyString(), any(ErrorType.class));
    verify(validationContext, times(0)).reportError(anyString(), any(TupleError.class));
    verify(validationContext, times(0)).reportError(anyString(), any(ErrorType.class), any());
    verify(validationContext, times(0)).reportError(anyString(), any(), any(ErrorType.class));
    verify(validationContext, times(0)).reportError(anyString(), anyLong(), any(), any(ErrorType.class));
    verify(validationContext, times(0)).reportError(anyString(), anyLong(), anyString(), any(), any(ErrorType.class));
  }

}
