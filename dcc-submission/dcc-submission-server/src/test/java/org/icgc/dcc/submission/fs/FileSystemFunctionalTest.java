/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.fs;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.config.ConfigModule;
import org.icgc.dcc.submission.core.CoreModule;
import org.icgc.dcc.submission.core.morphia.MorphiaModule;
import org.icgc.dcc.submission.fs.GuiceJUnitRunner.GuiceModules;
import org.icgc.dcc.submission.fs.hdfs.HadoopUtils;
import org.icgc.dcc.submission.http.HttpModule;
import org.icgc.dcc.submission.http.jersey.JerseyModule;
import org.icgc.dcc.submission.shiro.ShiroModule;
import org.icgc.dcc.submission.shiro.ShiroPasswordAuthenticator;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ConfigModule.class, CoreModule.class,//
HttpModule.class, JerseyModule.class,// TODO: find out why those two seem necessary
MorphiaModule.class, FileSystemModule.class, ShiroModule.class })
public class FileSystemFunctionalTest extends FileSystemTest {

  private static final Logger log = LoggerFactory.getLogger(FileSystemFunctionalTest.class);

  protected DccFileSystem dccFileSystem;

  private FileSystem fileSystem;

  private ShiroPasswordAuthenticator passwordAuthenticator;

  @Inject
  public void setFileSystem(FileSystem fileSystem, ShiroPasswordAuthenticator passwordAuthenticator) {
    this.fileSystem = fileSystem;
    this.passwordAuthenticator = passwordAuthenticator;
  }

  @Override
  public void setUp() throws IOException {
    super.setUp();

    this.dccFileSystem = new DccFileSystem(this.mockConfig, this.fileSystem);
  }

  @Test
  public void test_fileSystem_typicalWorkflow() throws IOException { // TODO: split?

    FileSystem fileSystem = this.dccFileSystem.getFileSystem();

    Iterable<String> filenameList0 =
        HadoopUtils.toFilenameList(HadoopUtils.lsDir(fileSystem, new Path(this.dccFileSystem.getRootStringPath())));
    Assert.assertNotNull(filenameList0);
    Assert.assertEquals(//
        "[]",//
        filenameList0.toString());
    log.info("ls0 = " + filenameList0);

    this.dccFileSystem.ensureReleaseFilesystem(this.mockRelease, Sets.newHashSet(this.mockProject.getKey()));

    Iterable<String> filenameList1 =
        HadoopUtils.toFilenameList(HadoopUtils.lsDir(fileSystem, new Path(this.dccFileSystem.getRootStringPath())));
    Assert.assertNotNull(filenameList1);
    Assert.assertEquals(//
        "[ICGC4]",//
        filenameList1.toString());
    log.info("ls1 = " + filenameList1);

    String releaseStringPath = this.dccFileSystem.buildReleaseStringPath(this.mockRelease);
    log.info("releaseStringPath = " + releaseStringPath);

    Iterable<String> filenameList2 =
        HadoopUtils.toFilenameList(HadoopUtils.lsDir(fileSystem, new Path(releaseStringPath)));
    assertThat(filenameList2).isNotNull().contains("DBQ", "SystemFiles");
    log.info("ls2 = " + filenameList2);

    log.info("ls = " + filenameList0);

    this.passwordAuthenticator.authenticate("admin", "adminspasswd".toCharArray(), null);

    ReleaseFileSystem myReleaseFilesystem =
        this.dccFileSystem.getReleaseFilesystem(this.mockRelease, this.passwordAuthenticator.getSubject());
    Assert.assertNotNull(myReleaseFilesystem);
    log.info("release file system = " + myReleaseFilesystem);

    myReleaseFilesystem.emptyValidationFolders();

    boolean releaseReadOnly = myReleaseFilesystem.isReadOnly();
    Assert.assertFalse(releaseReadOnly);
    log.info("release read only = " + releaseReadOnly);

    SubmissionDirectory mySubmissionDirectory = myReleaseFilesystem.getSubmissionDirectory(PROJECT_KEY);
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
