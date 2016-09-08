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

import static org.icgc.dcc.submission.server.sftp.SftpServerService.FILE_TRANSFER_SESSION_ATTRIBUTE;
import static org.icgc.dcc.submission.server.sftp.SftpServerService.NO_FILE_TRANSFER;

import java.io.IOException;
import java.io.OutputStream;

import lombok.NonNull;

import org.apache.hadoop.fs.Path;
import org.apache.sshd.common.Session;
import org.icgc.dcc.submission.server.sftp.FileTransfer;

public final class SessionOutputStream extends OutputStream {

  private final OutputStream delegate;
  private final Session session;

  public SessionOutputStream(@NonNull OutputStream delegate, @NonNull Session session, @NonNull Path path) {
    this.delegate = delegate;
    this.session = session;
    session.setAttribute(FILE_TRANSFER_SESSION_ATTRIBUTE, new FileTransfer(path.toString()));
  }

  @Override
  public void write(int b) throws IOException {
    delegate.write(b);
  }

  @Override
  public void write(byte[] b) throws IOException {
    delegate.write(b);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    delegate.write(b, off, len);
  }

  @Override
  public void flush() throws IOException {
    delegate.flush();
  }

  @Override
  public void close() throws IOException {
    delegate.close();
    session.setAttribute(FILE_TRANSFER_SESSION_ATTRIBUTE, NO_FILE_TRANSFER);
  }

}