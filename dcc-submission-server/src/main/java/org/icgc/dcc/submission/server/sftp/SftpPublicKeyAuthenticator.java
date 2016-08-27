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

import static com.google.common.io.BaseEncoding.base64;
import static org.icgc.dcc.submission.server.sftp.SftpSessions.setAuthentication;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import org.apache.sshd.common.Session;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Adds public key authentication support for a single {@link #knownKey}. This is mainly for use with deployments that
 * require a password-less login.
 * 
 * @see http://caffeineiscodefuel.blogspot.ca/2013/04/apache-mina-sshd-publickeyauthenticator.html
 */
@Slf4j
@RequiredArgsConstructor
public class SftpPublicKeyAuthenticator implements PublickeyAuthenticator {

  @NonNull
  private final String knownKey;

  /**
   * Returns true if the key matches our known key, this allows authentication to proceed.
   */
  @Override
  public boolean authenticate(String username, PublicKey key, ServerSession session) {
    if (key instanceof RSAPublicKey) {
      val rsaKey = (RSAPublicKey) key;

      if (isMatch(rsaKey)) {
        log.info("Successfully authenicated user '{}' using public key", username);
        login(username, session);

        return true;
      }

      log.warn("Invalid authentication for user '{}' using public key", username);
      return false;
    }

    // Doesn't handle other key types currently.
    return false;
  }

  @SneakyThrows
  private void login(String username, Session session) {
    try {
      val authentication = new UsernamePasswordAuthenticationToken(username, "", ImmutableList.of(
          new SimpleGrantedAuthority("ROLE_ADMIN")));

      setAuthentication(session, authentication);
    } catch (Throwable t) {
      log.error("Exception logging in user '{}': {}", username, t.getMessage());
      throw t;
    }
  }

  private boolean isMatch(RSAPublicKey rsaKey) {
    String actual = new String(encode(rsaKey));
    String expected = new String(decode(knownKey.trim()));
    val match = actual.equals(expected);

    return match;
  }

  /**
   * Converts a Java RSA PK to SSH2 Format.s
   */
  private static byte[] encode(RSAPublicKey key) {
    try {
      val buffer = new ByteArrayOutputStream();
      val name = "ssh-rsa".getBytes(Charsets.US_ASCII.name());

      write(name, buffer);
      write(key.getPublicExponent().toByteArray(), buffer);
      write(key.getModulus().toByteArray(), buffer);

      return buffer.toByteArray();
    } catch (Exception e) {
      log.error("Error encoding public key:", e);
    }

    return null;
  }

  /**
   * Decodes the known key.
   */
  private static byte[] decode(String knownKey) {
    return base64().decode(knownKey);
  }

  private static void write(byte[] text, OutputStream outputStream) throws IOException {
    for (int shift = 24; shift >= 0; shift -= 8) {
      outputStream.write((text.length >>> shift) & 0xFF);
    }

    outputStream.write(text);
  }

}
