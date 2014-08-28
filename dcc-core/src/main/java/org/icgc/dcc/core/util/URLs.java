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

import java.io.File;
import java.net.URI;
import java.net.URL;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

import com.google.common.io.Files;
import com.google.common.io.Resources;

/**
 * Utility methods and constants for {@link URL}s.
 */
@NoArgsConstructor(access = PRIVATE)
public final class URLs {

  @SneakyThrows
  public static URL getUrl(@NonNull final String stringRepresentation) {
    return new URL(stringRepresentation);
  }

  public static URL getUrlFromPath(@NonNull final String path) {
    return getUrl(new File(path));
  }

  public static URL getUrl(@NonNull final File file) {
    return getUrl(file.toURI());
  }

  @SneakyThrows
  public static URL getUrl(@NonNull final URI uri) {
    return uri.toURL();
  }

  @SneakyThrows
  public static String toFile(
      @NonNull final URL url,
      @NonNull final String filePath) {
    Resources
        .asByteSource(url)
        .copyTo(
            Files.asByteSink(
                new File(filePath)));

    return filePath;
  }

}
