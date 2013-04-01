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
import org.junit.Test;

import com.google.common.io.ByteStreams;

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

    when(this.mockDccFileSystem.buildProjectStringPath(this.mockRelease, PROJECT_KEY)).thenReturn(ROOT_DIR);
    when(this.mockDccFileSystem.buildFileStringPath(this.mockRelease, PROJECT_KEY, FILENAME_1)).thenReturn(FILEPATH_1);
    when(this.mockDccFileSystem.getFileSystem()).thenReturn(this.mockFileSystem);

    this.submissionDirectory =
        new SubmissionDirectory(this.mockDccFileSystem, this.mockRelease, PROJECT_KEY, this.mockSubmission);
  }

  @Test
  public void test_addFile_addFile() throws IOException {
    this.submissionDirectory.addFile(FILENAME_1,
        ByteStreams.newInputStreamSupplier("header1\theader2\theader3\na\tb\tc\nd\te\tf\tg\n".getBytes()).getInput());

    String ls = this.submissionDirectory.listFile().toString();
    Assert.assertEquals("[]", ls);// TODO: not very useful...
  }
}
