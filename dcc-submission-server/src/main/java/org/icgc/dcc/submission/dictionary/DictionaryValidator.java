/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.dictionary;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newCopyOnWriteArraySet;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.split;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_DONOR_ID;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME_END;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_CHROMOSOME_START;
import static org.icgc.dcc.common.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_MUTATION_TYPE;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.model.BusinessKeys;
import org.icgc.dcc.common.core.model.ValueType;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.dictionary.model.RestrictionType;
import org.icgc.dcc.submission.dictionary.model.SummaryType;
import org.icgc.dcc.submission.validation.primary.restriction.CodeListRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.DiscreteValuesRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.RangeFieldRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.ScriptRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.ScriptRestriction.InvalidScriptException;

import com.google.common.base.Function;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.Table;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;

@Slf4j
public class DictionaryValidator {

  private final Dictionary dictionary;
  private final DictionaryIndex dictionaryIndex;
  private final CodeListIndex codeListIndex;

  public DictionaryValidator(Dictionary dictionary, Iterable<CodeList> codeLists) {
    this.dictionary = checkNotNull(dictionary);
    this.dictionaryIndex = new DictionaryIndex(dictionary);
    this.codeListIndex = new CodeListIndex(codeLists);
  }

  public DictionaryConstraintViolations validate() {
    Set<DictionaryConstraintViolation> errors = newLinkedHashSet();
    Set<DictionaryConstraintViolation> warnings = newLinkedHashSet();

    try {
      validateSchemata(errors, warnings);
      validateCodeLists(errors, warnings);
    } catch (Exception e) {
      log.error("Exception validating:", e);
      errors.add(new DictionaryConstraintViolation("Exception validating", e.getMessage()));
    }

    return new DictionaryConstraintViolations(warnings, errors);
  }

  private void validateSchemata(Set<DictionaryConstraintViolation> errors, Set<DictionaryConstraintViolation> warnings) {
    for (val schema : dictionary.getFiles()) {
      try {
        if (isBlank(schema.getPattern())) {
          errors.add(new DictionaryConstraintViolation("Missing schema file pattern", schema.getName()));
        } else {
          Pattern.compile(schema.getPattern());
        }
      } catch (PatternSyntaxException e) {
        errors.add(new DictionaryConstraintViolation("Invalid schema file pattern", schema.getName(), schema
            .getPattern()));
      }

      validateFieldNames(errors, schema);
      validateFields(errors, warnings, schema);
      validateRelations(errors, schema);
    }

    validateBusinessKeys(errors, warnings);
  }

  private void validateFields(Set<DictionaryConstraintViolation> errors, Set<DictionaryConstraintViolation> warnings,
      FileSchema schema) {
    for (val field : schema.getFields()) {
      Set<RestrictionType> restrictionTypes =
          newCopyOnWriteArraySet(dictionaryIndex.getRestrictionTypes(schema.getName(), field.getName()));
      restrictionTypes.remove(RestrictionType.REQUIRED);
      if (restrictionTypes.size() > 2) {
        errors.add(new DictionaryConstraintViolation("Incompatible field restrictions", schema.getName(), field
            .getName(),
            restrictionTypes));
      }

      val summaryType = field.getSummaryType();
      if (summaryType == SummaryType.FREQUENCY && field.getValueType().isNumeric()) {
        warnings.add(new DictionaryConstraintViolation("Potentially large field summary value set", schema, field,
            summaryType));
      }
      if (summaryType == SummaryType.AVERAGE && !field.getValueType().isNumeric()) {
        errors.add(new DictionaryConstraintViolation("Incompatible numeric field summary type", schema, field,
            summaryType));
      }
      if (summaryType == SummaryType.FREQUENCY && schema.getUniqueFields().size() == 1
          && schema.getUniqueFields().contains(field.getName())) {
        warnings.add(new DictionaryConstraintViolation("Frequency defined for unique field", schema, field,
            summaryType,
            schema.getUniqueFields()));
      }

      validateRestrictions(errors, schema, field);
    }
  }

