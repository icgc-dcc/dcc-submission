package org.icgc.dcc.submission.validation.first.file;

import static org.icgc.dcc.submission.validation.first.io.FPVFileSystem.CodecType.BZIP2;
import static org.icgc.dcc.submission.validation.first.io.FPVFileSystem.CodecType.GZIP;
import static org.icgc.dcc.submission.validation.first.io.FPVFileSystem.CodecType.PLAIN_TEXT;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.first.file.FileCorruptionChecker;
import org.icgc.dcc.submission.validation.first.file.NoOpFileChecker;
import org.icgc.dcc.submission.validation.first.io.FPVFileSystem;
import org.icgc.dcc.submission.validation.first.io.FPVFileSystem.CodecType;
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
  SubmissionDirectory submissionDirectory;

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

    fs = spy(new FPVFileSystem(submissionDirectory));
  }

  @Test
  public void testTextInputValid() throws Exception {
    // TODO: should close those...
    DataInputStream textInputStream = getTestInputStream(PLAIN_TEXT);

    doReturn(textInputStream).when(fs).getDecompressingInputStream(anyString());
    doReturn(PLAIN_TEXT).when(fs).determineCodecFromContent(anyString());
    doReturn(PLAIN_TEXT).when(fs).determineCodecFromFilename(anyString());

    FileCorruptionChecker checker =
        new FileCorruptionChecker(
            new NoOpFileChecker(validationContext, fs));
    checker.checkFile(anyString());
    checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void testGZipInputValid() throws Exception {
    DataInputStream testInputStream = getTestInputStream(GZIP);
    doReturn(testInputStream).when(fs).getDecompressingInputStream(anyString());
    doReturn(GZIP).when(fs).determineCodecFromContent(anyString());
    doReturn(GZIP).when(fs).determineCodecFromFilename(anyString());
    when(submissionDirectory.open(anyString())).thenReturn(testInputStream);

    FileCorruptionChecker checker =
        new FileCorruptionChecker(
            new NoOpFileChecker(validationContext, fs));
    checker.checkFile("file1.gz");
    checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void testBZip2InputValid() throws Exception {
    DataInputStream testInputStream = getTestInputStream(BZIP2);
    doReturn(testInputStream).when(fs).getDecompressingInputStream(anyString());
    doReturn(BZIP2).when(fs).determineCodecFromContent(anyString());
    doReturn(BZIP2).when(fs).determineCodecFromFilename(anyString());
    when(submissionDirectory.open(anyString())).thenReturn(testInputStream);

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.checkFile("file.bz2");
    checkNoErrorsReported(validationContext);
    assertTrue(checker.isValid());
  }

  @Test
  public void testConcatenatedBZip2InputValid() throws Exception {
    DataInputStream testInputStream = getConcatenatedBZipTestInputStream(TEST_TEXT);
    doReturn(testInputStream).when(fs).getDecompressingInputStream(anyString());
    doReturn(BZIP2).when(fs).determineCodecFromContent(anyString());
    doReturn(BZIP2).when(fs).determineCodecFromFilename(anyString());
    when(submissionDirectory.open(anyString())).thenReturn(testInputStream);

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.checkFile("file.bz2");
    checkErrorReported();
  }

  @Test
  public void testGZipInputNotValid() throws Exception {
    doReturn(GZIP).when(fs).determineCodecFromContent(anyString());
    doReturn(GZIP).when(fs).determineCodecFromFilename(anyString());
    doThrow(new IOException()).when(fs).attemptGzipRead(anyString());

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.checkFile("file.gz");
    verify(fs).determineCodecFromFilename(anyString());
    verify(fs).determineCodecFromContent(anyString());
    verify(fs).attemptGzipRead(anyString());
    verify(fs, never()).getDecompressingInputStream(anyString());
    checkErrorReported();
  }

  @Test
  public void testBZip2InputNotValid() throws Exception {
    doReturn(BZIP2).when(fs).determineCodecFromContent(anyString());
    doReturn(BZIP2).when(fs).determineCodecFromFilename(anyString());
    doThrow(new IOException()).when(fs).attemptBzip2Read(anyString());

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.checkFile("file.bz2");
    verify(fs).determineCodecFromFilename(anyString());
    verify(fs).determineCodecFromContent(anyString());
    verify(fs).attemptBzip2Read(anyString());
    verify(fs, never()).getDecompressingInputStream(anyString());
    checkErrorReported();
  }

  @Test
  public void testFilenameBzCodecMismatch() throws Exception {

    DataInputStream testInputStream = getTestInputStream(BZIP2);
    doReturn(testInputStream).when(fs).getDecompressingInputStream(anyString());
    doReturn(BZIP2).when(fs).determineCodecFromContent(anyString());
    doReturn(PLAIN_TEXT).when(fs).determineCodecFromFilename(anyString());
    when(submissionDirectory.open(anyString())).thenReturn(testInputStream);

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.checkFile("file.gz");
    verify(fs).determineCodecFromFilename(anyString());
    verify(fs).determineCodecFromContent(anyString());
    verify(fs, never()).attemptBzip2Read(anyString());
    verify(fs, never()).attemptGzipRead(anyString());
    verify(fs, never()).getDecompressingInputStream(anyString());
    checkErrorReported();

  }

  @Test
  public void testFilenameTextCodecMismatch() throws Exception {
    DataInputStream testInputStream = getTestInputStream(BZIP2);
    doReturn(testInputStream).when(fs).getDecompressingInputStream(anyString());
    doReturn(GZIP).when(fs).determineCodecFromContent(anyString());
    doReturn(PLAIN_TEXT).when(fs).determineCodecFromFilename(anyString());

    FileCorruptionChecker checker = new FileCorruptionChecker(new NoOpFileChecker(validationContext, fs));
    checker.checkFile("file.gz");
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

  public static DataInputStream getConcatenatedBZipTestInputStream(String content) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    BZip2CompressorOutputStream out = new BZip2CompressorOutputStream(bytes);
    IOUtils.write(content.getBytes(), out);
    IOUtils.closeQuietly(out);
    out = new BZip2CompressorOutputStream(bytes);
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
