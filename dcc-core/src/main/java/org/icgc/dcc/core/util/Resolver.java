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

import static com.google.common.net.HttpHeaders.ACCEPT;

import java.io.InputStream;
import java.net.URL;

import lombok.SneakyThrows;
import lombok.val;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Optional;

public interface Resolver {

  int DEFAULT_PORT = 5380;
  String PATH_BASE = "/ws";

  String getFullUrl(Optional<String> qualifier);

  /**
   * Abstraction that resolves the content of the most current dictionary.
   */
  public interface DictionaryResolver extends Resolver {

    String PATH_SPECIFIC = PATH_BASE + "/dictionaries";
    String PATH_CURRENT = PATH_BASE + "/nextRelease/dictionary";
    String DEFAULT_DICTIONARY_URL = "http://***REMOVED***:" + DEFAULT_PORT + PATH_CURRENT;

    /**
     * Resolves the current version of the dictionary.
     * 
     * @return the current dictionary
     */
    ObjectNode getDictionary();

    /**
     * Resolves version {@code version} of the dictionary.
     * 
     * @return the requested dictionary
     */
    ObjectNode getDictionary(Optional<String> version);

  }

  /**
   * Abstraction that resolves the content of the code lists.
   */
  public interface CodeListsResolver extends Resolver {

    String PATH = PATH_BASE + "/codeLists";
    String DEFAULT_CODELISTS_URL = "http://***REMOVED***:" + DEFAULT_PORT + PATH;

    /**
     * Resolves the codelists.
     * 
     * @return the code lists
     */
    ArrayNode getCodeLists();

  }

  static class Resolvers {

    @SneakyThrows
    static InputStream getContent(String url) {
      val connection = new URL(url).openConnection();
      connection.setRequestProperty(ACCEPT, "application/json");

      return (InputStream) connection.getContent();
    }

  }

}
