package org.icgc.dcc.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.apache.hadoop.fs.FileSystem;
import org.icgc.dcc.config.ConfigModule;
import org.icgc.dcc.core.CoreModule;
import org.icgc.dcc.core.morphia.MorphiaModule;
import org.icgc.dcc.filesystem.GuiceJUnitRunner.GuiceModules;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.icgc.dcc.http.HttpModule;
import org.icgc.dcc.http.jersey.JerseyModule;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ConfigModule.class, CoreModule.class,//
HttpModule.class, JerseyModule.class,// TODO: find out why those two seem necessary
MorphiaModule.class, FileSystemModule.class })
public class FileSystemFunctionalTest extends FileSystemTest {

  private static final Logger log = LoggerFactory.getLogger(FileSystemFunctionalTest.class);

  protected DccFileSystem dccFileSystem;

  private FileSystem fileSystem;

  @Inject
  public void setFileSystem(FileSystem fileSystem) {
    this.fileSystem = fileSystem;
  }

  @Override
  public void setUp() throws IOException {
    super.setUp();

    this.dccFileSystem = new DccFileSystem(this.mockConfig, this.mockReleases, this.mockProjects, this.fileSystem);
  }

  @Test
  public void test_fileSystem_typicalWorkflow() throws IOException { // TODO: split?
    FileSystem fileSystem = this.dccFileSystem.getFileSystem();

    Iterable<String> filenameList0 =
        HadoopUtils.toFilenameList(HadoopUtils.lsDir(fileSystem, this.dccFileSystem.getRootStringPath()));
    Assert.assertNotNull(filenameList0);
    Assert.assertEquals(//
        "[]",//
        filenameList0.toString());
    log.info("ls0 = " + filenameList0);

    this.dccFileSystem.ensureReleaseFilesystem(this.mockRelease);

    Iterable<String> filenameList1 =
        HadoopUtils.toFilenameList(HadoopUtils.lsDir(fileSystem, this.dccFileSystem.getRootStringPath()));
    Assert.assertNotNull(filenameList1);
    Assert.assertEquals(//
        "[ICGC4]",//
        filenameList1.toString());
    log.info("ls1 = " + filenameList1);

    String releaseStringPath = this.dccFileSystem.buildReleaseStringPath(this.mockRelease);
    log.info("releaseStringPath = " + releaseStringPath);

    Iterable<String> filenameList2 = HadoopUtils.toFilenameList(HadoopUtils.lsDir(fileSystem, releaseStringPath));
    Assert.assertNotNull(filenameList2);
    Assert.assertEquals(//
        "[DBQ]",//
        filenameList2.toString());
    log.info("ls2 = " + filenameList2);

    log.info("ls = " + filenameList0);
    ReleaseFileSystem myReleaseFilesystem = this.dccFileSystem.getReleaseFilesystem(this.mockRelease, this.mockUser);
    Assert.assertNotNull(myReleaseFilesystem);
    log.info("release file system = " + myReleaseFilesystem);

    Iterable<SubmissionDirectory> mySubmissionDirectoryList = myReleaseFilesystem.listSubmissionDirectory();
    Assert.assertNotNull(myReleaseFilesystem);

    int size = ((ArrayList<SubmissionDirectory>) mySubmissionDirectoryList).size();
    Assert.assertEquals(1, size);

    log.info("mySubmissionDirectoryList # = " + size);

    boolean releaseReadOnly = myReleaseFilesystem.isReadOnly();
    Assert.assertFalse(releaseReadOnly);
    log.info("release read only = " + releaseReadOnly);

    SubmissionDirectory mySubmissionDirectory = myReleaseFilesystem.getSubmissionDirectory(this.mockProject);
    Assert.assertNotNull(mySubmissionDirectory);

    boolean submissionReadOnly = mySubmissionDirectory.isReadOnly();
    Assert.assertTrue(submissionReadOnly);
    log.info("submission read only = " + submissionReadOnly);

    InputStream in1 =
        ByteStreams.newInputStreamSupplier("header1\theader2\theader3\na\tb\tc\nd\te\tf\tg\n".getBytes()).getInput();
    String filepath1 = mySubmissionDirectory.addFile(FILENAME_1, in1);
    boolean exists1 = HadoopUtils.checkExistence(fileSystem, filepath1);
    Assert.assertTrue(exists1);
    log.info("added file = " + filepath1);

    InputStream in2 =
        ByteStreams.newInputStreamSupplier("header9\theader8\theader7\nz\tb\ty\nx\tw\tv\tu\n".getBytes()).getInput();
    String filepath2 = mySubmissionDirectory.addFile(FILENAME_2, in2);
    boolean exists2 = HadoopUtils.checkExistence(fileSystem, filepath2);
    Assert.assertTrue(exists2);
    log.info("added file = " + filepath2);

    Iterable<String> fileList1 = mySubmissionDirectory.listFile();
    Assert.assertNotNull(fileList1);
    Assert.assertTrue(Iterables.contains(fileList1, "cnsm__bla__bla__s__bla__bla.tsv"));
    Assert.assertTrue(Iterables.contains(fileList1, "cnsm__bla__bla__p__bla__bla.tsv"));
    log.info("ls1 = " + fileList1);

    Iterable<String> fileList2 = mySubmissionDirectory.listFile(Pattern.compile(".*__p__.*"));
    Assert.assertNotNull(fileList2);
    Assert.assertEquals(//
        "[cnsm__bla__bla__p__bla__bla.tsv]",//
        fileList2.toString());
    log.info("ls2 = " + fileList2);

    mySubmissionDirectory.deleteFile(FILENAME_1);

    Iterable<String> fileList3 = mySubmissionDirectory.listFile();
    Assert.assertNotNull(fileList3);
    Assert.assertEquals(//
        "[cnsm__bla__bla__s__bla__bla.tsv]",//
        fileList3.toString());
    log.info("ls3 = " + fileList3);
  }

  @After
  public void tearDown() {
    HadoopUtils.rmr(this.fileSystem, this.dccFileSystem.buildReleaseStringPath(this.mockRelease));
  }
}
