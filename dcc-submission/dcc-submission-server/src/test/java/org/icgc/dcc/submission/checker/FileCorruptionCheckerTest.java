package org.icgc.dcc.submission.checker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.icgc.dcc.submission.checker.Util.CodecType;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

public class FileCorruptionCheckerTest {

  private static final String TEST_TEXT = "THIS_IS_A_TEST";
  private SubmissionDirectory submissionDir;
  private Dictionary dict;
  private DccFileSystem fs;

  @Before
  public void setup() {
    submissionDir = mock(SubmissionDirectory.class);
    dict = mock(Dictionary.class);
    fs = mock(DccFileSystem.class);

    FileSchema testSchema = mock(FileSchema.class);
    String paramString = "testfile1";
    when(testSchema.getPattern()).thenReturn(paramString);
    when(dict.fileSchema(anyString())).thenReturn(Optional.of(testSchema));
    when(submissionDir.listFile()).thenReturn(ImmutableList.of("testfile1", "testfile2"));
  }

  @Test
  public void testTextInputValid() throws Exception {
    DataInputStream textInputStream = getTestInputStream(CodecType.PLAIN_TEXT);
    when(fs.open(anyString())).thenReturn(textInputStream);
    FileCorruptionChecker checker = new FileCorruptionChecker(new BaseFileChecker(dict, submissionDir), fs);
    List<FirstPassValidationError> errors = checker.check(anyString());
    assertTrue(errors.isEmpty());
    assertTrue(checker.isValid());
  }

  @Test
  public void testGZipInputValid() throws Exception {
    DataInputStream testInputStream = getTestInputStream(CodecType.GZIP);
    when(fs.open(anyString())).thenReturn(testInputStream);
    FileCorruptionChecker checker = new FileCorruptionChecker(new BaseFileChecker(dict, submissionDir), fs);
    List<FirstPassValidationError> errors = checker.check(anyString());
    assertTrue(errors.isEmpty());
    assertTrue(checker.isValid());
  }

  @Test
  public void testBZip2InputValid() throws Exception {
    DataInputStream testInputStream = getTestInputStream(CodecType.BZIP2);
    when(fs.open(anyString())).thenReturn(testInputStream);
    FileCorruptionChecker checker = new FileCorruptionChecker(new BaseFileChecker(dict, submissionDir), fs);
    List<FirstPassValidationError> errors = checker.check(anyString());
    assertTrue(errors.isEmpty());
    assertTrue(checker.isValid());
  }

  /**
   * @param plainText
   * @return
   * @throws IOException
   */
  private static DataInputStream getTestInputStream(CodecType type) throws IOException {
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
    IOUtils.write(TEST_TEXT.getBytes(), out);
    IOUtils.closeQuietly(out);
    return new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
  }

  @Test
  public void testTextInputDetection() throws Exception {
    DataInputStream textInputStream = getTestInputStream(CodecType.PLAIN_TEXT);
    when(fs.open(anyString())).thenReturn(textInputStream);
    assertEquals(CodecType.PLAIN_TEXT, Util.determineCodec(fs, submissionDir, anyString()));
  }

  @Test
  public void testGZipInputDetection() throws Exception {
    DataInputStream inputStream = getTestInputStream(CodecType.GZIP);
    when(fs.open(anyString())).thenReturn(inputStream);
    assertEquals(CodecType.GZIP, Util.determineCodec(fs, submissionDir, anyString()));
  }

  @Test
  public void testBZipInputDetection() throws Exception {
    DataInputStream inputStream = getTestInputStream(CodecType.BZIP2);
    when(fs.open(anyString())).thenReturn(inputStream);
    assertEquals(CodecType.BZIP2, Util.determineCodec(fs, submissionDir, anyString()));
  }

}
