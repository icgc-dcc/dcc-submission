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
package org.icgc.dcc.sftp.fs;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.sftp.fs.HdfsFileUtils.handleException;

import java.io.FileNotFoundException;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.apache.hadoop.fs.Path;
import org.apache.shiro.subject.Subject;
import org.apache.sshd.server.FileSystemView;
import org.apache.sshd.server.SshFile;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.security.UsernamePasswordAuthenticator;

/**
 * Virtual file system that bridges the SSHD SftpModule and the DCC file system
 */
@RequiredArgsConstructor
public class HdfsFileSystemView implements FileSystemView {

  @NonNull
  private final DccFileSystem dccFileSystem;
  @NonNull
  private final ProjectService projectService;
  @NonNull
  private final ReleaseService releaseService;
  @NonNull
  private final UsernamePasswordAuthenticator passwordAuthenticator;

  /**
   * Returns the appropriate file system abstraction for the specified {@code file} path.
   * 
   * @param file - the path to the file to get
   * @return the {@link SshFile} for the provided file path
   */
  @Override
  public SshFile getFile(String file) {
    try {
      Path filePath = getFilePath(file);
      Subject user = passwordAuthenticator.getSubject();
      Release release = releaseService.getNextRelease().getRelease();
      ReleaseFileSystem rfs = dccFileSystem.getReleaseFilesystem(release, user);
      RootHdfsSshFile root = new RootHdfsSshFile(rfs, projectService, releaseService);

      switch (filePath.depth()) {
      case 0:
        return root;
      case 1:
        return getSubmissionDirectory(file, filePath, rfs, root);
      case 2:
        return getSubmissionFile(file, filePath, rfs, root);
      default:
        throw new FileNotFoundException("Invalid file path: " + file);
      }
    } catch (Exception e) {
      return handleException(SshFile.class, e);
    }
  }

  /**
   * Get file object.
   * 
   * @param baseDir The reference towards which the file should be resolved
   * @param file The path to the file to get
   * @return The {@link SshFile} for the provided path
   */
  @Override
  public SshFile getFile(SshFile baseDir, String file) {
    try {
      Path filePath = getFilePath(file);
      checkState(baseDir instanceof HdfsSshFile, "Invalid SshFile: %s", baseDir);
      return ((HdfsSshFile) baseDir).getChild(filePath);
    } catch (Exception e) {
      return handleException(SshFile.class, e);
    }
  }

  private static BaseDirectoryHdfsSshFile getSubmissionDirectory(String file, Path path, ReleaseFileSystem rfs,
      RootHdfsSshFile root) throws FileNotFoundException {
    BaseDirectoryHdfsSshFile submissionDirectory = getHdfsSshFile(rfs, root, path);
    if (!submissionDirectory.doesExist()) {
      throw new FileNotFoundException("Invalid file path: " + file);
    }

    return submissionDirectory;
  }

  private static SshFile getSubmissionFile(String file, Path path, ReleaseFileSystem rfs, RootHdfsSshFile root)
      throws FileNotFoundException {
    BaseDirectoryHdfsSshFile submissionDirectory = getSubmissionDirectory(file, path.getParent(), rfs, root);
    String submissionFileName = path.getName();
    FileHdfsSshFile submissionFile = new FileHdfsSshFile(submissionDirectory, submissionFileName);

    if (!submissionFile.doesExist()) {
      new FileNotFoundException("Invalid file path: " + file);
    }

    return submissionFile;
  }

  private static Path getFilePath(String file) {
    checkNotNull(file);

    if (file.endsWith("/.")) {
      // Fix for DCC-1071: Remove trailing "/."
      file = file.substring(0, file.length() - 2);
    }
    if (file.equals("~") || file.endsWith("/~")) {
      // Fix for DCC-1071: Alias to root since "home" is ambiguous in general
      file = "/";
    }
    if (file.isEmpty() || file.equals(".")) {
      // Alias
      file = "/";
    }

    Path filePath = new Path(file);

    return filePath;
  }

  private static BaseDirectoryHdfsSshFile getHdfsSshFile(ReleaseFileSystem rfs, RootHdfsSshFile root, Path path) {
    BaseDirectoryHdfsSshFile result;
    if (rfs.isSystemDirectory(path)) {
      result = new SystemFileHdfsSshFile(root, path.getName());
    } else {
      result = new SubmissionDirectoryHdfsSshFile(root, path.getName());
    }

    return result;
  }

}
