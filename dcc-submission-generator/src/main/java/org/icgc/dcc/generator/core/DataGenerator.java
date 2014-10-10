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

package org.icgc.dcc.generator.core;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.shuffle;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.icgc.dcc.generator.utils.Dictionaries.getSchemaType;
import static org.icgc.dcc.generator.utils.Dictionaries.isCodeListField;
import static org.icgc.dcc.generator.utils.Dictionaries.isMissingCodeAccepted;
import static org.icgc.dcc.generator.utils.Dictionaries.isRequired;
import static org.icgc.dcc.generator.utils.Dictionaries.isUniqueField;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.common.core.model.SpecialValue;
import org.icgc.dcc.common.core.model.ValueType;
import org.icgc.dcc.generator.model.CodeListTerms;
import org.icgc.dcc.generator.model.Key;
import org.icgc.dcc.generator.utils.CodeLists;
import org.icgc.dcc.generator.utils.RegexMatches;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Relation;
import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.dictionary.model.RestrictionType;
import org.icgc.dcc.submission.dictionary.model.Term;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

/**
 * The DataGenerator generates all the data that's required for the various files. The DataGenerator also holds an array
 * of codeListTerms for each file, and a list of keys. The list of keys are referred to both as primary keys and foreign
 * keys. They're referred to primary keys when used in the context of the file from which the originate and foreign keys
 * when used in the context of the files to which they're applied to
 */
@Slf4j
public class DataGenerator {

  private static final Map<String, String> PRIMARY_KEY_PREFIXES = new ImmutableMap.Builder<String, String>()
      .put("donor", "001")
      .put("specimen", "002")
      .put("sample", "003")
      .put("m", "004")
      .put("p", "005")
      .put("g", "005")
      .put("s", "006")
      .build();

  private static final String CONSTANT_DATE = "20130313";
  private static final String ALPHABET = "abcdefghi jklmnopqrstuvwxyz ABCDEFGHIJKLMNOPQ RSTUVWXYZ";

  final List<Key> keys = newArrayList();
  final List<CodeListTerms> codeListTerms = newArrayList();
  final CodeLists codeLists;

  final Integer stringSize;
  final Random random;

  int uniqueInteger = 0;
  int uniqueId = 0;

  public DataGenerator(CodeLists codeLists, Integer stringSize, Long seed) {
    this.codeLists = codeLists;
    this.stringSize = stringSize;
    this.random = (seed == null) ? new Random() : new Random(seed);
  }

  /**
   * Generate a random char
   */
  public char generateRandomChar(String text) {
    int randomIndex = generateRandomInteger(0, text.length() - 1);
    return text.charAt(randomIndex);
  }

