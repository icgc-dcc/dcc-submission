/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.core.util;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Optional.fromNullable;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.toArray;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.util.Joiners.COLON;
import static org.icgc.dcc.core.util.Joiners.PATH;
import static org.icgc.dcc.core.util.Optionals.ABSENT_STRING;

import java.net.URI;
import java.net.URL;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

import com.google.common.base.Optional;

/**
 * Utility methods and constants for {@link URI}s.
 * <p>
 * TODO: change to decorator? + write test class
 */
@NoArgsConstructor(access = PRIVATE)
public final class URIs {

  public static final String LOCAL_ROOT = Protocol.FILE.getId() + Separators.PATH;
  public static final String HDFS_ROOT = Protocol.HDFS.getId() + Separators.PATH;

  private static final int USERNAME_OFFSET = 0;
  private static final int PASSWORD_OFFSET = USERNAME_OFFSET + 1;
  private static final String SCHEME_SEPARATOR = "://";
  private static final String DEFAULT_PROTOCOL = "http"; // TODO: make more generic...
  private static final String DEFAULT_SCHEME = DEFAULT_PROTOCOL + SCHEME_SEPARATOR;

  @SneakyThrows
  public static URI getUri(
      @NonNull final Protocol protocol,
      @NonNull final String host,
      final int port,
      Optional<String> optionalPath) {
    return new URI(getUriString(protocol, host, port, optionalPath));
  }

  public static String getUriString(
      @NonNull final Protocol protocol,
      @NonNull final String host,
      final int port,
      Optional<String> optionalPath) {
    val base = protocol.getId() +
        COLON.join(
            host,
            port);

    return optionalPath.isPresent() ?
        PATH.join(base, optionalPath.get()) :
        base;
  }

  @SneakyThrows
  public static URI getURI(@NonNull final String value) {
    return new URI(value.contains(SCHEME_SEPARATOR) ?
        value :
        DEFAULT_SCHEME + value);
  }

  public static Optional<String> getHost(@NonNull final URI uri) {
    return fromNullable(uri.getHost());
  }

  public static Optional<Integer> getPort(@NonNull final URI uri) {
    return fromNullable(uri.getPort());
  }

  public static Optional<String> getUsername(@NonNull final URI uri) {
    return uri.getUserInfo() != null ?
        Optional.of(getCredentials(uri.getUserInfo()).getKey()) :
        ABSENT_STRING;
  }

  public static Optional<String> getPassword(@NonNull final URI uri) {
    return uri.getUserInfo() != null ?
        Optional.of(getCredentials(uri.getUserInfo()).getValue()) :
        ABSENT_STRING;
  }

  private static Entry<String, String> getCredentials(@NonNull final String userInfo) {
    val credentials = toArray(
        Splitters.CREDENTIALS.split(
            firstNonNull(userInfo, Separators.CREDENTIALS)),
        String.class);
    checkState(credentials.length == PASSWORD_OFFSET + 1, // TODO: case where only the username is provided possible?
        "Credentials are expected to have 2 components, a username and a password");
    return new SimpleEntry<String, String>(
        credentials[USERNAME_OFFSET],
        credentials[PASSWORD_OFFSET]);
  }

  @SneakyThrows
  public static URI fromUrl(@NonNull final URL url) {
    return url.toURI();
  }

}