  private void validateRestrictions(Set<DictionaryConstraintViolation> errors, FileSchema schema, Field field) {
    for (val restriction : field.getRestrictions()) {
      val config = restriction.getConfig();
      if (restriction.getType() == null) {
        errors.add(new DictionaryConstraintViolation("Field restriction type is blank", schema, field, restriction));
      }

      if (restriction.getType() == RestrictionType.CODELIST) {
        String codeListName = config.getString(CodeListRestriction.FIELD);
        if (isBlank(codeListName)) {
          errors.add(new DictionaryConstraintViolation("Field code list name is blank", schema, field, restriction));
        } else if (!codeListIndex.has(codeListName)) {
          errors
              .add(new DictionaryConstraintViolation("Field invalid code list reference", schema, field, restriction));
        }
      }

      if (restriction.getType() == RestrictionType.DISCRETE_VALUES) {
        String text = config.getString(DiscreteValuesRestriction.PARAM);
        String[] values = split(text, ",");
        for (val value : values) {
          if (isBlank(value)) {
            errors.add(new DictionaryConstraintViolation("Blank discrete value", schema, field, restriction));
            break;
          }
        }
      }

      if (restriction.getType() == RestrictionType.RANGE) {
        String min = config.getString(RangeFieldRestriction.MIN);
        String max = config.getString(RangeFieldRestriction.MAX);
        if (!field.getValueType().isNumeric()) {
          errors.add(new DictionaryConstraintViolation("Non-numeric range field value type", schema, field,
              restriction,
              field.getValueType()));
        }
        if (field.getValueType() == ValueType.INTEGER && Longs.tryParse(min) == null) {
          errors.add(new DictionaryConstraintViolation("Non INTEGER range min value", schema, field, restriction, min));
        }
        if (field.getValueType() == ValueType.DECIMAL && Doubles.tryParse(min) == null) {
          errors.add(new DictionaryConstraintViolation("Non DECIMAL range min value", schema, field, restriction, min));
        }
        if (field.getValueType() == ValueType.INTEGER && Longs.tryParse(max) == null) {
          errors.add(new DictionaryConstraintViolation("Non INTEGER range max value", schema, field, restriction, max));
        }
        if (field.getValueType() == ValueType.DECIMAL && Doubles.tryParse(max) == null) {
          errors.add(new DictionaryConstraintViolation("Non DECIMAL range max value", schema, field, restriction, max));
        }
      }

      if (restriction.getType() == RestrictionType.SCRIPT) {
        val description = config.getString(ScriptRestriction.PARAM_DESCRIPTION);
        if (isBlank(description)) {
          errors.add(new DictionaryConstraintViolation("Script restriction is missing description parameter",
              schema, field, restriction));
        }

        val script = config.getString(ScriptRestriction.PARAM);
        if (isBlank(script)) {
          errors.add(new DictionaryConstraintViolation("Script restriction is missing script parameter",
              schema, field, restriction));

          continue;
        }

        try {
          val scriptContext = new ScriptRestriction.ScriptContext(script);

          val inputs = scriptContext.getInputs();
          for (val inputName : inputs.keySet()) {
            Field inputField = dictionaryIndex.getField(schema.getName(), inputName);
            if (inputField == null) {
              errors.add(new DictionaryConstraintViolation("File schema is missing referenced script field",
                  schema, field, restriction, script, inputName));

              continue;
            }
          }
        } catch (InvalidScriptException e) {
          errors.add(new DictionaryConstraintViolation(e.getMessage(), schema, field, restriction, script));
        }
      }
    }
  }

  private void validateFieldNames(Set<DictionaryConstraintViolation> errors, FileSchema schema) {
    val fieldNames = HashMultiset.create(schema.getFieldNames());
    for (String fieldName : fieldNames) {
      if (fieldNames.count(fieldName) > 1) {
        errors.add(new DictionaryConstraintViolation("Duplicate field name", schema.getName(), fieldName));
      }
    }
  }

