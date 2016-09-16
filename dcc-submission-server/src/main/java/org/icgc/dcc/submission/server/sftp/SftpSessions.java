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
package org.icgc.dcc.submission.server.sftp;

import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.common.core.util.Separators.EMPTY_STRING;

import java.util.Optional;

import lombok.NoArgsConstructor;
import lombok.NonNull;

import org.apache.sshd.common.Session;
import org.apache.sshd.common.Session.AttributeKey;
import org.springframework.security.core.Authentication;

@NoArgsConstructor(access = PRIVATE)
public final class SftpSessions {

  public static final FileTransfer NO_FILE_TRANSFER = new FileTransfer(EMPTY_STRING);
  public static final AttributeKey<FileTransfer> FILE_TRANSFER_SESSION_ATTRIBUTE = new AttributeKey<>();

  private static final AttributeKey<Authentication> SESSION_KEY = new AttributeKey<Authentication>();

  public static void setAuthentication(Session session, Authentication authentication) {
    session.setAttribute(SESSION_KEY, authentication);
  }

  public static Authentication getAuthentication(Session session) {
    return session.getAttribute(SESSION_KEY);
  }

  public static void setFileTransfer(@NonNull Session session, @NonNull FileTransfer fileTransfer) {
    session.setAttribute(FILE_TRANSFER_SESSION_ATTRIBUTE, fileTransfer);
  }

  public static void unsetFileTransfer(@NonNull Session session) {
    session.setAttribute(FILE_TRANSFER_SESSION_ATTRIBUTE, NO_FILE_TRANSFER);
  }

  public static Optional<FileTransfer> getFileTransfer(@NonNull Session session) {
    return Optional.ofNullable(session.getAttribute(FILE_TRANSFER_SESSION_ATTRIBUTE));
  }

}
