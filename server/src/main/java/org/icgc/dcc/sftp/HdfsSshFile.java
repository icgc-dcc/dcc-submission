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
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.sshd.server.SshFile;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
abstract class HdfsSshFile implements SshFile {

  protected static final Logger log = LoggerFactory.getLogger(HdfsSshFile.class);

  protected final String SEPARATOR = "/";

  protected Path path;

  protected final FileSystem fs;

  protected HdfsSshFile(Path path, FileSystem fs) {
    this.path = path;
    this.fs = fs;
  }

  protected HdfsSshFile(ReleaseFileSystem rfs) {
    DccFileSystem dccFS = rfs.getDccFileSystem();
    this.path = new Path(dccFS.buildReleaseStringPath(rfs.getRelease()));
    this.fs = dccFS.getFileSystem();
  }

  @Override
  public boolean doesExist() {
    try {
      return this.fs.exists(path);
    } catch(IOException e) {
      log.error("File system error", e);
      return false;
    }
  }

  @Override
  public boolean isReadable() {
    FsAction u;
    try {
      u = fs.getFileStatus(path).getPermission().getUserAction();
    } catch(IOException e) {
      log.error("File system error", e);
      return false;
    }
    return (u == FsAction.ALL || u == FsAction.READ_WRITE || u == FsAction.READ || u == FsAction.READ_EXECUTE);
  }

  @Override
  public boolean isWritable() {
    FsAction u;
    try {
      u = fs.getFileStatus(path).getPermission().getUserAction();
    } catch(IOException e) {
      log.error("File system error", e);
      return false;
    }
    return (u == FsAction.ALL || u == FsAction.READ_WRITE || u == FsAction.WRITE || u == FsAction.WRITE_EXECUTE);
  }

  @Override
  public boolean isExecutable() {
    return false;
  }

  @Override
  public boolean isRemovable() {
    return false;
  }

  @Override
  public long getLastModified() {
    try {
      return this.fs.getFileStatus(path).getModificationTime();
    } catch(IOException e) {
      log.error("File system error", e);
      return 0;
    }
  }

  @Override
  public boolean setLastModified(long time) {
    try {
      this.fs.setTimes(path, time, -1);
      return true;
    } catch(IOException e) {
      log.error("File system error", e);
    }
    return false;
  }

  @Override
  public long getSize() {
    try {
      return fs.getFileStatus(path).getLen();
    } catch(IOException e) {
      log.error("File system error", e);
    }
    return 0;
  }

  @Override
  public boolean mkdir() {
    return false;
  }

  @Override
  public boolean delete() {
    return false;
  }

  @Override
  public boolean move(SshFile destination) {
    return false;
  }

  @Override
  public OutputStream createOutputStream(long offset) throws IOException {
    if(this.isWritable() == false) {
      throw new IOException("SFTP is in readonly mode");
    }
    return fs.create(path);
  }

  @Override
  public InputStream createInputStream(long offset) throws IOException {
    // because of DCC-412, the download size bug, we will temporarily disable download
    // return fs.open(path);
    throw new UnsupportedOperationException("disabled download from SFTP");
  }

  @Override
  public void handleClose() throws IOException {

  }

  public abstract HdfsSshFile getChild(Path filePath);
}
