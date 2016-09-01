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
package org.icgc.dcc.submission.validation.key.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableMap;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.CONDITIONAL_RELATION;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.OPTIONAL_RELATION;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.RELATION;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.SURJECTION;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.UNIQUENESS;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.CONDITIONAL_FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.OPTIONAL_FK;
import static org.icgc.dcc.submission.validation.key.core.KVKeyType.PK;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Relation;
import org.icgc.dcc.submission.dictionary.model.RestrictionType;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;

@RequiredArgsConstructor
public class KVDynamicDictionary implements KVDictionary {

  private final Dictionary dictionary;

  @Override
  public KVFileTypeKeysIndices getKeysIndices(@NonNull KVFileType fileType) {
    val fileSchema = getFileSchema(fileType);
    val schemaFieldNames = fileSchema.fieldNames();

    // Primary keys
    val builder = ImmutableMap.<KVKeyType, Multimap<KVFileType, Integer>> builder();
    val pks = getKeysMultimap();
    val primaryKeys = getPrimaryKeyNames(fileType);
    pks.putAll(fileType, resolveKeyIndices(schemaFieldNames, primaryKeys));
    builder.put(PK, pks);

    val relations = fileSchema.getRelations();
    // We assumed that optionals mean which fields should be used to relate to the other file type.
    // E.g. biomarker has the following structure. Optionals '1' means that 'specimen_id' should be used
    // @formatter:off
    //      {
    //        "fields": [
    //          "donor_id",
    //          "specimen_id"
    //        ],
    //        "bidirectional": false,
    //        "other": "specimen",
    //        "otherFields": [
    //          "donor_id",
    //          "specimen_id"
    //        ],
    //        "optionals": [
    //          1
    //        ]
    //      }
    // @formatter:on
    // resolveForeignKeys() and resolveOptionalKeys() take into consideration optionals when resolve keys
    val foreignKeys = resolveForeignKeys(schemaFieldNames, relations, fileSchema);
    val optionalKeys = resolveOptionalKeys(schemaFieldNames, relations, fileSchema);
    val conditionalKeys = resolveConditionalKeys(schemaFieldNames, relations, fileSchema);

    builder.put(FK, foreignKeys);
    builder.put(OPTIONAL_FK, optionalKeys);
    builder.put(CONDITIONAL_FK, conditionalKeys);

    return new KVFileTypeKeysIndices(builder.build(), createConditionEvaluators(fileType));
  }

  @Override
  public List<String> getErrorFieldNames(@NonNull KVFileType fileType, @NonNull KVErrorType errorType,
      @NonNull Optional<KVFileType> optionalReferencedFileType) {
    if (errorType == UNIQUENESS) {
      return getPrimaryKeyNames(fileType);
    }

    val referencedFileType = optionalReferencedFileType.get();
    // Return parent's fields for surjection
    if (errorType == SURJECTION) {
      return getPrimaryKeyNames(referencedFileType);
    }

    val fileSchema = getFileSchema(fileType);
    val relationsStream = fileSchema.getRelations().stream()
        .filter(relation -> referencedFileType == KVFileType.from(relation.getOtherFileType()));

    checkArgument(optionalReferencedFileType.isPresent(), "Referenced file type must be provided for '%s' error "
        + "type", KVErrorType.class.getSimpleName());

    if (errorType == RELATION) {
      val errorFileds = relationsStream
          .filter(relation -> !hasOptionalFk(relation, fileSchema))
          .map(relation -> getFileTypeFKs(relation, fileSchema))
          .findFirst();
      checkState(errorFileds.isPresent(), "Failed to resolve error fields for file type '%s'; error type '%s' and "
          + "referenced file type '%s'", fileType, errorType, referencedFileType);

      return errorFileds.get();
    }

    if (errorType == OPTIONAL_RELATION) {
      val errorFileds = relationsStream
          .filter(relation -> hasOptionalFk(relation, fileSchema))
          .map(relation -> getOptionalFileTypeFKs(relation, fileSchema))
          .findFirst();
      checkState(errorFileds.isPresent(), "Failed to resolve error fields for file type '%s'; error type '%s' and "
          + "referenced file type '%s'", fileType, errorType, referencedFileType);

      return errorFileds.get();
    }

    if (errorType == CONDITIONAL_RELATION) {
      val errorFileds = relationsStream
          .filter(relation -> hasConditionalFk(relation))
          .map(relation -> getConditionalFileTypeFKs(relation, fileSchema))
          .findFirst();
      checkState(errorFileds.isPresent(), "Failed to resolve error fields for file type '%s'; error type '%s' and "
          + "referenced file type '%s'", fileType, errorType, referencedFileType);

      return errorFileds.get();
    }

    throw new IllegalArgumentException(format("Failed to resolve error filed names for error type '%s'", errorType));
  }

