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
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.toArray;
import static lombok.AccessLevel.PRIVATE;

import java.net.URI;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

/**
 * Util methods for {@link URI}s.
 * <p>
 * TODO: change to decorator? + write test class
 */
@NoArgsConstructor(access = PRIVATE)
public final class URIs {

  private static final String MISSING_INFO = Strings2.EMPTY_STRING;
  private static final int USERNAME_OFFSET = 0;
  private static final int PASSWORD_OFFSET = USERNAME_OFFSET + 1;
  private static final int MISSING_PORT = -1;
  private static final String MISSING_CREDENTIALS = Joiners.CREDENTIALS.join(MISSING_INFO, MISSING_INFO);
  private static final String SCHEME_SEPARATOR = "://";
  private static final String DEFAULT_PROTOCOL = "http"; // TODO: make more generic...
  private static final String DEFAULT_SCHEME = DEFAULT_PROTOCOL + SCHEME_SEPARATOR;

  @SneakyThrows
  public static URI getURI(String value) {
    return new URI(value.contains(SCHEME_SEPARATOR) ?
        value :
        DEFAULT_SCHEME + value);
  }

  // TODO: change to optional
  public static String getHost(URI uri) {
    return firstNonNull(uri.getHost(), MISSING_INFO);
  }

  public static String getPort(URI uri) {
    val port = uri.getPort();
    return String.valueOf(port == MISSING_PORT ? MISSING_INFO : port);
  }

  public static String getUsername(URI uri) {
    return getCredentials(uri.getUserInfo()).getKey();
  }

  public static String getPassword(URI uri) {
    return getCredentials(uri.getUserInfo()).getValue();
  }

  private static Entry<String, String> getCredentials(String userInfo) {
    val credentials = toArray(
        Splitters.CREDENTIALS.split(
            firstNonNull(userInfo, MISSING_CREDENTIALS)),
        String.class);
    checkState(credentials.length == PASSWORD_OFFSET + 1, // TODO: case where only the username is provided possible?
        "Credentials are expected to have 2 components, a username and a password");
    return new SimpleEntry<String, String>(
        credentials[USERNAME_OFFSET],
        credentials[PASSWORD_OFFSET]);
  }

}