  private void validateRelations(Set<DictionaryConstraintViolation> errors, FileSchema schema) {
    for (val relation : schema.getRelations()) {
      if (relation.getFields().isEmpty()) {
        errors
            .add(new DictionaryConstraintViolation("Missing schema fields for relation", schema, relation));
      }

      if (relation.getOtherFields().isEmpty()) {
        errors
            .add(new DictionaryConstraintViolation("Missing other schema fields for relation", schema, relation));
      }

      if (relation.getFields().size() != relation.getOtherFields().size()) {
        errors
            .add(new DictionaryConstraintViolation(
                "Mismatching field count between schema and other schema for relation", schema, relation,
                relation.getFields().size(), relation.getOtherFields().size()));
      }

      for (val fieldName : relation.getFields()) {
        if (!dictionaryIndex.hasField(schema.getName(), fieldName)) {
          errors
              .add(new DictionaryConstraintViolation("Invalid schema field for relation", schema, relation, fieldName));
        }
      }
      if (!dictionaryIndex.hasSchema(relation.getOther())) {
        errors.add(new DictionaryConstraintViolation("Invalid other schema for relation", schema, relation, relation
            .getOther()));
      }
      for (val otherFieldName : relation.getOtherFields()) {
        if (!dictionaryIndex.hasField(schema.getName(), otherFieldName)) {
          errors
              .add(new DictionaryConstraintViolation("Invalid other schema field for relation", schema, relation,
                  otherFieldName));
        }
      }

      FileSchema otherSchema = dictionaryIndex.getSchema(relation.getOther());
      SetView<String> difference =
          difference(newHashSet(otherSchema.getUniqueFields()), newHashSet(relation.getOtherFields()));
      if (!difference.isEmpty()) {
        errors.add(new DictionaryConstraintViolation("Other schema fields are not unique for relation", schema,
            relation,
            difference));
      }
    }
  }

  private void validateBusinessKeys(Set<DictionaryConstraintViolation> errors,
      Set<DictionaryConstraintViolation> warnings) {
    FileSchema ssm_p = dictionaryIndex.getSchema("ssm_p");
    if (ssm_p == null) {
      errors.add(new DictionaryConstraintViolation(
          "'ssm_p' schema is missing but is required for required business key field validation"));
    } else {
      // See BusinessKeys.MUTATION_KEY
      // TODO: Add the full set after the dictionary is created
      val relaxedMutationKey = ImmutableList.<String> of(
          SUBMISSION_OBSERVATION_CHROMOSOME,
          SUBMISSION_OBSERVATION_CHROMOSOME_START,
          SUBMISSION_OBSERVATION_CHROMOSOME_END,
          SUBMISSION_OBSERVATION_MUTATION_TYPE);

      for (val keyField : relaxedMutationKey) {
        val required = dictionaryIndex.hasRestrictionType(ssm_p.getName(), keyField, RestrictionType.REQUIRED);
        if (!required) {
          errors.add(new DictionaryConstraintViolation(
              "'ssm_p' schema field is required for downstream processing", keyField, BusinessKeys.MUTATION));
        }
      }
    }

    FileSchema donor = dictionaryIndex.getSchema("donor");
    if (donor == null) {
      errors.add(new DictionaryConstraintViolation(
          "'donor' schema is missing but is required for required business key field validation"));
    } else {
      val keyField = SUBMISSION_DONOR_ID;
      val required = dictionaryIndex.hasRestrictionType(donor.getName(), keyField, RestrictionType.REQUIRED);
      if (!required) {
        errors.add(new DictionaryConstraintViolation(
            "'donor' schema field is required for business key field", keyField));
      }
    }

    // TODO: Add validations for remaining business keys
  }

  private void validateCodeLists(Set<DictionaryConstraintViolation> errors, Set<DictionaryConstraintViolation> warnings) {
    for (val codeListName : dictionary.getCodeListNames()) {
      val collection = codeListIndex.get(codeListName);
      int count = collection.size();
      if (count == 0) {
        warnings.add(new DictionaryConstraintViolation("Missing code list", codeListName));
        break;
      }
      if (count > 1) {
        errors.add(new DictionaryConstraintViolation("Duplicate code lists", collection));
      }

      val codeList = getFirst(collection, null);

      Multiset<String> codes = HashMultiset.create();
      Multiset<String> values = HashMultiset.create();
      for (val term : codeList.getTerms()) {
        codes.add(term.getCode());
        values.add(term.getValue());
      }

      for (val term : codeList.getTerms()) {
        val code = term.getCode();
        val value = term.getValue();

        if (codes.count(code) > 1) {
          errors.add(new DictionaryConstraintViolation("Duplicate code list codes", term, code, codeList));
        }
        if (values.count(value) > 1) {
          errors.add(new DictionaryConstraintViolation("Duplicate code list values", term, value, codeList));
        }
        if (codes.contains(value) && !code.equals(value)) {
          errors.add(new DictionaryConstraintViolation("Non-disjoint code list code and value", term, value, codeList));
        }
      }
    }
  }

