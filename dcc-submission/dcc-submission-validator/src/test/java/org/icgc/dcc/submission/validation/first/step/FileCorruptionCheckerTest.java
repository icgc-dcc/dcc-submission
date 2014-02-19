package org.icgc.dcc.submission.validation.first.step;

import static org.icgc.dcc.submission.validation.first.FPVFileSystem.CodecType.BZIP2;
import static org.icgc.dcc.submission.validation.first.FPVFileSystem.CodecType.GZIP;
import static org.icgc.dcc.submission.validation.first.FPVFileSystem.CodecType.PLAIN_TEXT;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    DataInputStream textInputStream = getTestInputStream(PLAIN_TEXT);

    when(fs.getDecompressingInputStream(anyString())).thenReturn(textInputStream);
    when(fs.determineCodecFromFilename(anyString())).thenReturn(PLAIN_TEXT);
    when(fs.determineCodecFromContent(anyString())).thenReturn(PLAIN_TEXT);

    FileCorruptionChecker checker =
        new FileCorruptionChecker(
            new NoOpFileChecker(validationContext, fs));
    checker.check(anyString());
    checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void testGZipInputValid() throws Exception {
    when(fs.getDecompressingInputStream(anyString())).thenReturn(getTestInputStream(GZIP));
    when(fs.determineCodecFromFilename(anyString())).thenReturn(GZIP);
    when(fs.determineCodecFromContent(anyString())).thenReturn(GZIP);

    FileCorruptionChecker checker =
        new FileCorruptionChecker(
            new NoOpFileChecker(validationContext, fs));
    checker.check("file1.gz");
    checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void testBZip2InputValid() throws Exception {
    when(fs.getDecompressingInputStream(anyString())).thenReturn(getTestInputStream(BZIP2));
    when(fs.determineCodecFromFilename(anyString())).thenReturn(BZIP2);
    when(fs.determineCodecFromContent(anyString())).thenReturn(BZIP2);

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.check("file.bz2");
    checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void testGZipInputNotValid() throws Exception {
    when(fs.determineCodecFromFilename(anyString())).thenReturn(GZIP);
    when(fs.determineCodecFromContent(anyString())).thenReturn(GZIP);
    doThrow(new IOException()).when(fs).attemptGzipRead(anyString());

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.check("file.gz");
    verify(fs).determineCodecFromFilename(anyString());
    verify(fs).determineCodecFromContent(anyString());
    verify(fs).attemptGzipRead(anyString());
    verify(fs, never()).getDecompressingInputStream(anyString());
    checkErrorReported();
  }

  @Test
  public void testBZip2InputNotValid() throws Exception {
    when(fs.determineCodecFromFilename(anyString())).thenReturn(BZIP2);
    when(fs.determineCodecFromContent(anyString())).thenReturn(BZIP2);
    doThrow(new IOException()).when(fs).attemptBzip2Read(anyString());

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.check("file.bz2");
    verify(fs).determineCodecFromFilename(anyString());
    verify(fs).determineCodecFromContent(anyString());
    verify(fs).attemptBzip2Read(anyString());
    verify(fs, never()).getDecompressingInputStream(anyString());
    checkErrorReported();
  }

  @Test
  public void testFilenameBzCodecMismatch() throws Exception {
    when(fs.getDecompressingInputStream(anyString())).thenReturn(getTestInputStream(BZIP2));
    when(fs.determineCodecFromFilename(anyString())).thenReturn(PLAIN_TEXT);
    when(fs.determineCodecFromContent(anyString())).thenReturn(BZIP2);

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.check("file.gz");
    verify(fs).determineCodecFromFilename(anyString());
    verify(fs).determineCodecFromContent(anyString());
    verify(fs, never()).attemptBzip2Read(anyString());
    verify(fs, never()).attemptGzipRead(anyString());
    verify(fs, never()).getDecompressingInputStream(anyString());
    checkErrorReported();

  }

  @Test
  public void testFilenameTextCodecMismatch() throws Exception {
    when(fs.determineCodecFromFilename(anyString())).thenReturn(PLAIN_TEXT);
    when(fs.determineCodecFromContent(anyString())).thenReturn(GZIP);

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.check("file.gz");
    verify(fs).determineCodecFromFilename(anyString());
    verify(fs).determineCodecFromContent(anyString());
    verify(fs, never()).attemptBzip2Read(anyString());
    verify(fs, never()).attemptGzipRead(anyString());
    verify(fs, never()).getDecompressingInputStream(anyString());
    checkErrorReported();

  }

  @SuppressWarnings("unused")
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

  private void checkErrorReported() {
    verify(validationContext, times(1)).reportError(any(Error.class));
  }

  private void checkNoErrorsReported(ValidationContext validationContext) {
    verify(validationContext, times(0)).reportError(any(Error.class));
  }

}
