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
package org.icgc.dcc.submission.loader.meta;

import java.util.Map;
import java.util.Map.Entry;

import org.icgc.dcc.common.core.json.Jackson;
import org.icgc.dcc.common.core.meta.Resolver.CodeListsResolver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import lombok.NonNull;
import lombok.val;

public class CodeListsService {

  private final Map<String, Map<String, String>> codeLists;

  public CodeListsService(@NonNull CodeListsResolver codeListsResolver) {
    this.codeLists = resolveCodeLists(codeListsResolver.get());
  }

  public Map<String, String> getCodeLists(@NonNull String codeListName) {
    return codeLists.get(codeListName);
  }

  private Map<String, Map<String, String>> resolveCodeLists(ArrayNode array) {
    val codeLists = ImmutableMap.<String, Map<String, String>> builder();
    for (val element : array) {
      codeLists.put(createCodeListEntry(element));
    }

    return codeLists.build();
  }

  private static Entry<String, Map<String, String>> createCodeListEntry(JsonNode element) {
    val codeListName = element.get("name").textValue();
    val terms = createTerms(Jackson.asArrayNode(element.get("terms")));

    return Maps.immutableEntry(codeListName, terms);
  }

  private static Map<String, String> createTerms(ArrayNode array) {
    val terms = ImmutableMap.<String, String> builder();
    for (val element : array) {
      val code = element.get("code").textValue();
      val value = element.get("value").textValue();
      terms.put(code, value);
    }

    return terms.build();
  }

}
