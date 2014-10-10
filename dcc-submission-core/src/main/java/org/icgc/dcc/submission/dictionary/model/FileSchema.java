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
package org.icgc.dcc.submission.dictionary.model;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.find;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.regex.Pattern.compile;
import static org.icgc.dcc.submission.dictionary.model.Field.IS_CONTROLLED;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.validation.Valid;

import lombok.NonNull;
import lombok.ToString;
import lombok.val;

import org.hibernate.validator.constraints.NotBlank;
import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.core.model.Dictionaries;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.dictionary.visitor.DictionaryElement;
import org.icgc.dcc.submission.dictionary.visitor.DictionaryVisitor;
import org.mongodb.morphia.annotations.Embedded;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Describes a file schema that contains {@code Field}s and that is part of a {@code Dictionary}
 */
@Embedded
@ToString(of = { "name" })
public class FileSchema implements DictionaryElement, Serializable {

  /**
   * TODO: use {@link FileType} instead of String.
   * <p>
   * Related to {@link Dictionaries#FILE_SCHEMA_NAME_KEY}.
   */
  @NotBlank
  private String name;

  private String label;

  /**
   * Related to {@link Dictionaries#FILE_SCHEMA_PATTERN_KEY}.
   */
  private String pattern;

  private FileSchemaRole role;

  private List<String> uniqueFields;

  @Valid
  private List<Field> fields;

  @Valid
  private List<Relation> relations;

  public FileSchema() {
    super();
    this.uniqueFields = new ArrayList<String>();
    this.fields = new ArrayList<Field>();
    this.relations = new ArrayList<Relation>();
  }

  public FileSchema(String name) {
    super();
    this.name = name;
    this.uniqueFields = new ArrayList<String>();
    this.fields = new ArrayList<Field>();
    this.relations = new ArrayList<Relation>();
  }

  @Override
  public void accept(DictionaryVisitor dictionaryVisitor) {
    dictionaryVisitor.visit(this);
    for (Field field : fields) {
      field.accept(dictionaryVisitor);
    }
    for (Relation relation : relations) {
      relation.accept(dictionaryVisitor);
    }
  }

  public Optional<Field> field(final String name) {
    return Iterables.tryFind(fields, new Predicate<Field>() {

      @Override
      public boolean apply(Field input) {
        return input.getName().equals(name);
      }
    });
  }

  public Iterable<String> fieldNames() {
    return Iterables.transform(fields, new Function<Field, String>() {

      @Override
      public String apply(Field input) {
        return input.getName();
      }
    });
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getPattern() {
    return pattern;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public FileSchemaRole getRole() {
    return role;
  }

  public void setRole(FileSchemaRole role) {
    this.role = role;
  }

  public List<String> getUniqueFields() {
    return uniqueFields;
  }

  public void setUniqueFields(List<String> uniqueFields) {
    this.uniqueFields = uniqueFields;
  }

  public List<Field> getFields() {
    return fields;
  }

  public void setFields(List<Field> fields) {
    this.fields = fields;
  }

  public void addField(Field field) {
    this.fields.add(field);
  }

  public String getName() {
    return name;
  }

  public boolean hasField(String fieldName) {
    for (Field field : this.fields) {
      if (field.getName().equals(fieldName)) {
        return true;
      }
    }
    return false;
  }

  public void setRelations(List<Relation> relations) {
    this.relations = relations;
  }

  public List<Relation> getRelations() {
    return ImmutableList.<Relation> copyOf(relations);
  }

  public boolean addRelation(Relation relation) {
    return this.relations.add(relation);
  }

  public void clearRelations() {
    this.relations.clear();
  }

  public boolean containsField(String fieldName) {
    return getFieldNames().contains(fieldName);
  }

  @JsonIgnore
  public List<String> getControlledFieldNames() {
    return newArrayList(getFieldNames(filter(fields, IS_CONTROLLED)));
  }

  static Predicate<Field> HAS_CODELIST_RESTRICTION = new Predicate<Field>() {

    @Override
    public boolean apply(Field field) {
      return field.hasCodeListRestriction();
    }

  };

  @JsonIgnore
  public Set<Field> getEncodedFields() {
    return ImmutableSet.<Field> copyOf(filter(getFields(), HAS_CODELIST_RESTRICTION));
  }

  @JsonIgnore
  public Set<String> getFieldNameSet() {
    return ImmutableSet.<String> copyOf(getFieldNames());
  }

  /**
   * TODO: change to {@link java.util.Set} and delete {@link #getFieldNameSet()}.
   */
  @JsonIgnore
  public List<String> getFieldNames() {
    return newArrayList(getFieldNames(getFields()));
  }

  /**
   * Returns the list of field names that have a {@link RequiredRestriction} set on them (irrespective of whether it's a
   * strict one or not).
   * <p>
   * TODO: DCC-1076 will render it unnecessary (everything would take place in {@link RequiredRestriction}).
   */
  @JsonIgnore
  public List<String> getRequiredFieldNames() {
    List<String> requiredFieldnames = newArrayList();
    for (val field : fields) {
      if (field.hasRequiredRestriction()) {
        requiredFieldnames.add(field.getName());
      }
    }
    return copyOf(requiredFieldnames);
  }

  @JsonIgnore
  public FileType getFileType() {
    return FileType.from(name);
  }

  @JsonIgnore
  public DataType getDataType() {
    return getFileType().getDataType();
  }

  @JsonIgnore
  private Iterable<String> getFieldNames(Iterable<Field> fields) {
    return Iterables.transform(fields, new Function<Field, String>() {

      @Override
      public String apply(Field field) {
        return field.getName();
      }

    });
  }

  /**
   * Returns whether or not the provided file name matches the pattern for the current {@link FileSchema}.
   */
  public boolean matches(@NonNull String fileName) {
    return compile(pattern) // TODO: lazy-load
        .matcher(fileName).matches();
  }

  /**
   * Returns a list of file schema having relations afferent to the current file schema and that have a 1..n left
   * cardinality ("strict")
   * 
   * TODO: move to dictionary? better name?
   */
  public List<FileSchema> getIncomingSurjectiveRelationFileSchemata(Dictionary dictionary) {
    List<FileSchema> afferentFileSchemata = Lists.newArrayList();
    for (FileSchema tmp : dictionary.getFiles()) {
      for (Relation relation : tmp.getRelations()) {
        if (relation.getOther().equals(name) && relation.isBidirectional()) {
          afferentFileSchemata.add(tmp);
        }
      }
    }
    return ImmutableList.<FileSchema> copyOf(afferentFileSchemata);
  }

  /**
   * Returns the optional field ordinal (0-based).
   */
  @JsonIgnore
  public Optional<Integer> getFieldOrdinal(String fieldName) {
    val index = newArrayList(getFieldNames()).indexOf(fieldName);
    return index == -1 ? Optional.<Integer> absent() : Optional.of(index);
  }

  /**
   * Returns the {@link Field} matching the name provided.
   * 
   * @throws NoSuchElementException if no such field exists.
   */
  @JsonIgnore
  public Field getField(final String fieldName) {
    return find(fields, new Predicate<Field>() {

      @Override
      public boolean apply(Field field) {
        return field.getName().equals(fieldName);
      }

    });
  }

}
