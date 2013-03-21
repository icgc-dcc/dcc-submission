package org.icgc.dcc.sftp;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.RawLocalFileSystem;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.filesystem.SubmissionDirectory;
import org.icgc.dcc.release.NextRelease;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.Submission;
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

  // @formatter:off
  @Mock Release release;
  @Mock Submission submission;
  @Mock Project project;
  @Mock NextRelease nextRelease;

  @Mock DccFileSystem fs;
  @Mock SubmissionDirectory submissionDirectory;
  @Mock ReleaseFileSystem releaseFileSystem;
  
  @Mock ProjectService projectService;
  @Mock ReleaseService releaseService;  
  // @formatter:off

  SubmissionDirectoryHdfsSshFile directory;
  
  @Before
  public void setUp() throws IOException {
    // Create the simulated project directory
    File root = tmp.newFolder(RELEASE_NAME);
    String projectDirectoryName = "/" + PROJECT_KEY;
    File projectDirectory = new File(root, projectDirectoryName);
    projectDirectory.mkdir();    
    
    // Mock release / project
    when(project.getKey()).thenReturn(PROJECT_KEY);
    when(nextRelease.getRelease()).thenReturn(release);
    when(releaseService.getNextRelease()).thenReturn(nextRelease);
    when(projectService.getProject(PROJECT_KEY)).thenReturn(project);

    // Mock file system
    when(fs.buildReleaseStringPath(release)).thenReturn(root.getAbsolutePath());
    when(fs.getReleaseFilesystem(release)).thenReturn(releaseFileSystem);
    when(fs.getFileSystem()).thenReturn(createFileSystem());
    when(releaseFileSystem.getDccFileSystem()).thenReturn(fs);
    when(releaseFileSystem.getRelease()).thenReturn(release);
    when(releaseFileSystem.getSubmissionDirectory(PROJECT_KEY)).thenReturn(submissionDirectory);
    when(submissionDirectory.isReadOnly()).thenReturn(false);
    when(submissionDirectory.getSubmission()).thenReturn(submission);    
    
    RootHdfsSshFile rootDirectory = new RootHdfsSshFile(releaseFileSystem, projectService, releaseService);
    String directoryName = PROJECT_KEY;
    directory = new SubmissionDirectoryHdfsSshFile(rootDirectory, directoryName);
  }
  
  @Test
  public void testDoesNotExist() throws IOException {
    String fileName = "file.txt";
    FileHdfsSshFile file = new FileHdfsSshFile(directory, fileName);

    assertThat(file.doesExist()).isFalse();
  }
  
  @Test
  public void testCreate() throws IOException {
    String fileName = "file.txt";
    FileHdfsSshFile file = new FileHdfsSshFile(directory, fileName);

    assertThat(file.create()).isTrue();
    assertThat(file.doesExist()).isTrue();
  }

  private static RawLocalFileSystem createFileSystem() {
    RawLocalFileSystem localFileSystem = new RawLocalFileSystem();
    localFileSystem.setConf(new Configuration());

    return localFileSystem;
  }
  
}
