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

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.hadoop.fs.Path;
import org.apache.shiro.subject.Subject;
import org.apache.sshd.server.FileSystemView;
import org.apache.sshd.server.SshFile;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.DccFileSystemException;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;

/**
 * Bridge between the SSHd SftpModule and the DCC file system
 */
public class HdfsFileSystemView implements FileSystemView {

  private final DccFileSystem dccFileSystem;

  private final ProjectService projectService;

  private final ReleaseService releaseService;

  private final UsernamePasswordAuthenticator passwordAuthenticator;

  public HdfsFileSystemView(DccFileSystem dccFileSystem, ProjectService projectService, ReleaseService releaseService,
      UsernamePasswordAuthenticator passwordAuthenticator) {
    this.dccFileSystem = dccFileSystem;
    this.projectService = projectService;
    this.releaseService = releaseService;
    this.passwordAuthenticator = passwordAuthenticator;
  }

  /**
   * Get file object.
   * @param file The path to the file to get
   * @return The {@link SshFile} for the provided path
   */
  @Override
  public SshFile getFile(String file) {
    Path filePath = getFilePath(file);
    Subject currentSubject = this.passwordAuthenticator.getSubject();
    Release curRelease = this.releaseService.getNextRelease().getRelease();
    ReleaseFileSystem rfs = this.dccFileSystem.getReleaseFilesystem(curRelease, currentSubject);
    RootHdfsSshFile root = new RootHdfsSshFile(rfs, this.projectService, this.releaseService);

    switch(filePath.depth()) {
    case 0:
      return root;
    case 1:
      return this.getHdfsSshFile(rfs, root, filePath);
    case 2:
      Path parentDirPath = filePath.getParent();
      return new FileHdfsSshFile(this.getHdfsSshFile(rfs, root, parentDirPath), filePath.getName());
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
    Path filePath = getFilePath(file);

    if(baseDir instanceof HdfsSshFile) {
      return ((HdfsSshFile) baseDir).getChild(filePath);
    }
    throw new IllegalStateException("Invalid SshFile: " + baseDir.toString());
  }

  private Path getFilePath(String file) {
    checkNotNull(file);
    file = (file.isEmpty() || file.equals(".")) ? "/" : file;
    Path filePath = new Path(file);
    return filePath;
  }

  private BaseDirectoryHdfsSshFile getHdfsSshFile(ReleaseFileSystem rfs, RootHdfsSshFile root, Path path) {
    BaseDirectoryHdfsSshFile result;
    if(rfs.isSystemDirectory(path)) {
      result = new SystemFileHdfsSshFile(root, path.getName(), this.releaseService);
    } else {
      result = new SubmissionDirectoryHdfsSshFile(root, path.getName(), this.releaseService);
    }
    return result;
  }
}
