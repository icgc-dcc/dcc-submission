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

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableMap;
import static org.icgc.dcc.common.json.Jackson.asArrayNode;
import static org.icgc.dcc.common.json.Jackson.asObjectNode;
import static org.icgc.dcc.submission.loader.util.Fields.PROJECT_ID;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.common.core.meta.Resolver.DictionaryResolver;
import org.icgc.dcc.common.core.util.stream.Streams;
import org.icgc.dcc.submission.loader.model.TypeDef;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class SubmissionMetadataService {

  private final Map<String, TypeDef> fileTypes;
  private final ObjectNode dictionary;

  public SubmissionMetadataService(@NonNull DictionaryResolver dictionaryResolver, @NonNull String dictionaryVersion) {
    this.dictionary = dictionaryResolver.apply(Optional.of(dictionaryVersion));
    this.fileTypes = resolveFileTypes();
  }

  public TypeDef getTypeDef(@NonNull String type) {
    checkType(type);

    return fileTypes.get(type);
  }

  public Map<String, String> getFilePatterns() {
    val filePatterns = ImmutableMap.<String, String> builder();
    for (val file : dictionary.get("files")) {
      val name = file.get("name").textValue();
      val pattern = file.get("pattern").textValue();
      filePatterns.put(name, pattern);
    }

    return filePatterns.build();
  }

  public List<String> getPrimaryKey(@NonNull String type) {
    checkType(type);

    val dictTypeDef = getDictionaryTypeDef(type);
    val primaryKey = ImmutableList.<String> builder();

    for (val element : dictTypeDef.get("uniqueFields")) {
      primaryKey.add(element.textValue());
    }
    primaryKey.add(PROJECT_ID);

    return primaryKey.build();
  }

  public Map<String, String> getParentPrimaryKey(@NonNull String type, @NonNull String parent) {
    checkType(type);
    checkType(parent);

    val dictTypeDef = getDictionaryTypeDef(type);
    for (val relation : dictTypeDef.get("relations")) {
      if (isParentRelation(relation, parent)) {
        val childPks = getChildPrimaryKey(relation);
        val parentPks = getParentPrimaryKey(relation);

        return mapChildParentPrimaryKeys(childPks, parentPks);
      }
    }

    return Collections.emptyMap();
  }

  public List<String> getChildPrimaryKey(@NonNull String type, @NonNull String parent) {
    checkType(type);
    checkType(parent);

    val dictTypeDef = getDictionaryTypeDef(type);
    for (val relation : dictTypeDef.get("relations")) {
      if (isParentRelation(relation, parent)) {
        return getChildPrimaryKey(relation);
      }
    }

    return Collections.emptyList();
  }

  public Collection<TypeDef> getFileTypes() {
    return fileTypes.values();
  }

  public Map<String, String> getFields(@NonNull String type) {
    checkType(type);

    val fields = ImmutableMap.<String, String> builder();
    for (val field : getTypeFields(type)) {
      val fieldName = field.get("name").textValue();
      val valueType = field.get("valueType").textValue();
      fields.put(fieldName, valueType);
    }

    return fields.build();
  }

  public Collection<String> getChildren(@NonNull String type) {
    return fileTypes.values().stream()
        .filter(typeDef -> typeDef.getParent().contains(type))
        .map(typeDef -> typeDef.getType())
        .collect(toImmutableList());
  }

  public Collection<String> getParent(@NonNull String type) {
    checkType(type);

    return getTypeDef(type).getParent();
  }

  public void checkType(@NonNull String type) {
    checkArgument(fileTypes.keySet().contains(type), "%s is not a valid file type", type);
  }

  private Map<String, String> mapChildParentPrimaryKeys(List<String> childPks, List<String> parentPks) {
    checkArgument(childPks.size() == parentPks.size(), "Different number of child and parent primary keys");

    val mappedPks = ImmutableMap.<String, String> builder();
    for (int i = 0; i < childPks.size(); i++) {
      mappedPks.put(childPks.get(i), parentPks.get(i));
    }

    return mappedPks.build();
  }

  private ArrayNode getTypeFields(String type) {
    return asArrayNode(getDictionaryTypeDef(type).get("fields"));
  }

  private ObjectNode getDictionaryTypeDef(String type) {
    for (val file : dictionary.get("files")) {
      val fileName = file.get("name").textValue();
      if (type.equals(fileName)) {
        return asObjectNode(file);
      }
    }

    throw new IllegalArgumentException(format("Failed to find type '%s'", type));
  }

  private Map<String, TypeDef> resolveFileTypes() {
    val files = asArrayNode(dictionary.get("files"));

    return Streams.stream(files)
        .map(file -> createTypeDef(asObjectNode(file)))
        .collect(toImmutableMap(td -> td.getType(), td -> td));
  }

  private static TypeDef createTypeDef(ObjectNode file) {
    val type = file.get("name").textValue();
    val parent = resolveParent(file.path("relations"));

    return new TypeDef(type, parent);
  }

  private static Collection<String> resolveParent(JsonNode relations) {
    if (relations.isMissingNode()) {
      return Collections.emptySet();
    }

    val parents = ImmutableSet.<String> builder();
    for (val relation : asArrayNode(relations)) {
      val parent = relation.get("other").textValue();
      parents.add(parent);
    }

    return parents.build();
  }

  private static List<String> getParentPrimaryKey(JsonNode relation) {
    val parentPrimaryKeys = ImmutableList.<String> builder();
    for (val element : relation.get("otherFields")) {
      parentPrimaryKeys.add(element.textValue());
    }

    return parentPrimaryKeys.build();
  }

  private static List<String> getChildPrimaryKey(JsonNode relation) {
    val parentPrimaryKeys = ImmutableList.<String> builder();
    for (val element : relation.get("fields")) {
      parentPrimaryKeys.add(element.textValue());
    }

    return parentPrimaryKeys.build();
  }

  private static boolean isParentRelation(JsonNode relation, String parent) {
    return parent.equals(relation.get("other").textValue());
  }

}
