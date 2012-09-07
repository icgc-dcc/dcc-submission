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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.sshd.server.SshFile;
import org.icgc.dcc.filesystem.DccFileSystemException;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;

/**
 * 
 */
public abstract class BaseDirectoryHdfsSshFile extends HdfsSshFile {

  private final RootHdfsSshFile root;

  private final String directoryName;

  protected BaseDirectoryHdfsSshFile(RootHdfsSshFile root, String directoryName) {
    super(new Path(root.path, directoryName.isEmpty() ? "/" : directoryName), root.fs);
    checkNotNull(root);
    checkNotNull(directoryName);
    this.root = root;
    this.directoryName = directoryName;
  }

  @Override
  public String getAbsolutePath() {
    return SEPARATOR + directoryName;
  }

  @Override
  public String getName() {
    return this.path.getName();
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
    if(isWritable()) {
      this.fs.create(path);
      return true;
    }
    return false;
  }

  @Override
  public void truncate() throws IOException {
    create();
  }

  @Override
  public List<SshFile> listSshFiles() {
    List<Path> pathList = HadoopUtils.lsAll(fs, path.toString());
    List<SshFile> sshFileList = new ArrayList<SshFile>();
    for(Path path : pathList) {
      sshFileList.add(new FileHdfsSshFile(this, path.getName()));
    }
    return sshFileList;
  }

  @Override
  public HdfsSshFile getChild(Path filePath) {
    switch(filePath.depth()) {
    case 0:
      return this;
    case 1:
      return new FileHdfsSshFile(this, filePath.getName());
    }
    throw new DccFileSystemException("Invalid file path: " + this.getAbsolutePath() + filePath.toString());
  }

  @Override
  public boolean mkdir() {
    try {
      return create();
    } catch(IOException e) {
      log.error("File system error", e);
    }
    return false;
  }

  @Override
  public boolean move(SshFile destination) {
    try {
      return this.fs.rename(path, new Path(destination.getAbsolutePath()));
    } catch(IOException e) {
      log.error("File system error", e);
    }
    return false;
  }

  public abstract void notifyModified();
}
