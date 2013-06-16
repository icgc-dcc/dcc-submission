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
import static com.google.common.base.Throwables.propagateIfInstanceOf;

import java.io.FileNotFoundException;
import java.io.IOException;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

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
 * Bridge between the SSHD SftpModule and the DCC file system
 */
@Slf4j
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
    try {
      Path filePath = getFilePath(file);
      Subject subject = this.passwordAuthenticator.getSubject();
      Release release = this.releaseService.getNextRelease().getRelease();
      ReleaseFileSystem rfs = this.dccFileSystem.getReleaseFilesystem(release, subject);
      RootHdfsSshFile root = new RootHdfsSshFile(rfs, this.projectService, this.releaseService);

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

      if (baseDir instanceof HdfsSshFile) {
        return ((HdfsSshFile) baseDir).getChild(filePath);
      }

      throw new IllegalStateException("Invalid SshFile: " + baseDir.toString());
    } catch (Exception e) {
      return handleException(SshFile.class, e);
    }
  }

  private BaseDirectoryHdfsSshFile getSubmissionDirectory(String file, Path path, ReleaseFileSystem rfs,
      RootHdfsSshFile root) throws FileNotFoundException {
    BaseDirectoryHdfsSshFile submissionDirectory = getHdfsSshFile(rfs, root, path);
    if (!submissionDirectory.doesExist()) {
      throw new FileNotFoundException("Invalid file path: " + file);
    }

    return submissionDirectory;
  }

  private SshFile getSubmissionFile(String file, Path path, ReleaseFileSystem rfs, RootHdfsSshFile root)
      throws FileNotFoundException {
    BaseDirectoryHdfsSshFile submissionDirectory = getSubmissionDirectory(file, path.getParent(), rfs, root);
    String submissionFileName = path.getName();
    FileHdfsSshFile submissionFile = new FileHdfsSshFile(submissionDirectory, submissionFileName);

    if (!submissionFile.doesExist()) {
      new FileNotFoundException("Invalid file path: " + file);
    }

    return submissionFile;
  }

  private Path getFilePath(String file) {
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

  private BaseDirectoryHdfsSshFile getHdfsSshFile(ReleaseFileSystem rfs, RootHdfsSshFile root, Path path) {
    BaseDirectoryHdfsSshFile result;
    if (rfs.isSystemDirectory(path)) {
      result = new SystemFileHdfsSshFile(root, path.getName());
    } else {
      result = new SubmissionDirectoryHdfsSshFile(root, path.getName());
    }

    return result;
  }

  /**
   * Apache MINA exception handling method designed to evade Java's checked exception mechanism to propagate
   * {@code IOException}s to avoid terminating MINA SFTP sessions.
   * 
   * @param type - the return type
   * @param e - the exception to propagate
   * @return nothing
   */
  @SneakyThrows
  protected <T> T handleException(Class<T> type, Exception e) {
    log.warn("SFTP user triggered exception: {}", e.getMessage());
    propagateIfInstanceOf(e, IOException.class);
    throw new IOException(e);
  }

}
