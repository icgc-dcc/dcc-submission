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

import static org.apache.commons.io.IOUtils.skip;
import static org.apache.sshd.common.file.SshFile.Attribute.Group;
import static org.apache.sshd.common.file.SshFile.Attribute.IsDirectory;
import static org.apache.sshd.common.file.SshFile.Attribute.IsRegularFile;
import static org.apache.sshd.common.file.SshFile.Attribute.IsSymbolicLink;
import static org.apache.sshd.common.file.SshFile.Attribute.LastAccessTime;
import static org.apache.sshd.common.file.SshFile.Attribute.LastModifiedTime;
import static org.apache.sshd.common.file.SshFile.Attribute.Owner;
import static org.apache.sshd.common.file.SshFile.Attribute.Permissions;
import static org.apache.sshd.common.file.SshFile.Attribute.Size;
import static org.apache.sshd.common.file.SshFile.Permission.GroupExecute;
import static org.apache.sshd.common.file.SshFile.Permission.GroupRead;
import static org.apache.sshd.common.file.SshFile.Permission.GroupWrite;
import static org.apache.sshd.common.file.SshFile.Permission.OthersExecute;
import static org.apache.sshd.common.file.SshFile.Permission.OthersRead;
import static org.apache.sshd.common.file.SshFile.Permission.OthersWrite;
import static org.apache.sshd.common.file.SshFile.Permission.UserExecute;
import static org.apache.sshd.common.file.SshFile.Permission.UserRead;
import static org.apache.sshd.common.file.SshFile.Permission.UserWrite;
import static org.icgc.dcc.submission.fs.SubmissionFileSystem.VALIDATION_DIRNAME;
import static org.icgc.dcc.submission.server.sftp.fs.HdfsFileUtils.handleException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.sshd.common.Session;
import org.apache.sshd.common.file.SshFile;
import org.icgc.dcc.submission.server.sftp.SftpContext;
import org.springframework.security.core.Authentication;

@Slf4j
@AllArgsConstructor
public abstract class HdfsSshFile implements SshFile {

  /**
   * Based on Jerry's experimentation with {@code ReadonlyExportedDataSshFile} this seems like a good nominal value
   * compared to Hadoop's default of 4096.
   * 
   * @see {@link FileSystem#open(Path)}
   */
  private static final int HDFS_READ_BUFFER_SIZE_BYTES = 32768;

  private static final String FILE_SYSTEM_GROUP = "icgc";
  private static final String FILE_SYSTEM_OWNER = "dcc";

  protected static final String SEPARATOR = "/";

  @NonNull
  protected final SftpContext context;
  @NonNull
  protected Path path;
  @NonNull
  protected final FileSystem fileSystem;
  @NonNull
  protected final Authentication authentication;
  @NonNull
  protected final Session session;

  @Override
  public Map<Attribute, Object> getAttributes(boolean followLinks) throws IOException {
    val map = new HashMap<Attribute, Object>();
    map.put(Size, getSize());
    map.put(IsDirectory, isDirectory());
    map.put(IsRegularFile, isFile());
    map.put(IsSymbolicLink, false);
    map.put(LastModifiedTime, getLastModified());
    map.put(LastAccessTime, getLastModified());
    map.put(Group, FILE_SYSTEM_GROUP);
    map.put(Owner, FILE_SYSTEM_OWNER);

    val p = EnumSet.noneOf(Permission.class);
    if (isReadable()) {
      p.add(UserRead);
      p.add(GroupRead);
      p.add(OthersRead);
    }
    if (isWritable()) {
      p.add(UserWrite);
      p.add(GroupWrite);
      p.add(OthersWrite);
    }
    if (isExecutable()) {
      p.add(UserExecute);
      p.add(GroupExecute);
      p.add(OthersExecute);
    }

    map.put(Permissions, p);

    return map;
  }

  @Override
  public void setAttributes(Map<Attribute, Object> attributes) throws IOException {
    // Do not inherit the attributes sent from the SFTP client.
    // See {@link NativeSshFileNio#setAttributes} for an example implementation.
  }

  @Override
  public Object getAttribute(Attribute attribute, boolean followLinks) throws IOException {
    return getAttributes(followLinks).get(attribute);
  }

  @Override
  public void setAttribute(Attribute attribute, Object value) throws IOException {
    val map = new HashMap<Attribute, Object>();
    map.put(attribute, value);

    setAttributes(map);
  }

  @Override
  public String readSymbolicLink() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean doesExist() {
    try {
      if (isValidationFile(path)) {
        // Validation files should not be visible
        return false;
      }

      return fileSystem.exists(path);
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

  @Override
  public boolean isReadable() {
    try {
      FsAction u = fileSystem.getFileStatus(path).getPermission().getUserAction();

      return (u == FsAction.ALL || u == FsAction.READ_WRITE || u == FsAction.READ || u == FsAction.READ_EXECUTE);
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

  @Override
  public boolean isWritable() {
    try {
      FsAction u = fileSystem.getFileStatus(path).getPermission().getUserAction();

      return (u == FsAction.ALL || u == FsAction.READ_WRITE || u == FsAction.WRITE || u == FsAction.WRITE_EXECUTE);
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
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
      return fileSystem.getFileStatus(path).getModificationTime();
    } catch (Exception e) {
      return handleException(Long.class, e);
    }
  }

  @Override
  public boolean setLastModified(long time) {
    try {
      fileSystem.setTimes(path, time, -1);

      return true;
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

  @Override
  public long getSize() {
    try {
      return fileSystem.getFileStatus(path).getLen();
    } catch (Exception e) {
      return handleException(Long.class, e);
    }
  }

  @Override
  public String getOwner() {
    try {
      return fileSystem.getFileStatus(path).getOwner();
    } catch (Exception e) {
      return handleException(String.class, e);
    }
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
    try {
      if (!this.isWritable()) {
        throw new IOException("SFTP is in readonly mode");
      }

      log.info("Submission file opened: '{}'", path);
      val delegate = fileSystem.create(path);

      return new SessionOutputStream(delegate, session, path);
    } catch (Exception e) {
      return handleException(OutputStream.class, e);
    }
  }

  /**
   * Opens the underlying {@code path} for SFTP download streaming.
   * <p>
   * Added download to support "Incremental Submission"
   * 
   * @see https://issues.apache.org/jira/browse/HDFS-246
   * @see https://jira.oicr.on.ca/browse/DCC-412
   */
  @Override
  public InputStream createInputStream(long offset) throws IOException {
    val inputStream = fileSystem.open(path, HDFS_READ_BUFFER_SIZE_BYTES);
    try {
      inputStream.seek(offset);
    } catch (IOException e) {
      // Seek fails when the offset requested passes the file length,
      // this line guarantee we are positioned at the end of the file
      skip(inputStream, offset);
    }

    return inputStream;
  }

  @Override
  public void createSymbolicLink(SshFile destination) throws IOException {
    // No-op
  }

  @Override
  public void handleClose() throws IOException {
    // No-op
  }

  public abstract HdfsSshFile getChild(Path filePath);

  protected boolean isValidationFile(Path path) {
    if (path == null) {
      return false;
    }

    val uri = path.toString();
    return uri.contains(VALIDATION_DIRNAME);
  }

}
