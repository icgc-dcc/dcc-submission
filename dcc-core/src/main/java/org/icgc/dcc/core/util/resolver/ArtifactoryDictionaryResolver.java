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
package org.icgc.dcc.core.util.resolver;

import static org.icgc.dcc.core.util.FormatUtils._;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.core.util.Optionals;
import org.icgc.dcc.core.util.resolver.Resolver.DictionaryResolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Optional;

public class ArtifactoryDictionaryResolver implements DictionaryResolver {

  private static String getDefaultVersion() {
    return "0.8a";
  }

  @Override
  public ObjectNode get() {
    return getDictionary(Optional.of(getDefaultVersion()));
  }

  @Override
  public ObjectNode apply(Optional<String> version) {
    return getDictionary(version);
  }

  @SneakyThrows
  private static ObjectNode getDictionary(Optional<String> version) {
    // Resolve
    @Cleanup
    val zip = new ZipInputStream(getDictionaryUrl(version).openStream());
    ZipEntry entry;

    val entryName = "org/icgc/dcc/resources/Dictionary.json";
    do {
      entry = zip.getNextEntry();
    } while (!entryName.equals(entry.getName()));

    return new ObjectMapper().readValue(zip, ObjectNode.class);
  }

  protected static URL getDictionaryUrl(Optional<String> optionalVersion) throws MalformedURLException {
    val basePath = "http://seqwaremaven.oicr.on.ca/artifactory";
    val template = "%s/simple/dcc-dependencies/org/icgc/dcc/dcc-resources/%s/dcc-resources-%s.jar";
    val version = Optionals.defaultValue(optionalVersion, getDefaultVersion());
    URL url = new URL(_(template, basePath, version, version));

    return url;
  }

}
