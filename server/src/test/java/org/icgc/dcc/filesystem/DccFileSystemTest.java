package org.icgc.dcc.filesystem;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.Test;

public class DccFileSystemTest extends FileSystemTest {

  private FileSystem mockFileSystem;

  private DccFileSystem dccFileSystem;

  @Override
  public void setUp() throws IOException {
    super.setUp();

    this.mockFileSystem = mock(FileSystem.class);

    when(this.mockFileSystem.mkdirs(any(Path.class))).thenReturn(true);
    when(this.mockFileSystem.listStatus(any(Path.class))).thenReturn(new FileStatus[] {});

    this.dccFileSystem = new DccFileSystem(this.mockConfig, this.mockProjects, this.mockFileSystem);
  }

  @Test
  public void test_ensureReleaseFilesystem_handlesUnexistingDirectory() throws IOException {
    when(this.mockFileSystem.exists(any(Path.class)))//
        .thenReturn(false).thenReturn(false).thenReturn(true); // did not exist, still doesn't exist, exists now
    this.dccFileSystem.ensureReleaseFilesystem(this.mockRelease);
    verify(this.mockFileSystem).listStatus(new Path("/tmp/my_root_fs_dir/" + this.mockRelease.getName()));
  }

  @Test
  public void test_ensureReleaseFilesystem_handlesExistingDirectory() throws IOException {
    when(this.mockFileSystem.exists(any(Path.class))).thenReturn(true).thenReturn(true); // existed before, still exists
    this.dccFileSystem.ensureReleaseFilesystem(this.mockRelease);
  }
}
