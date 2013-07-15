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

import static org.icgc.dcc.sftp.fs.HdfsFileUtils.handleException;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.sshd.server.SshFile;

public class FileHdfsSshFile extends HdfsSshFile {

  private final BaseDirectoryHdfsSshFile directory;

  public FileHdfsSshFile(BaseDirectoryHdfsSshFile directory, String fileName) {
    super(new Path(directory.path, fileName), directory.fs);
    this.directory = directory;
  }

  @Override
  public String getAbsolutePath() {
    return directory.getAbsolutePath() + SEPARATOR + path.getName();
  }

  @Override
  public String getName() {
    return path.getName();
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public boolean isFile() {
    return true;
  }

  @Override
  public boolean isReadable() {
    // Submission files are write-only
    return false;
  }

  @Override
  public boolean isWritable() {
    try {
      if (directory.isWritable() == false) {
        return false;
      }
      if (doesExist()) {
        return super.isWritable();
      }

      return true;
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

  @Override
  public boolean isRemovable() {
    try {
      return isWritable();
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

  @Override
  public SshFile getParentFile() {
    return directory;
  }

  @Override
  public boolean mkdir() {
    return false;
  }

  @Override
  public boolean create() throws IOException {
    try {
      if (isWritable()) {
        directory.notifyModified();
        fs.createNewFile(path);

        return true;
      }
      return false;
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

  @Override
  public boolean delete() {
    try {
      if (isRemovable()) {
        directory.notifyModified();
        boolean success = fs.delete(path, false);

        if (success == false) {
          throw new IOException("Unable to delete file " + path.toUri());
        }

        return success;
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
  public boolean move(SshFile destination) {
    try {
      if (isWritable() && destination.isWritable()) {
        Path destinationPath =
            new Path(directory.getParentFile().path, destination.getAbsolutePath().substring(1));

        directory.notifyModified();
        boolean success = fs.rename(path, destinationPath);

        if (success == false) {
          throw new IOException("Unable to move file " + path.toUri() + " to " + destinationPath.toUri());
        }

        path = destinationPath;

        return success;
      }

      return false;
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

  @Override
  public List<SshFile> listSshFiles() {
    return null;
  }

  @Override
  public HdfsSshFile getChild(Path filePath) {
    return handleException(HdfsSshFile.class, "Invalid file path: %s%s", getAbsolutePath(), filePath.toString());
  }

}
