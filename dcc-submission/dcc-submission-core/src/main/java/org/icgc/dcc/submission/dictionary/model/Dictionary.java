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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.icgc.dcc.submission.core.util.Constants.CodeListRestriction_FIELD;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.validation.Valid;

import lombok.NonNull;
import lombok.ToString;
import lombok.val;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.hibernate.validator.constraints.NotBlank;
import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.submission.core.model.BaseEntity;
import org.icgc.dcc.submission.core.model.HasName;
import org.icgc.dcc.submission.dictionary.visitor.DictionaryElement;
import org.icgc.dcc.submission.dictionary.visitor.DictionaryVisitor;

import com.google.code.morphia.annotations.Entity;
import com.google.code.morphia.annotations.Indexed;
import com.google.code.morphia.annotations.PrePersist;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.mongodb.BasicDBObject;

/**
 * Describes a dictionary that contains {@code FileSchema}ta and that may be used by some releases
 */
@Entity
@ToString(of = { "version", "state" })
public class Dictionary extends BaseEntity implements HasName, DictionaryElement {

  @NotBlank
  @Indexed(unique = true)
  private String version;

  private DictionaryState state;

  @Valid
  private List<FileSchema> files;

  public Dictionary() {
    super();
    this.state = DictionaryState.OPENED;
    this.files = new ArrayList<FileSchema>();
  }

  public Dictionary(String version) {
    this();
    this.version = version;
  }

  @Override
  public void accept(DictionaryVisitor dictionaryVisitor) {
    dictionaryVisitor.visit(this);
    for (FileSchema fileSchema : files) {
      fileSchema.accept(dictionaryVisitor);
    }
  }

  @PrePersist
  public void updateTimestamp() {
    lastUpdate = new Date();
  }

  public void close() {
    this.state = DictionaryState.CLOSED;
  }

  @Override
  @JsonIgnore
  public String getName() {
    return this.version;
  }

  public String getVersion() {
    return version;
  }

  public DictionaryState getState() {
    return state;
  }

  public List<FileSchema> getFiles() {
    return files;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public void setState(DictionaryState state) {
    this.state = state;
  }

  public void setFiles(List<FileSchema> files) {
    this.files = files;
  }

  /**
   * Optionally returns a {@link FileSchema} matching {@link SubmissionFileType} provided.
   * <p>
   * TODO: change to not return optional (never valid not to have a match)
   */
  @JsonIgnore
  public Optional<FileSchema> getFileSchema(SubmissionFileType type) {
    return getFileSchemaByName(type.getTypeName());
  }

  /**
   * Optionally returns a {@link FileSchema} matching the file schema name provided.
   * <p>
   * TODO: phase out in favour of {@link #getFileSchema(SubmissionFileType)}.
   */
  @JsonIgnore
  public Optional<FileSchema> getFileSchemaByName(@NonNull final String fileSchemaName) {
    return Iterables.tryFind(this.files, new Predicate<FileSchema>() {

      @Override
      public boolean apply(FileSchema input) {
        return input.getName().equals(fileSchemaName);
      }
    });
  }

  /**
   * Optionally returns a {@link FileSchema} for which the file name provided would be matching the pattern.
   */
  @JsonIgnore
  public Optional<FileSchema> getFileSchemaByFileName(String fileName) {
    val optional = Optional.<FileSchema> absent();
    for (FileSchema fileSchema : files) {
      if (fileSchema.matches(fileName)) {
        return Optional.of(fileSchema);
      }
    }
    return optional;
  }

  /**
   * Returns the list of {@code FileSchema} names
   * 
   * @return the list of {@code FileSchema} names
   */
  @JsonIgnore
  public List<String> getFileSchemaNames() {
    return newArrayList(Iterables.transform(this.files, new Function<FileSchema, String>() {

      @Override
      public String apply(FileSchema input) {
        return input.getName();
      }
    }));
  }

  /**
   * Returns the list of {@code FileSchema} file patterns.
   * 
   * @return the list of {@code FileSchema} file patterns.
   */
  @JsonIgnore
  public List<String> getFilePatterns() {
    return newArrayList(Iterables.transform(this.files, new Function<FileSchema, String>() {

      @Override
      public String apply(FileSchema input) {
        return input.getPattern();
      }
    }));
  }

  /**
   * Returns a non-null String matching the file pattern for the given {@link SubmissionFileType}.
   */
  @JsonIgnore
  public String getFilePattern(SubmissionFileType type) {
    String pattern = null;
    for (val fileSchema : files) {
      val match = type.getTypeName().equals(fileSchema.getName());
      if (match) {
        pattern = fileSchema.getPattern();
        break;
      }
    }
    return checkNotNull(pattern, "No file schema found for type '{}'", type);
  }

  /**
   * Returns a list of {@link FileSchema}s for a given {@link FeatureType}.
   */
  @JsonIgnore
  public List<FileSchema> getFileSchemata(final FeatureType featureType) {
    val filter = filter(files, new Predicate<FileSchema>() {

      @Override
      public boolean apply(FileSchema input) {
        SubmissionFileType type = SubmissionFileType.from(input.getName());
        return type.getDataType() == featureType;
      }
    });

    return newArrayList(filter);
  }

  public boolean hasFileSchema(String fileName) {
    for (FileSchema fileSchema : this.files) {
      if (fileSchema.getName().equals(fileName)) {
        return true;
      }
    }
    return false;
  }

  public void addFile(FileSchema file) {
    this.files.add(file);
  }

  @JsonIgnore
  public Set<String> getCodeListNames() { // TODO: add corresponding unit test(s) - see DCC-905
    Set<String> codeListNames = newLinkedHashSet();
    for (FileSchema fileSchema : getFiles()) {
      for (Field field : fileSchema.getFields()) {
        for (Restriction restriction : field.getRestrictions()) {
          if (restriction.getType() == RestrictionType.CODELIST) {
            BasicDBObject config = restriction.getConfig();
            String codeListName = config.getString(CodeListRestriction_FIELD);
            codeListNames.add(codeListName);
          }
        }
      }
    }
    return ImmutableSet.copyOf(codeListNames);
  }

  public boolean usesCodeList(final String codeListName) {
    return getCodeListNames().contains(codeListName);
  }
}
