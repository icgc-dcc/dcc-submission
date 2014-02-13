package org.icgc.dcc.submission.validation.first.step;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
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
import org.icgc.dcc.submission.core.report.Error;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.first.FPVFileSystem;
import org.icgc.dcc.submission.validation.first.FPVFileSystem.CodecType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;

@RunWith(MockitoJUnitRunner.class)
public class FileCorruptionCheckerTest {

  private static final String TEST_TEXT = "THIS_IS_A_TEST";
  private Dictionary dict;

  @Mock
  ValidationContext validationContext;
  @Mock
  FPVFileSystem fs;

  @Before
  public void setup() {
    dict = mock(Dictionary.class);

    FileSchema testSchema = mock(FileSchema.class);
    String paramString = "testfile1";
    when(testSchema.getPattern()).thenReturn(paramString);
    when(dict.getFileSchemaByName(anyString())).thenReturn(Optional.of(testSchema));
    when(dict.getFileSchemaByFileName(anyString())).thenReturn(Optional.of(testSchema));

    when(validationContext.getDictionary()).thenReturn(dict);
  }

  @Test
  public void testTextInputValid() throws Exception {
    // TODO: should close those...
    DataInputStream textInputStream = getTestInputStream(CodecType.PLAIN_TEXT);

    when(fs.getNoCompressionInputStream(anyString())).thenReturn(textInputStream);
    FileCorruptionChecker checker =
        new FileCorruptionChecker(
            new NoOpFileChecker(validationContext, fs));
    checker.check(anyString());
    checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void testGZipInputValid() throws Exception {
    when(fs.getNoCompressionInputStream(anyString())).thenReturn(getTestInputStream(CodecType.GZIP),
        getTestInputStream(CodecType.GZIP));
    FileCorruptionChecker checker =
        new FileCorruptionChecker(
            new NoOpFileChecker(validationContext, fs));
    checker.check("file1.gz");
    checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void testBZip2InputValid() throws Exception {
    when(fs.getNoCompressionInputStream(anyString())).thenReturn(getTestInputStream(CodecType.BZIP2),
        getTestInputStream(CodecType.BZIP2));

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.check("file.bz2");
    checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void testGZipInputNotValid() throws Exception {
    when(fs.getNoCompressionInputStream(anyString())).thenReturn(getTestInputStream(CodecType.GZIP),
        corruptInputStream(getTestInputStream(CodecType.GZIP)));

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.check("file.gz");
    verify(fs, times(2)).getNoCompressionInputStream(anyString());
    checkErrorReported();
  }

  @Test
  public void testBZip2InputNotValid() throws Exception {
    when(fs.getNoCompressionInputStream(anyString())).thenReturn(getTestInputStream(CodecType.BZIP2),
        corruptInputStream(getTestInputStream(CodecType.BZIP2)));

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.check("file.bz2");
    verify(fs, times(2)).getNoCompressionInputStream(anyString());
    checkErrorReported();
  }

  @Test
  public void testFilenameBzCodecMismatch() throws Exception {
    when(fs.getNoCompressionInputStream(anyString())).thenReturn(getTestInputStream(CodecType.BZIP2));

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.check("file.gz");
    verify(fs, times(1)).getNoCompressionInputStream(anyString());
    checkErrorReported();

  }

  @Test
  public void testFilenameTextCodecMismatch() throws Exception {
    when(fs.getNoCompressionInputStream(anyString())).thenReturn(getTestInputStream(CodecType.PLAIN_TEXT));

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.check("file.gz");
    verify(fs, times(1)).getNoCompressionInputStream(anyString());
    checkErrorReported();

  }

  @Test
  public void testFilenameGzCodecMismatch() throws Exception {
    when(fs.getNoCompressionInputStream(anyString())).thenReturn(getTestInputStream(CodecType.GZIP));

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.check("file.txt");
    verify(fs, times(1)).getNoCompressionInputStream(anyString());
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

  /**
   * Must close stream afterwards.
   */
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
    case PLAIN_TEXT:
      // Do nothing
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
    when(fs.getNoCompressionInputStream(anyString())).thenReturn(textInputStream);
    assertEquals(CodecType.PLAIN_TEXT, fs.determineCodecFromContent(anyString()));
  }

  @Test
  public void testGZipInputDetection() throws Exception {
    DataInputStream inputStream = getTestInputStream(CodecType.GZIP);
    when(fs.getNoCompressionInputStream(anyString())).thenReturn(inputStream);
    assertEquals(CodecType.GZIP, fs.determineCodecFromContent(anyString()));
  }

  @Test
  public void testBZipInputDetection() throws Exception {
    DataInputStream inputStream = getTestInputStream(CodecType.BZIP2);
    when(fs.getNoCompressionInputStream(anyString())).thenReturn(inputStream);
    assertEquals(CodecType.BZIP2, fs.determineCodecFromContent(anyString()));
  }

  private void checkErrorReported() {
    verify(validationContext, times(1)).reportError(any(Error.class));
  }

  private void checkNoErrorsReported(ValidationContext validationContext) {
    verify(validationContext, times(0)).reportError(any(Error.class));
  }

}
