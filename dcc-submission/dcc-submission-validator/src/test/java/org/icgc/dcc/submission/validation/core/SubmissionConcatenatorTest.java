package org.icgc.dcc.submission.validation.core;

import static com.typesafe.config.ConfigFactory.parseMap;

import java.io.File;
import java.io.IOException;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
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
    System.out.println("Test root dir: '" + rootDir + "'");

    copyDirectory(TEST_DIR, new Path(rootDir, new Path(RELEASE_NAME, PROJECT_KEY)));
  }

  @Test
  public void testConcat() throws Exception {
    val submissionDirectory = createSubmissionDirectory();

    concatenator.concat(submissionDirectory);
  }

  private void copyDirectory(File sourceDir, Path targetDir) throws IOException {
    for (val file : sourceDir.listFiles()) {
      val source = new Path(file.toURI());
      val target = new Path(targetDir, file.getName());

      System.out.println("Copying file: from '" + source + "' to '" + target + "'");
      fileSystem.copyFromLocalFile(source, target);
    }
  }

  private Dictionary getDictionary() {
    // Patch file name patterns to support multiple files per file type
    // TODO: Remove patching
    val dictionary = TestUtils.dictionary();
    for (val fileSchema : dictionary.getFiles()) {
      val regex = fileSchema.getPattern();
      val patchedRegex = regex.replaceFirst("\\.", "\\.(?:[^.]+\\\\.)?");
      fileSchema.setPattern(patchedRegex);

      log.warn("Patched '{}' file schema regex from '{}' to '{}'!",
          new Object[] { fileSchema.getName(), regex, patchedRegex });
    }

    return dictionary;
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

}
