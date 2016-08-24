/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.sftp.fs;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.submission.sftp.fs.HdfsFileUtils.handleException;

import java.io.FileNotFoundException;

import org.apache.hadoop.fs.Path;
import org.apache.sshd.common.file.FileSystemView;
import org.apache.sshd.common.file.SshFile;
import org.icgc.dcc.submission.sftp.SftpContext;
import org.springframework.security.core.Authentication;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * Virtual file system that bridges the SSHD SftpModule and the DCC file system
 */
@RequiredArgsConstructor
public class HdfsFileSystemView implements FileSystemView {

  @NonNull
  private final SftpContext context;
  @NonNull
  private final Authentication authentication;

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
      RootHdfsSshFile root = new RootHdfsSshFile(context, authentication);

      switch (filePath.depth()) {
      case 0:
        return root;
      case 1:
        return getSubmissionDirectory(file, filePath, root);
      case 2:
        return getSubmissionFile(file, filePath, root);
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

  private BaseDirectoryHdfsSshFile getSubmissionDirectory(String file, Path path, RootHdfsSshFile root) {
    val submissionDirectory = getHdfsSshFile(root, path);

    return submissionDirectory;
  }

  private SshFile getSubmissionFile(String file, Path path, RootHdfsSshFile root) {
    val submissionDirectory = getSubmissionDirectory(file, path.getParent(), root);
    val submissionFileName = path.getName();
    val submissionFile = new FileHdfsSshFile(context, submissionDirectory, submissionFileName);

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

  private BaseDirectoryHdfsSshFile getHdfsSshFile(RootHdfsSshFile root, Path path) {
    BaseDirectoryHdfsSshFile result;
    if (context.isSystemDirectory(path, authentication) && context.isAdminUser(authentication)) {
      result = new SystemFileHdfsSshFile(context, root, path.getName());
    } else { // FIXME? What happens if is-system-dir but not is-admin...?
      result = new SubmissionDirectoryHdfsSshFile(context, root, path.getName());
    }

    return result;
  }

  @Override
  public FileSystemView getNormalizedView() {
    return this;
  }

}
