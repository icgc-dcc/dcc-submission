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

import org.apache.hadoop.fs.Path;
import org.apache.sshd.server.SshFile;
import org.icgc.dcc.filesystem.DccFileSystemException;

/**
 * 
 */
class FileHdfsSshFile extends HdfsSshFile {

  private final DirectoryHdfsSshFile directory;

  FileHdfsSshFile(DirectoryHdfsSshFile directory, String fileName) {
    super(new Path(directory.path, fileName), directory.fs);
    this.directory = directory;
  }

  @Override
  public String getAbsolutePath() {
    return this.directory.getAbsolutePath() + SEPARATOR + path.getName();
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
    if(directory.isWritable() == false) {
      return false;
    }
    if(doesExist()) {
      return super.isWritable();
    }
    return true;
  }

  @Override
  public boolean isRemovable() {
    return isWritable();
  }

  @Override
  public SshFile getParentFile() {
    return directory;
  }

  @Override
  public boolean mkdir() {
    return false;
  }

  @Override
  public boolean create() throws IOException {
    if(isWritable()) {
      this.fs.createNewFile(path);
      this.directory.notifyModified();
      return true;
    }
    return false;
  }

  @Override
  public boolean delete() {
    if(isRemovable()) {
      try {
        boolean success = this.fs.delete(path, false);
        if(success) {
          this.directory.notifyModified();
        }
        return success;
      } catch(IOException e) {
        log.error("File system error", e);
      }
    }
    return false;
  }

  @Override
  public void truncate() throws IOException {
    create();
  }

  @Override
  public boolean move(SshFile destination) {
    if(isWritable()) {
      try {
        boolean success = this.fs.rename(path, new Path(destination.getAbsolutePath()));
        if(success) {
          this.directory.notifyModified();
        }
        return success;
      } catch(IOException e) {
        log.error("File system error", e);
      }
    }
    return false;
  }

  @Override
  public List<SshFile> listSshFiles() {
    return null;
  }

  @Override
  public HdfsSshFile getChild(Path filePath) {
    throw new DccFileSystemException("Invalid file path: " + this.getAbsolutePath() + filePath.toString());
  }
}
