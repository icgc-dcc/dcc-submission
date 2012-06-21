/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.sftp;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.sshd.server.SshFile;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.filesystem.SubmissionDirectory;

/**
 * 
 */
public class FileHdfsSshFile extends BaseHdfsSshFile {

  private final SubmissionDirectory directory;

  private final ReleaseFileSystem releaseFileSystem;

  public FileHdfsSshFile(Path path, FileSystem fs, SubmissionDirectory directory, ReleaseFileSystem releaseFileSystem) {
    super(path, fs);
    this.directory = directory;
    this.releaseFileSystem = releaseFileSystem;
  }

  @Override
  public String getAbsolutePath() {
    return SEPARATOR + directory.getProjectKey() + SEPARATOR + path.getName();
  }

  @Override
  public String getName() {
    return this.path.getName();
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public boolean isFile() {
    return true;
  }

  @Override
  public boolean isWritable() {
    if(directory.isReadOnly()) {
      return false;
    }
    return super.isWritable();
  }

  @Override
  public SshFile getParentFile() {
    return new DirectoryHdfsSshFile(this.path.getParent(), this.fs, this.directory, this.releaseFileSystem);
  }

  @Override
  public boolean mkdir() {
    return false;
  }

  @Override
  public boolean create() throws IOException {
    if(isWritable()) {
      this.fs.create(path);
      return true;
    }
    return false;
  }

  @Override
  public void truncate() throws IOException {
    create();
  }

  @Override
  public boolean move(SshFile destination) {
    try {
      return this.fs.rename(path, new Path(destination.getAbsolutePath()));
    } catch(IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  @Override
  public List<SshFile> listSshFiles() {
    return null;
  }
}