  @Override
  public List<String> getPrimaryKeyNames(@NonNull KVFileType fileType) {
    val fileSchema = getFileSchema(fileType);

    return fileSchema.getUniqueFields().stream()
        .sorted()
        .collect(toImmutableList());
  }

  @Override
  public Iterable<KVFileType> getTopologicallyOrderedFileTypes() {
    return KVDynamicDictionaryHelper.getTopologicallyOrderedFileTypes(dictionary);
  }

  @Override
  public Collection<KVFileType> getParents(@NonNull KVFileType fileType) {
    return dictionary.getParents(fileType.getFileType()).stream()
        .map(KVFileType::from)
        .collect(toImmutableList());
  }

  @Override
  public boolean hasChildren(@NonNull KVFileType fileType) {
    return dictionary.getFiles().stream()
        .flatMap(file -> file.getRelations().stream())
        .map(relation -> relation.getOtherFileType())
        .map(KVFileType::from)
        .anyMatch(ft -> ft == fileType);
  }

  @Override
  public Collection<KVFileType> getSurjectiveReferencedTypes(@NonNull KVFileType fileType) {
    val fileSchema = getFileSchema(fileType);

    return fileSchema.getRelations().stream()
        .filter(Relation::isBidirectional)
        .map(Relation::getOtherFileType)
        .map(KVFileType::from)
        .collect(toImmutableList());
  }

  private Map<KVFileType, RowConditionEvaluator> createConditionEvaluators(@NonNull KVFileType fileType) {
    val conditions = resolveConditions(fileType);
    val conditionFields = getTypeFields(fileType, dictionary);

    return conditions.entrySet().stream()
        .collect(toImmutableMap(entry -> entry.getKey(),
            entry -> new RowConditionEvaluator(entry.getValue(), conditionFields)));
  }

  private Map<KVFileType, String> resolveConditions(KVFileType fileType) {
    val fileSchema = getFileSchema(fileType);
    val relations = fileSchema.getRelations();
    val conditions = resolveConditions(relations);

    return conditions;
  }

  private FileSchema getFileSchema(KVFileType fileType) {
    return dictionary.getFileSchema(fileType.getFileType());
  }

  private static List<Integer> resolveKeyIndices(List<String> fieldNames, List<String> primaryKeys) {
    return primaryKeys.stream()
        .map(pk -> fieldNames.indexOf(pk))
        .collect(toImmutableList());
  }

  private static Multimap<KVFileType, Integer> resolveForeignKeys(List<String> schemaFieldNames,
      List<Relation> relations, FileSchema fileSchema) {
    val foreignKeys = ArrayListMultimap.<KVFileType, Integer> create();

    for (val relation : relations) {
      if (hasOptionalFk(relation, fileSchema)) {
        continue;
      }

      val type = relation.getOtherFileType();
      val fkFields = getFileTypeFKs(relation, fileSchema);
      val fkIndices = resolveKeyIndices(schemaFieldNames, fkFields);
      foreignKeys.putAll(KVFileType.from(type), fkIndices);
    }

    return foreignKeys;
  }

