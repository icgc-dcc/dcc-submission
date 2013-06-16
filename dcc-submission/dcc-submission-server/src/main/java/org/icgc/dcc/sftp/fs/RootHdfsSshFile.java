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

import static org.icgc.dcc.filesystem.hdfs.HadoopUtils.lsAll;
import static org.icgc.dcc.sftp.fs.HdfsFileUtils.SshFileList;
import static org.icgc.dcc.sftp.fs.HdfsFileUtils.handleException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.apache.sshd.server.SshFile;
import org.icgc.dcc.core.ProjectServiceException;
import org.icgc.dcc.filesystem.DccFileSystemException;
import org.icgc.dcc.filesystem.SubmissionDirectory;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.sftp.SftpContext;

@Slf4j
public class RootHdfsSshFile extends HdfsSshFile {

  private final SftpContext context;

  public RootHdfsSshFile(SftpContext context) {
    super(context.getReleasePath(), context.getFileSystem());
    this.context = context;
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
      List<Path> pathList = lsAll(fs, path);
      List<SshFile> sshFileList = new ArrayList<SshFile>();
      for (Path path : pathList) {
        try {
          // if it is System File directory and admin user, add to file list
          if (context.isSystemDirectory(path)) {
            sshFileList.add(new SystemFileHdfsSshFile(this, path.getName()));
          } else {
            SubmissionDirectoryHdfsSshFile dir = new SubmissionDirectoryHdfsSshFile(this, path.getName());
            if (dir.doesExist()) { // Necessary because of error handling workaround
              sshFileList.add(dir);
            }
          }
        } catch (DccFileSystemException e) {
          log.info("Directory skipped due to insufficient permissions: " + path.getName());
        } catch (ProjectServiceException e) {
          log.info("Skipped due to no corresponding project: " + path.getName());
        }
      }

      return sshFileList;
    } catch (Exception e) {
      return handleException(SshFileList, e);
    }
  }

  public SubmissionDirectory getSubmissionDirectory(String directoryName) {
    try {
      try {
        return context.getSubmissionDirectory(directoryName);
      } catch (ProjectServiceException e) {
        throw new FileNotFoundException(directoryName);
      }
    } catch (Exception e) {
      return handleException(SubmissionDirectory.class, e);
    }
  }

  @Override
  public HdfsSshFile getChild(Path filePath) {
    try {
      switch (filePath.depth()) {
      case 0:
        return this;
      case 1:
        return new SubmissionDirectoryHdfsSshFile(this, filePath.getName());
      case 2:
        SubmissionDirectoryHdfsSshFile parentDir =
            new SubmissionDirectoryHdfsSshFile(this, filePath.getParent().getName());
        return new FileHdfsSshFile(parentDir, filePath.getName());
      }
    } catch (Exception e) {
      return handleException(HdfsSshFile.class, e);
    }

    return handleException(HdfsSshFile.class, "Invalid file path: %s%s", getAbsolutePath(), filePath.toString());
  }

  @Override
  public void truncate() throws IOException {
  }

  public void notifyModified(SubmissionDirectory submissionDirectory) {
    Submission submission = submissionDirectory.getSubmission();
    context.resetSubmission(submission);
  }

  public void systemFilesNotifyModified() {
    context.resetSubmissions();
  }

}
