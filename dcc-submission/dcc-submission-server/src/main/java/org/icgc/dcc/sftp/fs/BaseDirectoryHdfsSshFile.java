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
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.filesystem.hdfs.HadoopUtils.lsAll;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.sshd.server.SshFile;

public abstract class BaseDirectoryHdfsSshFile extends HdfsSshFile {

  private final RootHdfsSshFile root;

  protected final String directoryName;

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
    try {
      if (isWritable()) {
        this.fs.create(path);
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

  @SuppressWarnings("unchecked")
  @Override
  public List<SshFile> listSshFiles() {
    try {
      List<Path> pathList = lsAll(fs, path);
      List<SshFile> sshFileList = newArrayList();

      for (Path path : pathList) {
        sshFileList.add(new FileHdfsSshFile(this, path.getName()));
      }

      return sshFileList;
    } catch (Exception e) {
      return handleException(List.class, e);
    }
  }

  @Override
  public HdfsSshFile getChild(Path filePath) {
    try {
      switch (filePath.depth()) {
      case 0:
        return this;
      case 1:
        return new FileHdfsSshFile(this, filePath.getName());
      }
    } catch (Exception e) {
      return handleException(HdfsSshFile.class, e);
    }

    return handleException(HdfsSshFile.class, "Invalid file path: " + this.getAbsolutePath() + filePath.toString());
  }

  @Override
  public boolean mkdir() {
    try {
      return create();
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

  @Override
  public boolean move(SshFile destination) {
    try {
      return this.fs.rename(path, new Path(destination.getAbsolutePath()));
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

  public abstract void notifyModified();

}
