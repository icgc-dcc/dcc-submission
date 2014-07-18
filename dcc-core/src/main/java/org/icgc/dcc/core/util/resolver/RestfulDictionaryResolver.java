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

import static org.icgc.dcc.core.util.Jackson.DEFAULT;
import static org.icgc.dcc.core.util.Joiners.PATH;
import static org.icgc.dcc.core.util.resolver.Resolver.Resolvers.getContent;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import org.icgc.dcc.core.util.resolver.Resolver.SubmissionSystemResolber.SubmissionSystemDictionaryResolver;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Optional;

@AllArgsConstructor
@NoArgsConstructor
public class RestfulDictionaryResolver implements SubmissionSystemDictionaryResolver {

  private String url = DEFAULT_DICTIONARY_URL;

  @Override
  public ObjectNode get() {
    return get(Optional.<String> absent());
  }

  @Override
  public ObjectNode get(Optional<String> version) {
    return getDictionary(version);
  }

  @SneakyThrows
  private ObjectNode getDictionary(Optional<String> version) {
    return DEFAULT.readValue(
        getContent(
        getSubmissionSystemUrl(version)),
        ObjectNode.class);
  }

  @Override
  public String getSubmissionSystemUrl(Optional<String> version) {
    return url + (version.isPresent() ?
        PATH.join(PATH_SPECIFIC, version.get()) :
        PATH_CURRENT);
  }

}
