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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.sshd.server.SshFile;
import org.icgc.dcc.filesystem.DccFileSystemException;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.filesystem.SubmissionDirectory;
import org.icgc.dcc.filesystem.hdfs.HadoopUtils;
import org.mortbay.log.Log;

/**
 * 
 */
class RootHdfsSshFile extends HdfsSshFile {

  private final ReleaseFileSystem rfs;

  public RootHdfsSshFile(ReleaseFileSystem rfs) {
    super(rfs);
    this.rfs = rfs;
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
    List<Path> pathList = HadoopUtils.lsAll(fs, path.toString());
    List<SshFile> sshFileList = new ArrayList<SshFile>();
    for(Path path : pathList) {
      try {
        sshFileList.add(new DirectoryHdfsSshFile(this, path.getName()));
      } catch(DccFileSystemException e) {
        Log.info("Directory skipped due to insufficient permissions: " + path.getName());
      }
    }
    return sshFileList;
  }

  public SubmissionDirectory getSubmissionDirectory(String directoryName) {
    return this.rfs.getSubmissionDirectory(directoryName);
  }

  @Override
  public HdfsSshFile getChild(Path filePath) {
    switch(filePath.depth()) {
    case 0:
      return this;
    case 1:
      return new DirectoryHdfsSshFile(this, filePath.getName());
    case 2:
      DirectoryHdfsSshFile parentDir = new DirectoryHdfsSshFile(this, filePath.getParent().getName());
      return new FileHdfsSshFile(parentDir, filePath.getName());
    }
    throw new DccFileSystemException("Invalid file path: " + this.getAbsolutePath() + filePath.toString());
  }

  @Override
  public void truncate() throws IOException {

  }
}
