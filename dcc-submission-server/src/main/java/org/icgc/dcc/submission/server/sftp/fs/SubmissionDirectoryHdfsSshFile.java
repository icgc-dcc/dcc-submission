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

import static org.icgc.dcc.submission.server.sftp.fs.HdfsFileUtils.handleException;

import java.io.FileNotFoundException;

import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.ReleaseException;
import org.icgc.dcc.submission.server.sftp.SftpContext;

import lombok.NonNull;

public class SubmissionDirectoryHdfsSshFile extends BaseDirectoryHdfsSshFile {

  @NonNull
  private final SubmissionDirectory directory;

  public SubmissionDirectoryHdfsSshFile(SftpContext context, RootHdfsSshFile root, String directoryName) {
    super(context, root, directoryName);
    this.directory = getSubmissionDirectory(directoryName);
  }

  @Override
  public String getAbsolutePath() {
    return SEPARATOR + directoryName;
  }

  @Override
  public boolean isWritable() {
    // See doesExist for explanation of the null check
    try {
      if (directory == null || directory.isReadOnly()) {
        return false;
      }

      return super.isWritable();
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

  @Override
  public boolean doesExist() {
    // If directory is null it means that the directory doesn't exist or the user does not have permission to access it
    // We are using this in lieu of throwing an exception, since Mina's interface erroneously disallows checked
    // exceptions
    try {
      return directory == null ? false : super.doesExist();
    } catch (Exception e) {
      return handleException(Boolean.class, e);
    }
  }

  private SubmissionDirectory getSubmissionDirectory(String directoryName) {
    try {
      try {
        return context.getSubmissionDirectory(directoryName, authentication);
      } catch (ReleaseException e) {
        throw new FileNotFoundException(directoryName);
      }
    } catch (Exception e) {
      return handleException(SubmissionDirectory.class, e);
    }
  }

}
