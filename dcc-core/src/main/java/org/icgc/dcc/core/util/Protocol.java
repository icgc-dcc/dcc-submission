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

import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.util.Strings2.removeTarget;

import java.net.URL;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import org.icgc.dcc.core.model.Identifiable;

@RequiredArgsConstructor(access = PRIVATE)
public enum Protocol implements Identifiable {

  FILE(Scheme.FILE),
  HTTP(Scheme.HTTP),
  HTTPS(Scheme.HTTPS),
  HDFS(Scheme.HDFS),
  MONGO(Scheme.MONGO),
  ES(Scheme.ES),
  S3(Scheme.S3);

  private static final String PROTOCOL_SUFFIX = "://"; // TODO: existing constant?

  private final Scheme scheme;

  @Override
  public String getId() {
    return scheme.getId() + PROTOCOL_SUFFIX;
  }

  public boolean isFile() {
    return this == FILE;
  }

  public boolean isHdfs() {
    return this == HDFS;
  }

  public static Protocol from(@NonNull final String protocol) {
    return valueOf(removeTarget(protocol, PROTOCOL_SUFFIX).toUpperCase());
  }

  @SneakyThrows
  public static Protocol fromURL(@NonNull final String url) {
    return fromURL(new URL(url));
  }

  public static Protocol fromURL(@NonNull final URL url) {
    return from(url.getProtocol());
  }

}