  /**
   * Generate a random String that matches [a-zA-Z ]{stringSize} and is non-empty
   */
  public String generateRandomString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < stringSize; i++) {
      char c = generateRandomChar(ALPHABET);

      sb.append(c);
    }

    String text = sb.toString();

    return isEmpty(text) ? generateRandomString() : text;
  }

  /**
   * Generate a random integer in the range [start, end]
   */
  public int generateRandomInteger(int start, int end) {
    checkState(start <= end, "Start '%s' must not be greater than end '%s'", start, end);

    if (start == end) {
      return start;
    }

    int offset = random.nextInt(end + 1);

    return start + offset;
  }

  /**
   * Generate a random boolean
   */
  public boolean generateRandomBoolean() {
    return generateRandomInteger(0, 1) == 0 ? false : true;
  }

  /**
   * Generate a random double in the range [0, end]
   */
  public double generateRandomDouble(double end) {
    return random.nextDouble() * end;
  }

  public <T> T generateRandomElement(List<T> list) {
    int randomIndex = generateRandomInteger(0, list.size() - 1);
    return list.get(randomIndex);
  }

  public <T> T generateRandomElement(T[] array) {
    int randomIndex = generateRandomInteger(0, array.length - 1);
    return array[randomIndex];
  }

  public <T> void generateRandomOrdering(List<T> list) {
    shuffle(list, random);
  }

  public List<Key> getPrimaryKeys() {
    return this.keys;
  }

  /**
   * Gets the list of values of the key associated with a primary key.
   */
  public List<Integer> getPrimaryKeyValues(String schemaName, String fieldName) {
    for (val key : keys) {
      if (key.matchesOrigin(schemaName, fieldName)) {
        return key.getKeys();
      }
    }

    return null;
  }

  /**
   * Gets the list of values associated with a foreign key.
   */
  public List<Integer> getForeignKeyValues(String schemaName, String fieldName) {
    for (val key : keys) {
      if (key.matchesApplied(schemaName, fieldName)) {
        return key.getKeys();
      }
    }

    return null;
  }

  /**
   * Gets a field value.
   */
  public String getFieldValue(String schemaName, Field field, List<String> uniqueFields) {
    String fieldName = field.getName();

    int randomProbabilityInteger = generateRandomInteger(1, 10);
    boolean isLessLikelyOutcome = randomProbabilityInteger >= 1 && randomProbabilityInteger <= 3;

    if (isRequired(field)) {
      return getRequiredFieldValue(schemaName, field, uniqueFields, fieldName, isLessLikelyOutcome);
    } else {
      return getNonRequiredFieldValue(schemaName, field, uniqueFields, fieldName, isLessLikelyOutcome);
    }
  }

  /**
   * Adds a Key object for every unique field in a schema. Adds the key to the 'keys' field.
   */
  public void addUniqueKeys(FileSchema schema) {
    List<String> uniqueFields = schema.getUniqueFields();
    if (uniqueFields != null) {
      log.info("Building unique key for {}", uniqueFields);
      for (String uniqueFieldName : uniqueFields) {
        String schemaIdentifier = schema.getName();
        String fieldIdentifier = uniqueFieldName;

        Key key = new Key(schemaIdentifier, fieldIdentifier);
        addKey(key);
      }
    } else {
      log.info("No unique key specified for {}", schema.getName());
    }
  }

  public void addKey(Key key) {
    this.keys.add(key);
  }

  public void addFieldValueToKey(String schemaName, String fieldName, Integer fieldValue) {
    getPrimaryKeyValues(schemaName, fieldName).add(fieldValue);
  }

  public void addCodeListTerms(FileSchema schema) {
    for (Field field : schema.getFields()) {
      Optional<Restriction> restriction = field.getRestriction(RestrictionType.CODELIST);
      if (restriction.isPresent()) {
        appendCodeListTerms(schema, field, restriction);
      }
    }
  }

  /**
   * Called at the beginning of generating files to update the fields prefixed with 'applied' in the Key model. These
   * 'applied' prefixed fields are used to identify the foreign field(s) which a given Key is applied to.
   */
  public void addKeyAppliedFields(FileSchema schema) {
    for (val field : schema.getFields()) {
      for (val relation : schema.getRelations()) {
        appendKeyAppliedFields(schema, field.getName(), relation);
      }
    }
  }

  /**
   * Called at the end of generation of each file to reset primary key counters
   */
  public void resetUniqueValueFields() {
    uniqueId = 0;
    uniqueInteger = 0;
  }

  private String generateRandomMissingCode() {
    return generateRandomElement(newArrayList(SpecialValue.FULL_MISSING_CODES));
  }

  private String generateUniqueInteger(String schemaName) {
    return PRIMARY_KEY_PREFIXES.get(getSchemaType(schemaName)) + String.valueOf(uniqueInteger++);
  }

  /**
   * Generates a unique id for fields that are primary keys.
   */
  private String generateUniqueId(String schemaName) {
    return PRIMARY_KEY_PREFIXES.get(getSchemaType(schemaName)) + String.valueOf(uniqueId++);
  }

  /**
   * Generates a unique field value. Assumption is that unique fields can only be of type text and integer.
   * <p>
   * TODO: Must be able to generate values matching a regex as well (DCC-1202)
   */
  private String generateUniqueFieldValue(String schemaName, ValueType fieldValueType) {
    switch (fieldValueType) {
    case TEXT:
      return generateUniqueId(schemaName);
    default:
      return generateUniqueInteger(schemaName);
    }
  }

  /**
   * Generates a field Value.
   */
  private String generateFieldValue(String schemaName, Field field, List<String> uniqueFields) {
    ValueType fieldValueType = field.getValueType();
    boolean hasRegexRestriction = field.hasRegexRestriction();
    boolean uniqueField = isUniqueField(uniqueFields, field.getName());

    if (uniqueField) {
      String value = generateUniqueFieldValue(schemaName, fieldValueType);
      if (hasRegexRestriction) { // See DCC-1202
        checkState(
            matches(getRegexPattern(field), value),
            "Expecting generated unique value '{}' to match '{}'",
            value, getRegexPattern(field));
      }
      return value;
    } else if (hasRegexRestriction) {
      checkState(!uniqueField, // See DCC-1202
          "Unique fields must not be using that mechanism for now");
      return getRegexMatchingValue(field);
    } else {
      switch (fieldValueType) {
      case TEXT:
        return generateRandomString();
      case INTEGER:
        return Integer.toString(generateRandomInteger(0, 200));
      case DECIMAL:
        return Double.toString(generateRandomDouble(50));
      default:
        return CONSTANT_DATE;
      }
    }
  }

  private void appendCodeListTerms(FileSchema schema, Field field, Optional<Restriction> restriction) {
    String codeListName = restriction.get().getConfig().getString(CodeListTerms.CODELIST_CONFIG_NAME);
    List<CodeList> values = codeLists.getCodeLists();
    Iterator<CodeList> iterator = values.iterator();

    while (iterator.hasNext()) {
      CodeList codeList = iterator.next();

      if (codeList.getName().equals(codeListName)) {
        CodeListTerms term = new CodeListTerms(schema.getName(), field.getName(), codeList.getTerms());
        codeListTerms.add(term);
      }
    }
  }

  private void appendKeyAppliedFields(FileSchema schema, String fieldName, Relation relation) {
    int k = 0;
    for (String appliedFieldIdentifier : relation.getFields()) {
      if (appliedFieldIdentifier.equals(fieldName)) {
        setKeyAppliedFields(schema, fieldName, relation, k);
      }
      k++;
    }
  }

  private void setKeyAppliedFields(FileSchema schema, String fieldName, Relation relation, int k) {
    for (Key key : keys) {
      String originSchemaIdentifier = relation.getOther();
      String originFieldIdentifier = relation.getOtherFields().get(k);// foreignKeyFieldName;

      if (key.matchesOrigin(originSchemaIdentifier, originFieldIdentifier)) {
        key.setAppliedSchemaIdentifier(schema.getName());
        if (key.getAppliedFieldIdentifier().contains(fieldName) == false) {
          key.getAppliedFieldIdentifier().add(fieldName);
        }
      }
    }
  }

  /**
   * Retrieves code list value for a give field in a given schema.
   */
  private String getCodeListValue(String schemaName, String fieldName) {
    for (CodeListTerms codeListTerm : codeListTerms) {
      if (codeListTerm.matches(schemaName, fieldName)) {
        List<Term> terms = codeListTerm.getTerms();
        Term randomTerm = generateRandomElement(terms);

        return randomTerm.getCode();
      }
    }

    return null;
  }

  private String getRequiredFieldValue(String schemaName, Field field, List<String> uniqueFields, String fieldName,
      boolean isLessLikelyOutcome) {
    if (isCodeListField(field)) {
      return getRequiredCodeListFieldValue(schemaName, field, fieldName, isLessLikelyOutcome);
    } else {
      return getRequiredNonCodeListFieldValue(schemaName, field, uniqueFields, isLessLikelyOutcome);
    }
  }

  private String getRequiredNonCodeListFieldValue(String schemaName, Field field, List<String> uniqueFields,
      boolean isLessLikelyOutcome) {
    if (isMissingCodeAccepted(field)) {
      return isLessLikelyOutcome ? generateRandomMissingCode() : generateFieldValue(schemaName, field, uniqueFields);
    } else {
      return generateFieldValue(schemaName, field, uniqueFields);
    }
  }

  private String getRequiredCodeListFieldValue(String schemaName, Field field, String fieldName,
      boolean isLessLikelyOutcome) {
    if (isMissingCodeAccepted(field)) {
      return isLessLikelyOutcome ? generateRandomMissingCode() : getCodeListValue(schemaName, fieldName);
    } else {
      return getCodeListValue(schemaName, fieldName);
    }
  }

  private String getNonRequiredFieldValue(String schemaName, Field field, List<String> uniqueFields, String fieldName,
      boolean isLessLikelyOutcome) {
    return isLessLikelyOutcome ? "" : (
        isCodeListField(field) ?
            getCodeListValue(schemaName, fieldName) :
            generateFieldValue(schemaName, field, uniqueFields));
  }

  /**
   * Returns a pre-defined matching value for the regex (see {@link RegexMatches}).
   */
  private String getRegexMatchingValue(Field regexField) {
    String pattern = getRegexPattern(regexField);
    return checkNotNull(
        RegexMatches.MATCHING_VALUES.get(pattern),
        "No matching value found for pattern: '%s', did the dictionary change recently? Last known version was '%s'",
        pattern, RegexMatches.LATEST_KNOWN_DICTIONARY_VERSION);
  }

  /**
   * Those are guaranteed to exist by design.
   */
  private String getRegexPattern(Field regexField) {
    return (String) regexField.getRestriction(RestrictionType.REGEX)
        .get()
        .getConfig()
        .get("pattern");
  }

  private boolean matches(String pattern, String value) {
    return compile(pattern).matcher(value).matches();
  }
}