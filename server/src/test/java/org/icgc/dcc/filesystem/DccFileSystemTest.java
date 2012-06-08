package org.icgc.dcc.filesystem;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.hadoop.fs.FileSystem;
import org.hsqldb.lib.StringInputStream;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.icgc.dcc.model.Project;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.User;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DccFileSystemTest {
  private static final Logger log = LoggerFactory.getLogger(DccFileSystem.class);

  private DccFileSystem dccFileSystem;

  // TODO: proper tests
  @Test
  public void testIt() throws Exception {
    FileSystem fileSystem = this.dccFileSystem.getFileSystem();

    log.info("ls = " + HadoopUtils.toFilenameList(HadoopUtils.ls(fileSystem, this.dccFileSystem.getRootStringPath())));

    Release myRelease = new Release("ICGC4");

    User myUser = new User();
    myUser.setUsername("vegeta");

    Project myProject = new Project("dragon_balls_quest");

    this.dccFileSystem.ensureReleaseFilesystem(myRelease);

    ReleaseFileSystem myReleaseFilesystem = this.dccFileSystem.getReleaseFilesystem(myRelease, myUser);
    log.info("release file system = " + myReleaseFilesystem);

    Iterable<SubmissionDirectory> mySubmissionDirectoryList = myReleaseFilesystem.listSubmissionDirectory();
    log.info("mySubmissionDirectoryList # = " + ((ArrayList<SubmissionDirectory>) mySubmissionDirectoryList).size());
    log.info("read only = " + myReleaseFilesystem.isReadOnly());

    SubmissionDirectory mySubmissionDirectory = myReleaseFilesystem.getSubmissionDirectory(myProject);

    String filename1 = "cnsm__bla__bla__p__bla__bla.tsv";
    InputStream in1 = new StringInputStream("header1\theader2\theader3\na\tb\tc\nd\te\tf\tg\n");
    String filepath1 = mySubmissionDirectory.addFile(filename1, in1);
    HadoopUtils.checkExistence(fileSystem, filepath1);
    log.info("added file = " + filepath1);

    String filename2 = "cnsm__bla__bla__s__bla__bla.tsv";
    InputStream in2 = new StringInputStream("header9\theader8\theader7\nz\tb\ty\nx\tw\tv\tu\n");
    String filepath2 = mySubmissionDirectory.addFile(filename2, in2);
    HadoopUtils.checkExistence(fileSystem, filepath2);
    log.info("added file = " + filepath2);

    Iterable<String> fileList1 = mySubmissionDirectory.listFile();
    log.info("ls1 = " + fileList1);

    Iterable<String> fileList2 = mySubmissionDirectory.listFile(Pattern.compile(".*__p__.*"));
    log.info("ls2 = " + fileList2);

    mySubmissionDirectory.deleteFile(filename1);

    HadoopUtils.rmr(fileSystem, this.dccFileSystem.buildReleaseStringPath(myRelease));
  }
}
