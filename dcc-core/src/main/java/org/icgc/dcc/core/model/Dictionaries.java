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
package org.icgc.dcc.core.model;

import java.util.Map;

import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.core.util.Guavas;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;

/**
 * 
 */
public class Dictionaries {

  /**
   * Do not touch those without modifying their counterparts in the Dictionary/FileSchema object model (in sub-module).
   */
  public static final String FILE_SCHEMATA_KEY = "files";
  public static final String FILE_SCHEMA_NAME_KEY = "name";
  public static final String FILE_SCHEMA_PATTERN_KEY = "pattern";

  public static Map<FileType, String> getPatterns(JsonNode root) {
    return Guavas.<JsonNode, FileType, String> transformListToMap(
        root.path(FILE_SCHEMATA_KEY),

        // Key function
        new Function<JsonNode, FileType>() {

          @Override
          public FileType apply(JsonNode node) {
            return FileType.from(node.path(FILE_SCHEMA_NAME_KEY).asText());
          }

        },

        // Value function
        new Function<JsonNode, String>() {

          @Override
          public String apply(JsonNode node) {
            return node.path(FILE_SCHEMA_PATTERN_KEY).asText();
          }

        });
  }

}
