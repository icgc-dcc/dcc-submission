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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.junit.Test;

import com.google.common.collect.Sets;

public class DccFileSystemTest extends FileSystemTest {

  private FileSystem mockFileSystem;

  private DccFileSystem dccFileSystem;

  @Override
  public void setUp() throws IOException {
    super.setUp();

    this.mockFileSystem = mock(FileSystem.class);

    when(this.mockFileSystem.mkdirs(any(Path.class))).thenReturn(true);
    when(this.mockFileSystem.listStatus(any(Path.class))).thenReturn(new FileStatus[] {});

    this.dccFileSystem = new DccFileSystem(this.mockConfig, this.mockFileSystem);
  }

  @Test
  public void test_ensureReleaseFilesystem_handlesUnexistingDirectory() throws IOException {
    when(this.mockFileSystem.exists(any(Path.class)))//
        .thenReturn(false).thenReturn(false).thenReturn(true); // did not exist, still doesn't exist, exists now
    this.dccFileSystem.ensureReleaseFilesystem(this.mockRelease, Sets.newHashSet(this.mockProject.getKey()));
  }

  @Test
  public void test_ensureReleaseFilesystem_handlesExistingDirectory() throws IOException {
    when(this.mockFileSystem.exists(any(Path.class))).thenReturn(true).thenReturn(true); // existed before, still exists
    this.dccFileSystem.ensureReleaseFilesystem(this.mockRelease, Sets.newHashSet(this.mockProject.getKey()));
  }
}
