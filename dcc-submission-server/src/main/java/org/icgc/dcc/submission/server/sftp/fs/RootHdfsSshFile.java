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
package org.icgc.dcc.submission.server.sftp.fs;

import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.lsAll;
import static org.icgc.dcc.submission.server.sftp.fs.HdfsFileUtils.SshFileList;
import static org.icgc.dcc.submission.server.sftp.fs.HdfsFileUtils.handleException;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.sshd.common.file.SshFile;
import org.icgc.dcc.submission.server.sftp.SftpContext;
import org.springframework.security.core.Authentication;

import com.google.common.base.Optional;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RootHdfsSshFile extends HdfsSshFile {

  public RootHdfsSshFile(SftpContext context, Authentication authentication) {
    super(context, context.getReleasePath(), context.getFileSystem(), authentication);
  }

  @Override
  public String getAbsolutePath() {
    return SEPARATOR;
  }

  @Override
  public String getName() {
    return "";
  }

  @Override
  public boolean isDirectory() {
    return true;
  }

  @Override
  public boolean isFile() {
    return false;
  }

  @Override
  public boolean isRemovable() {
    return false;
  }

  @Override
  public SshFile getParentFile() {
    return this;
  }

  @Override
  public boolean mkdir() {
    return false;
  }

  @Override
  public boolean create() throws IOException {
    return false;
  }

  @Override
  public boolean move(SshFile destination) {
    return false;
  }

  @Override
  public List<SshFile> listSshFiles() {
    try {
      List<Path> paths = lsAll(fileSystem, path);
      List<SshFile> sshFiles = newArrayList();
      val userProjectKeys = context.getUserProjectKeys(authentication);
      for (Path path : paths) {

        val sshFile = listSshFile(path, userProjectKeys);
        if (sshFile.isPresent()) {
          sshFiles.add(sshFile.get());
        }
      }

      return sshFiles;
    } catch (Exception e) {
      return handleException(SshFileList, e);
    }
  }

  @Override
  public HdfsSshFile getChild(Path filePath) {
    try {
      switch (filePath.depth()) {
      case 0:
        return this;
      case 1:
        return new SubmissionDirectoryHdfsSshFile(context, this, filePath.getName());
      case 2:
        val parentDir = new SubmissionDirectoryHdfsSshFile(context, this, filePath.getParent().getName());
        return new FileHdfsSshFile(context, parentDir, filePath.getName());
      }
    } catch (Exception e) {
      return handleException(HdfsSshFile.class, e);
    }

    return handleException(HdfsSshFile.class, "Invalid file path: %s%s", getAbsolutePath(), filePath.toString());
  }

  @Override
  public void truncate() throws IOException {
    // No-op
  }

  private Optional<SshFile> listSshFile(Path path, List<String> userProjectKeys) {
    try {
      if (context.isSystemDirectory(path, authentication)) {
        if (context.isAdminUser(authentication)) {
          // System file directory and admin user, add to file list
          return Optional.<SshFile> of(new SystemFileHdfsSshFile(context, this, path.getName()));
        } else {
          return Optional.<SshFile> absent();
        }
      } else {
        // Represents a submission directory
        val projectKey = path.getName();
        val viewable = userProjectKeys.contains(projectKey);
        if (!viewable) {
          return Optional.<SshFile> absent();
        }

        val submissionDir = new SubmissionDirectoryHdfsSshFile(context, this, projectKey);
        if (submissionDir.doesExist()) {
          // Necessary because of error handling workaround
          return Optional.<SshFile> of(submissionDir);
        }
      }
    } catch (Exception e) {
      log.warn("Path '{}' skipped due to exception", path.getName(), e.getMessage());
    }

    return Optional.<SshFile> absent();
  }

}
