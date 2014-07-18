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

import static com.google.common.base.Preconditions.checkArgument;
import static org.icgc.dcc.core.util.Jackson.DEFAULT;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import org.icgc.dcc.core.util.resolver.Resolver.SubmissionSystemResolber.SubmissionSystemCodeListsResolver;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Optional;

@AllArgsConstructor
@NoArgsConstructor
public class RestfulCodeListsResolver implements SubmissionSystemCodeListsResolver {

  private String url = DEFAULT_CODELISTS_URL;

  @Override
  public ArrayNode get() {
    return getCodeList();
  }

  @SneakyThrows
  private ArrayNode getCodeList() {
    return DEFAULT.readValue(
        Resolvers.getContent(
            getSubmissionSystemUrl(
            Optional.<String> absent())),
        ArrayNode.class);
  }

  @Override
  public String getSubmissionSystemUrl(Optional<String> qualifier) {
    checkArgument(!qualifier.isPresent(),
        "Code lists can not be qualified, '%s' provided", qualifier);
    return url + PATH;
  }

}
