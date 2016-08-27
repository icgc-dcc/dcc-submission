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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.common.hadoop.fs.HadoopUtils.lsAll;
import static org.icgc.dcc.submission.server.sftp.fs.HdfsFileUtils.SshFileList;
import static org.icgc.dcc.submission.server.sftp.fs.HdfsFileUtils.handleException;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.sshd.common.file.SshFile;
import org.icgc.dcc.submission.server.sftp.SftpContext;

import lombok.NonNull;
import lombok.SneakyThrows;

public abstract class BaseDirectoryHdfsSshFile extends HdfsSshFile {

  @NonNull
  private final RootHdfsSshFile root;
  @NonNull
  protected final String directoryName;

  protected BaseDirectoryHdfsSshFile(SftpContext context, RootHdfsSshFile root, String directoryName) {
    super(context, new Path(root.path, directoryName.isEmpty() ? "/" : directoryName), root.fileSystem,
        root.authentication);
    this.root = checkNotNull(root);
    this.directoryName = checkNotNull(directoryName);
  }

  @Override
  public String getAbsolutePath() {
    return SEPARATOR + directoryName;
  }

  @Override
  public String getName() {
    return path.getName();
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
  public RootHdfsSshFile getParentFile() {
    return root;
  }

  @Override
  public boolean create() throws IOException {
    try {
      if (isWritable()) {
        fileSystem.create(path);

        return true;
      }
      return false;
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

  @Override
  public void truncate() throws IOException {
    create();
  }

  @Override
  public List<SshFile> listSshFiles() {
    try {
      List<Path> paths = lsAll(fileSystem, path);
      List<SshFile> sshFiles = newArrayList();

      for (Path path : paths) {
        FileHdfsSshFile sshFile = new FileHdfsSshFile(context, this, path.getName());
        if (sshFile.doesExist()) {
          sshFiles.add(sshFile);
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
        return new FileHdfsSshFile(context, this, filePath.getName());
      }
    } catch (Exception e) {
      return handleException(HdfsSshFile.class, e);
    }

    return handleException(HdfsSshFile.class, "Invalid file path: %s%s", getAbsolutePath(), filePath.toString());
  }

  @Override
  @SneakyThrows
  public boolean mkdir() {
    return create();
  }

  @Override
  public boolean move(SshFile destination) {
    try {
      return fileSystem.rename(path, new Path(destination.getAbsolutePath()));
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

}