  public static class DictionaryIndex {

    final Map<String, FileSchema> schemata = newHashMap();
    final Table<String, String, Field> fields = HashBasedTable.create();
    final Table<String, String, List<Restriction>> restrictions = HashBasedTable.create();
    final Table<String, String, Multimap<RestrictionType, Restriction>> restrictionTypes = HashBasedTable.create();

    public DictionaryIndex(Dictionary dictionary) {
      index(dictionary);
    }

    private void index(Dictionary dictionary) {
      for (val schema : dictionary.getFiles()) {
        schemata.put(schema.getName(), schema);
        for (val field : schema.getFields()) {
          fields.put(schema.getName(), field.getName(), field);
          restrictions.put(schema.getName(), field.getName(), field.getRestrictions());
          restrictionTypes.put(schema.getName(), field.getName(), indexByType(field.getRestrictions()));
        }
      }
    }

    private Multimap<RestrictionType, Restriction> indexByType(Iterable<Restriction> restrictions) {
      return Multimaps.index(restrictions, new Function<Restriction, RestrictionType>() {

        @Override
        public RestrictionType apply(Restriction restriction) {
          return restriction.getType();
        }

      });
    }

    public FileSchema getSchema(String schemaName) {
      return schemata.get(schemaName);
    }

    public boolean hasSchema(String schemaName) {
      return schemata.containsKey(schemaName);
    }

    public Field getField(String schemaName, String fieldName) {
      return fields.get(schemaName, fieldName);
    }

    public boolean hasField(String schemaName, String fieldName) {
      return fields.contains(schemaName, fieldName);
    }

    public List<Restriction> getRestrictions(String schemaName, String fieldName) {
      return restrictions.get(schemaName, fieldName);
    }

    public boolean hasRestrictions(String schemaName, String fieldName) {
      return restrictions.contains(schemaName, fieldName);
    }

    public Set<RestrictionType> getRestrictionTypes(String schemaName, String fieldName) {
      val types = restrictionTypes.get(schemaName, fieldName);
      return types == null ? new HashSet<RestrictionType>() : types.keySet();
    }

    public Multimap<RestrictionType, Restriction> getRestrictionType(String schemaName, String fieldName, String type) {
      return restrictionTypes.get(schemaName, fieldName);
    }

    public boolean hasRestrictionType(String schemaName, String fieldName, RestrictionType type) {
      val types = restrictionTypes.get(schemaName, fieldName);
      return types != null && types.containsKey(type);
    }

  }

  public static class CodeListIndex {

    private final Multimap<String, CodeList> codeLists;

    public CodeListIndex(Iterable<CodeList> codeLists) {
      this.codeLists = index(codeLists);
    }

    private Multimap<String, CodeList> index(Iterable<CodeList> codeLists) {
      return Multimaps.index(codeLists, new Function<CodeList, String>() {

        @Override
        public String apply(CodeList codeList) {
          return codeList.getName();
        }

      });
    }

    public Collection<CodeList> get(String name) {
      return codeLists.get(name);
    }

    public boolean has(String name) {
      return codeLists.containsKey(name);
    }

  }

  @Value
  public static class DictionaryConstraintViolations {

    private final Set<DictionaryConstraintViolation> warnings;
    private final Set<DictionaryConstraintViolation> errors;

    public boolean hasWarnings() {
      return !warnings.isEmpty();
    }

    public boolean hasErrors() {
      return !errors.isEmpty();
    }

  }

  @Value
  public static class DictionaryConstraintViolation {

    private final String description;
    private final Object[] context;

    public DictionaryConstraintViolation(String description, Object... context) {
      this.description = description;
      this.context = context;
    }

  }

}
