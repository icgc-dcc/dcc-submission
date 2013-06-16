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

import static com.google.common.base.Throwables.propagateIfInstanceOf;
import static org.icgc.dcc.filesystem.DccFileSystem.VALIDATION_DIRNAME;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.SneakyThrows;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.sshd.server.SshFile;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HdfsSshFile implements SshFile {

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
      if (isValidationFile(path)) {
        // Validation files should not be visible
        return false;
      }

      return fs.exists(path);
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

  @Override
  public boolean isReadable() {
    try {
      FsAction u = fs.getFileStatus(path).getPermission().getUserAction();

      return (u == FsAction.ALL || u == FsAction.READ_WRITE || u == FsAction.READ || u == FsAction.READ_EXECUTE);
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

  @Override
  public boolean isWritable() {
    try {
      FsAction u = fs.getFileStatus(path).getPermission().getUserAction();

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
      return fs.getFileStatus(path).getModificationTime();
    } catch (Exception e) {
      return handleException(Long.class, e);
    }
  }

  @Override
  public boolean setLastModified(long time) {
    try {
      fs.setTimes(path, time, -1);

      return true;
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

  @Override
  public long getSize() {
    try {
      return fs.getFileStatus(path).getLen();
    } catch (Exception e) {
      return handleException(Long.class, e);
    }
  }

  @Override
  public String getOwner() {
    try {
      return fs.getFileStatus(path).getOwner();
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
      if (this.isWritable() == false) {
        throw new IOException("SFTP is in readonly mode");
      }

      return fs.create(path);
    } catch (Exception e) {
      return handleException(OutputStream.class, e);
    }
  }

  @Override
  public InputStream createInputStream(long offset) throws IOException {
    // because of DCC-412, the download size bug, we will temporarily disable download
    // return fs.open(path);
    // Ideally we would throw an Unsupported Exception, but mina will kick user out
    // so we have to use a low level IOException to keep user connected
    throw new IOException("Download from SFTP is disabled");
  }

  @Override
  public void handleClose() throws IOException {
  }

  public abstract HdfsSshFile getChild(Path filePath);

  protected boolean isValidationFile(Path path) {
    if (path == null) {
      return false;
    }

    String uri = path.toString();
    return uri.contains(VALIDATION_DIRNAME);
  }

  /**
   * Apache MINA exception handling method designed to evade Java's checked exception mechanism to propagate
   * {@code IOException}s to avoid terminating MINA SFTP sessions.
   * 
   * @param e - the exception to propagate
   */
  protected void handleException(Exception e) {
    handleException(Void.class, e);
  }

  /**
   * Apache MINA exception handling method designed to evade Java's checked exception mechanism to propagate
   * {@code IOException}s to avoid terminating MINA SFTP sessions.
   * 
   * @param type - the return type
   * @param message - the exception method
   * @return nothing
   */
  protected <T> T handleException(Class<T> type, String message) {
    return handleException(type, new IOException(message));
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
