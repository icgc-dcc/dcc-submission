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
package org.icgc.dcc.core;

import static com.google.common.io.Resources.getResource;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.core.util.Extensions.JSON;
import static org.icgc.dcc.core.util.Joiners.EXTENSION;
import static org.icgc.dcc.core.util.Joiners.PATH;

import java.net.URL;

import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = PRIVATE)
public final class DccResources {

  public static final String DICTIONARY_JSON_FILE_NAME = EXTENSION.join("Dictionary", JSON);
  public static final String CODELISTS_JSON_FILE_NAME = EXTENSION.join("CodeList", JSON);

  private static final String DCC_RESOURCES_DIR = "org/icgc/dcc/resources";

  public static URL getDictionaryDccResource() {
    return getResourceUrl(DICTIONARY_JSON_FILE_NAME);
  }

  public static URL getCodeListsDccResource() {
    return getResourceUrl(CODELISTS_JSON_FILE_NAME);
  }

  private static URL getResourceUrl(@NonNull final String fileName) {
    return getResource(getResourcePath(fileName));
  }

  public static String getDictionaryResourcePath() {
    return getResourcePath(DICTIONARY_JSON_FILE_NAME);
  }

  public static String getCodeListsResourcePath() {
    return getResourcePath(CODELISTS_JSON_FILE_NAME);
  }

  private static String getResourcePath(@NonNull final String fileName) {
    return PATH.join(DCC_RESOURCES_DIR, fileName);
  }

}
