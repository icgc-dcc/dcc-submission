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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Iterables.tryFind;

import java.util.Map;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.core.util.Guavas;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

/**
 * Generic utils method to traver dictionaries and codelist arrays.
 */
public class Dictionaries {

  /**
   * Do not touch those without modifying their counterparts in the Dictionary/FileSchema object model (in sub-module).
   */
  public static final String FILE_SCHEMATA_KEY = "files";
  public static final String FILE_SCHEMA_NAME_KEY = "name";
  public static final String FILE_SCHEMA_PATTERN_KEY = "pattern";
  public static final String FIELDS_KEY = "fields";
  public static final String RESTRICTIONS_KEY = "restrictions";
  public static final String CODELIST_KEY = "codelist";
  public static final String TYPE_KEY = "type";
  public static final String CONFIG_KEY = "config";
  public static final String CONFIG_NAME_KEY = "name";
  public static final String FIELD_NAME_KEY = "name";

  public static final String CODELIST_NAME_KEY = "name";
  public static final String TERMS_KEY = "terms";
  public static final String CODELIST_VALUE_KEY = "value";
  public static final String CODELIST_CODE_KEY = "code";

  public static Map<FileType, String> getPatterns(@NonNull final JsonNode dictionaryRoot) {

    return Guavas.<JsonNode, FileType, String> transformListToMap(
        dictionaryRoot.path(FILE_SCHEMATA_KEY),

        // Key function
        new Function<JsonNode, FileType>() {

          @Override
          public FileType apply(JsonNode node) {
            return FileType.from(getString(node, FILE_SCHEMA_NAME_KEY));
          }

        },

        // Value function
        new Function<JsonNode, String>() {

          @Override
          public String apply(JsonNode node) {
            return getString(node, FILE_SCHEMA_PATTERN_KEY);
          }

        });
  }

  /**
   * Returns the name of a codelist for a given field. Field is expected to have a codelist restriction defined on it.
   */
  public static String getCodeListName(
      @NonNull final JsonNode dictionaryRoot,
      @NonNull final FileType fileType,
      @NonNull final String fieldName) {
    val codeListRestriction = getCodeListRestriction(dictionaryRoot, fileType, fieldName);
    checkState(codeListRestriction.isPresent(),
        "Expecting codelist to exists for: '%s.%s'", fileType, fieldName);

    return codeListRestriction
        .get()
        .path(CONFIG_KEY)
        .path(CONFIG_NAME_KEY)
        .asText();
  }

  private static JsonNode getFileSchema(
      @NonNull final JsonNode dictionaryRoot,
      @NonNull final FileType fileType) {

    return find(
        dictionaryRoot.path(FILE_SCHEMATA_KEY),
        new Predicate<JsonNode>() {

          @Override
          public boolean apply(JsonNode fileSchema) {
            return fileType == FileType.from(getString(fileSchema, FILE_SCHEMA_NAME_KEY));
          }

        });
  }

  private static JsonNode getField(
      @NonNull final JsonNode dictionaryRoot,
      @NonNull final FileType fileType,
      @NonNull final String fieldName) {

    return find(
        getFileSchema(dictionaryRoot, fileType)
            .path(FIELDS_KEY),
        new Predicate<JsonNode>() {

          @Override
          public boolean apply(JsonNode field) {
            return fieldName.equals(getString(field, FIELD_NAME_KEY));
          }

        });
  }

  private static JsonNode getRestrictions(
      @NonNull final JsonNode dictionaryRoot,
      @NonNull final FileType fileType,
      @NonNull final String fieldName) {

    return getField(dictionaryRoot, fileType, fieldName)
        .path(RESTRICTIONS_KEY);
  }

  /**
   * There should be only 1 if any.
   */
  private static Optional<JsonNode> getCodeListRestriction(
      @NonNull final JsonNode dictionaryRoot,
      @NonNull final FileType fileType,
      @NonNull final String fieldName) {

    return tryFind(
        getRestrictions(dictionaryRoot, fileType, fieldName),
        new Predicate<JsonNode>() {

          @Override
          public boolean apply(JsonNode restriction) {
            return CODELIST_KEY.equals(getString(restriction, TYPE_KEY));
          }

        });
  }

  public static Map<String, String> getMapping(
      @NonNull final JsonNode codeListsRoot,
      @NonNull final String codeListName) {
    checkArgument(codeListsRoot.isArray(), // By design
        "Codelist json file is expected to have an array as root node, instead got: '%s'",
        codeListsRoot.getNodeType());
    val codeListTerms = getCodeListTerms(codeListsRoot, codeListName);
    checkState(codeListTerms.isArray()); // By design

    return Guavas.transformListToMap(
        codeListTerms,
        new Function<JsonNode, String>() {

          @Override
          public String apply(JsonNode term) {
            return getString(term, CODELIST_CODE_KEY);
          }

        },
        new Function<JsonNode, String>() {

          @Override
          public String apply(JsonNode term) {
            return getString(term, CODELIST_VALUE_KEY);
          }

        });
  }

  private static JsonNode getCodeListTerms(
      @NonNull final JsonNode codeListsRoot,
      @NonNull final String codeListName) {

    return getCodeList(codeListsRoot, codeListName)
        .path(TERMS_KEY);
  }

  private static JsonNode getCodeList(
      @NonNull final JsonNode codeListsRoot,
      @NonNull final String codeListName) {

    return find(
        codeListsRoot,
        new Predicate<JsonNode>() {

          @Override
          public boolean apply(JsonNode codeList) {
            return codeListName.equals(getString(codeList, CODELIST_NAME_KEY));
          }

        });
  }

  public static Map<String, String> getMapping(
      @NonNull final JsonNode dictionaryRoot,
      @NonNull final JsonNode codeListsRoot,
      @NonNull final FileType fileType,
      @NonNull final String fieldName) {

    return getMapping(
        codeListsRoot,
        getCodeListName(
            dictionaryRoot,
            fileType,
            fieldName));
  }

  private static String getString(
      @NonNull final JsonNode node,
      @NonNull final String key) {

    return node
        .path(key)
        .asText();
  }

}