  private static Multimap<KVFileType, Integer> resolveOptionalKeys(List<String> schemaFieldNames,
      List<Relation> relations, FileSchema fileSchema) {
    val foreignKeys = ArrayListMultimap.<KVFileType, Integer> create();

    for (val relation : relations) {
      if (!hasOptionalFk(relation, fileSchema)) {
        continue;
      }

      val type = relation.getOtherFileType();
      val fkFields = getOptionalFileTypeFKs(relation, fileSchema);
      val fkIndices = resolveKeyIndices(schemaFieldNames, fkFields);
      foreignKeys.putAll(KVFileType.from(type), fkIndices);
    }

    return foreignKeys;
  }

  private static Multimap<KVFileType, Integer> resolveConditionalKeys(List<String> schemaFieldNames,
      List<Relation> relations, FileSchema fileSchema) {
    val conditionalKeys = getKeysMultimap();
    for (val relation : relations) {
      if (!hasConditionalFk(relation)) {
        continue;
      }

      val type = relation.getOtherFileType();
      val fkFields = getConditionalFileTypeFKs(relation, fileSchema);
      val fkIndices = resolveKeyIndices(schemaFieldNames, fkFields);
      conditionalKeys.putAll(KVFileType.from(type), fkIndices);
    }

    return conditionalKeys;
  }

  private static Map<KVFileType, String> resolveConditions(List<Relation> relations) {
    val conditions = ImmutableMap.<KVFileType, String> builder();
    for (val relation : relations) {
      val condition = relation.getCondition();
      if (!isNullOrEmpty(condition)) {
        val type = relation.getOtherFileType();
        conditions.put(KVFileType.from(type), condition);
      }
    }

    return conditions.build();
  }

  private static boolean hasConditionalFk(Relation relation) {
    return !isNullOrEmpty(relation.getCondition());
  }

  private static boolean hasOptionalFk(Relation relation, FileSchema fileSchema) {
    return relation.getFields().stream()
        .anyMatch(fkField -> isOptionalFk(fileSchema, fkField));
  }

  private static List<String> getFileTypeFKs(Relation relation, FileSchema fileSchema) {
    val optionalFields = resolveOptionalFields(relation);

    return relation.getFields().stream()
        .filter(fkField -> optionalFields.isEmpty() || optionalFields.contains(fkField))
        .filter(fkField -> !isOptionalFk(fileSchema, fkField))
        .sorted()
        .collect(toImmutableList());
  }

  private static List<String> getOptionalFileTypeFKs(Relation relation, FileSchema fileSchema) {
    val optionalFields = resolveOptionalFields(relation);

    return relation.getFields().stream()
        .filter(fkField -> optionalFields.isEmpty() || optionalFields.contains(fkField))
        .filter(fkField -> isOptionalFk(fileSchema, fkField))
        .sorted()
        .collect(toImmutableList());
  }

  private static List<String> getConditionalFileTypeFKs(Relation relation, FileSchema fileSchema) {
    return relation.getFields().stream()
        .sorted()
        .collect(toImmutableList());
  }

  private static List<String> resolveOptionalFields(Relation relation) {
    val optionals = relation.getOptionals();
    if (optionals == null || optionals.isEmpty()) {
      return emptyList();
    }

    val fields = relation.getFields();

    return optionals.stream()
        .map(index -> fields.get(index))
        .collect(toImmutableList());
  }

  private static boolean isOptionalFk(FileSchema fileSchema, String fkField) {
    val field = fileSchema.getField(fkField);

    return field.getRestrictions().stream()
        .filter(rest -> rest.getType() == RestrictionType.REQUIRED)
        .allMatch(rest -> rest.getConfig().getBoolean("acceptMissingCode"));
  }

  private static Multimap<KVFileType, Integer> getKeysMultimap() {
    return ArrayListMultimap.<KVFileType, Integer> create();
  }

  private static List<String> getTypeFields(KVFileType key, Dictionary dictionary) {
    return dictionary.getFileSchema(key.getFileType()).fieldNames();
  }

}
