package org.icgc.dcc.submission.validation.core;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.io.Files.readLines;
import static com.typesafe.config.ConfigFactory.parseMap;
import static org.apache.commons.lang.StringUtils.join;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.fest.util.Files.contentOf;

import java.io.File;
import java.io.IOException;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.validation.TestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.PatternFilenameFilter;
import com.typesafe.config.Config;

@Slf4j
public class SubmissionConcatenatorTest {

  /**
   * Test file system.
   */
  static final File TEST_DIR = new File("src/test/resources/fixtures/validation/concat/fs");

  /**
   * Test data.
   */
  static final String RELEASE_NAME = "release1";
  static final String PROJECT_KEY = "project1";

  /**
   * Scratch space.
   */
  @Rule
  public final TemporaryFolder tmp = new TemporaryFolder();

  /**
   * Environment.
   */
  FileSystem fileSystem;
  Path rootDir;

  /**
   * Class under test.
   */
  SubmissionConcatenator concatenator;

  @Before
  public void setUp() throws IOException {
    this.fileSystem = FileSystem.getLocal(new Configuration());
    this.rootDir = new Path(tmp.newFolder().getAbsolutePath());
    this.concatenator = new SubmissionConcatenator(fileSystem, getDictionary());
    log.info("Test root dir: '{}'", rootDir);

    copyDirectory(TEST_DIR, new Path(rootDir, new Path(RELEASE_NAME, PROJECT_KEY)));
  }

  @Test
  public void testConcat() throws Exception {
    val submissionDirectory = createSubmissionDirectory();

    val concatFiles = concatenator.concat(submissionDirectory);
    assertThat(concatFiles.size()).isEqualTo(1);

    val concatFile = concatFiles.get(0);
    val testFiles = getTestFiles("donor.*.txt");
    assertThat(concatFile.getParts().size()).isEqualTo(testFiles.length);

    val lineCount = concatFile.getLineCount();
    assertThat(lineCount).isEqualTo(4);

    try {
      concatFile.getCoordinates(-1); // TODO: add test for original line number too
      fail("Negative line number");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    try {
      concatFile.getCoordinates(1);
      fail("Header line number");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    val part1 = concatFile.getCoordinates(2);
    assertThat(part1.getOriginalPath().getName()).isEqualTo("donor.1.txt");
    val part2 = concatFile.getCoordinates(3);
    assertThat(part2.getOriginalPath().getName()).isEqualTo("donor.2.txt");
    val part3 = concatFile.getCoordinates(4);
    assertThat(part3.getOriginalPath().getName()).isEqualTo("donor.3.txt");

    try {
      concatFile.getCoordinates(5);
      fail("Invalid line number");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    val testLines1 = readLines(testFiles[0], UTF_8);
    val testLines2 = readLines(testFiles[1], UTF_8);
    val testLines3 = readLines(testFiles[2], UTF_8);

    // Remove headers
    testLines2.remove(0);
    testLines3.remove(0);

    val testLines = newArrayList();
    testLines.addAll(testLines1);
    testLines.addAll(testLines2);
    testLines.addAll(testLines3);

    val expectedContent = join(testLines, "\n") + "\n";
    val actualContent = contentOf(new File(concatFile.getPath().toUri().toString()), UTF_8);
    assertThat(actualContent).isEqualTo(expectedContent);

  }

  private void copyDirectory(File sourceDir, Path targetDir) throws IOException {
    for (val file : sourceDir.listFiles()) {
      val source = new Path(file.toURI());
      val target = new Path(targetDir, file.getName());

      log.info("Copying file: from '{}' to '{}'", source, target);
      fileSystem.copyFromLocalFile(source, target);
    }
  }

  private Dictionary getDictionary() {
    val dictionary = TestUtils.dictionary();

    // Patch file name patterns to support multiple files per file type
    // TODO: Remove patching
    for (val fileSchema : dictionary.getFiles()) {
      patchFileSchema(fileSchema);
    }

    return dictionary;
  }

  private void patchFileSchema(FileSchema fileSchema) {
    val regex = fileSchema.getPattern();
    val patchedRegex = regex.replaceFirst("\\.", "\\.(?:[^.]+\\\\.)?");
    fileSchema.setPattern(patchedRegex);

    log.warn("Patched '{}' file schema regex from '{}' to '{}'!",
        new Object[] { fileSchema.getName(), regex, patchedRegex });
  }

  private SubmissionDirectory createSubmissionDirectory() {
    return new SubmissionDirectory(
        new DccFileSystem(getConfig(), fileSystem),
        new Release(RELEASE_NAME),
        PROJECT_KEY,
        new Submission());
  }

  private Config getConfig() {
    val fsRoot = rootDir.toUri().toString();
    val fsUrl = fileSystem.getUri().toString();

    return parseMap(ImmutableMap.<String, Object> of(
        "fs.root", fsRoot,
        "fs.url", fsUrl
        ));
  }

  private File[] getTestFiles(String regex) {
    return TEST_DIR.listFiles(new PatternFilenameFilter(regex));
  }

}
