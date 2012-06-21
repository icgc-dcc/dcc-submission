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

import org.apache.hadoop.fs.Path;
import org.apache.sshd.server.FileSystemView;
import org.apache.sshd.server.SshFile;
import org.icgc.dcc.filesystem.DccFileSystemException;
import org.icgc.dcc.filesystem.ReleaseFileSystem;

/**
 * Bridge between the SSHd SftpModule and the DCC file system
 */
public class HdfsFileSystemView implements FileSystemView {

  private final ReleaseFileSystem rfs;

  public HdfsFileSystemView(ReleaseFileSystem rfs) {
    this.rfs = rfs;
  }

  /**
   * Get file object.
   * @param file The path to the file to get
   * @return The {@link SshFile} for the provided path
   */
  @Override
  public SshFile getFile(String file) {
    Path filePath = new Path(file);

    RootHdfsSshFile root = new RootHdfsSshFile(rfs);

    switch(filePath.depth()) {
    case 0:
      return root;
    case 1:
      return new DirectoryHdfsSshFile(root, filePath.getName());
    case 2:
      DirectoryHdfsSshFile parentDir = new DirectoryHdfsSshFile(root, filePath.getParent().getName());
      return new FileHdfsSshFile(parentDir, filePath.getName());
    default:
      throw new DccFileSystemException("Invalid file path: " + file);
    }
  }

  /**
   * Get file object.
   * @param baseDir The reference towards which the file should be resolved
   * @param file The path to the file to get
   * @return The {@link SshFile} for the provided path
   */
  @Override
  public SshFile getFile(SshFile baseDir, String file) {
    Path filePath = new Path(file);

    if(baseDir instanceof RootHdfsSshFile) {
      RootHdfsSshFile root = (RootHdfsSshFile) baseDir;
      switch(filePath.depth()) {
      case 0:
        return root;
      case 1:
        return new DirectoryHdfsSshFile(root, filePath.getName());
      case 2:
        DirectoryHdfsSshFile parentDir = new DirectoryHdfsSshFile(root, filePath.getParent().getName());
        return new FileHdfsSshFile(parentDir, filePath.getName());
      }
    }

    if(baseDir instanceof DirectoryHdfsSshFile) {
      DirectoryHdfsSshFile parentDir = (DirectoryHdfsSshFile) baseDir;
      switch(filePath.depth()) {
      case 0:
        return parentDir;
      case 1:
        return new FileHdfsSshFile(parentDir, filePath.getName());
      }
    }
    throw new DccFileSystemException("Invalid file path: " + baseDir.getAbsolutePath() + file);
  }
}
