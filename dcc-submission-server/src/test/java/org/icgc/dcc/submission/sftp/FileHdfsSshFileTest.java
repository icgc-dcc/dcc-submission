package org.icgc.dcc.submission.sftp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RawLocalFileSystem;
import org.apache.shiro.subject.Subject;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.fs.SubmissionFile;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.sftp.fs.FileHdfsSshFile;
import org.icgc.dcc.submission.sftp.fs.RootHdfsSshFile;
import org.icgc.dcc.submission.sftp.fs.SubmissionDirectoryHdfsSshFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FileHdfsSshFileTest {

  private static final String RELEASE_NAME = "release1";

  private static final String PROJECT_KEY = "project1";

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Mock
  Release release;
  @Mock
  Submission submission;
  @Mock
  Project project;
  @Mock
  SubmissionDirectory submissionDirectory;
  @Mock
  SftpContext context;
  @Mock
  Subject subject;

  SubmissionDirectoryHdfsSshFile directory;

  @Before
  public void setUp() throws IOException {
    // Create the simulated project directory
    File root = tmp.newFolder(RELEASE_NAME);
    String projectDirectoryName = "/" + PROJECT_KEY;
    File projectDirectory = new File(root, projectDirectoryName);
    projectDirectory.mkdir();

    when(submissionDirectory.isReadOnly()).thenReturn(false);
    when(submissionDirectory.getSubmission()).thenReturn(submission);
    when(context.getFileSystem()).thenReturn(createFileSystem());
    when(context.getReleasePath()).thenReturn(new Path(root.getAbsolutePath()));
    when(context.getSubmissionFile(any(Path.class))).thenReturn(new SubmissionFile("", new Date(), 0, null));
    when(context.getSubmissionDirectory(PROJECT_KEY, subject)).thenReturn(submissionDirectory);

    RootHdfsSshFile rootDirectory = new RootHdfsSshFile(context, subject);
    String directoryName = PROJECT_KEY;
    directory = new SubmissionDirectoryHdfsSshFile(context, rootDirectory, directoryName);
  }

  @Test
  public void testDoesNotExist() throws IOException {
    String fileName = "file.txt";
    FileHdfsSshFile file = new FileHdfsSshFile(context, directory, fileName);

    assertThat(file.doesExist()).isFalse();
  }

  @Test
  public void testCreate() throws IOException {
    String fileName = "file.txt";
    FileHdfsSshFile file = new FileHdfsSshFile(context, directory, fileName);

    assertThat(file.create()).isTrue();
    assertThat(file.doesExist()).isTrue();
  }

  private static RawLocalFileSystem createFileSystem() {
    RawLocalFileSystem localFileSystem = new RawLocalFileSystem();
    localFileSystem.setConf(new Configuration());

    return localFileSystem;
  }

}
