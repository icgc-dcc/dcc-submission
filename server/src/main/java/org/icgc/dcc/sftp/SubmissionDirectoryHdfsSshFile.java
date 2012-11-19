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

import org.icgc.dcc.filesystem.SubmissionDirectory;

class SubmissionDirectoryHdfsSshFile extends BaseDirectoryHdfsSshFile {

  private final SubmissionDirectory directory;

  public SubmissionDirectoryHdfsSshFile(RootHdfsSshFile root, String directoryName) {
    super(root, directoryName);
    this.directory = root.getSubmissionDirectory(directoryName);
  }

  @Override
  public String getAbsolutePath() {
    return SEPARATOR + directoryName;
  }

  @Override
  public boolean isWritable() {
    // See doesExist for explanation of the null check
    if(directory == null || directory.isReadOnly()) {
      return false;
    }
    // check if the current project is validating
    if(this.directory.isWritable() == false) {
      return false;
    }
    return super.isWritable();
  }

  @Override
  public boolean doesExist() {
    // If directory is null it means that the directory doesn't exist or the user does not have permission to access it
    // We are using this in lieu of throwing an exception, since Mina's interface erroneously disallows checked
    // exceptions
    return directory == null ? false : super.doesExist();
  }

  @Override
  public void notifyModified() {
    this.getParentFile().notifyModified(this.directory);
  }
}
