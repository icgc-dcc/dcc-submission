package org.icgc.dcc.filesystem;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import junit.framework.Assert;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.hsqldb.lib.StringInputStream;
import org.junit.Test;

public class SubmissionDirectoryTest extends FileSystemTest {

  private FSDataOutputStream mockFSDataOutputStream;

  private FileSystem mockFileSystem;

  private DccFileSystem mockDccFileSystem;

  private SubmissionDirectory submissionDirectory;

  @Override
  public void setUp() throws IOException {
    super.setUp();

    this.mockFSDataOutputStream = mock(FSDataOutputStream.class);
    this.mockFileSystem = mock(FileSystem.class);
    this.mockDccFileSystem = mock(DccFileSystem.class);

    when(this.mockFileSystem.mkdirs(any(Path.class))).thenReturn(true);
    when(this.mockFileSystem.listStatus(any(Path.class))).thenReturn(new FileStatus[] {});
    when(this.mockFileSystem.create(any(Path.class))).thenReturn(this.mockFSDataOutputStream);

    when(this.mockDccFileSystem.buildProjectStringPath(this.mockRelease, this.mockProject)).thenReturn(ROOT_DIR);
    when(this.mockDccFileSystem.buildFilepath(this.mockRelease, this.mockProject, FILENAME_1)).thenReturn(FILEPATH_1);
    when(this.mockDccFileSystem.getFileSystem()).thenReturn(this.mockFileSystem);

    this.submissionDirectory = new SubmissionDirectory(this.mockDccFileSystem, this.mockRelease, this.mockProject);
  }

  @Test
  public void test_addFile_addFile() throws IOException {
    this.submissionDirectory.addFile(FILENAME_1, new StringInputStream(
        "header1\theader2\theader3\na\tb\tc\nd\te\tf\tg\n"));

    String ls = this.submissionDirectory.listFile().toString();
    Assert.assertEquals("[]", ls);// TODO: not very useful...
  }
}
