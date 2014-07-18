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
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Maps.filterValues;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.core.util.SerializableMaps;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

/**
 * A model of the submission model.
 */
@RequiredArgsConstructor
public class SubmissionModel implements Serializable, ControlFieldsReference {

  @Getter
  private final String dictionaryVersion;

  private final Map<FileType, FileModel> files;
  private final Map<FileType, JoinModel> joins;
  private final Map<FileType, List<String>> pks;
  private final Map<FileType, List<String>> fks;

  @Getter
  private final Map<String, String> generalMapping;

  @RequiredArgsConstructor
  public static class FileModel implements Serializable {

    private final Pattern pattern;
    private final Map<String, FieldModel> fields;

    @RequiredArgsConstructor
    public static class FieldModel implements Serializable {

      private final ValueType type;
      private final boolean controlled;
      private final Optional<Map<String, String>> mapping;

    }
  }

  @RequiredArgsConstructor
  public static class JoinModel implements Serializable {

    private final FileType target;
    private final boolean innerJoin;

  }

  @JsonIgnore
  public Map<String, Optional<Map<String, String>>> getFileMapping(FileType fileType) {

    return SerializableMaps.transformValues(
        getFields(fileType),
        TO_OPTIONAL_MAP);
  }

  @JsonIgnore
  public Map<String, ValueType> getValueTypes(FileType fileType) {
    return SerializableMaps.transformValues( // TODO: defensive copy
        getFields(fileType),
        TO_VALUE_TYPE);
  }

  @JsonIgnore
  public Map<String, Map<String, String>> getMappings(
      @NonNull final FileType fileType) {

    return SerializableMaps.transformValues(
        filterValues(
            getFields(fileType),
            HAS_MAPPING),
        TO_PRESENT_MAP);
  }

  @JsonIgnore
  public Pattern getPattern(FileType fileType) {
    return files.get(fileType).pattern;
  }

  @JsonIgnore
  public List<String> getFieldNames(FileType fileType) {
    return ImmutableList.copyOf(getFields(fileType).keySet()); // TODO: cleanup
  }

  @JsonIgnore
  public Set<DataType> getDataTypes() {
    return newLinkedHashSet(transform(
        files.keySet(), FileType.TO_DATA_TYPE));
  }

  @Override
  @JsonIgnore
  public boolean isControlledField(FileType fileType, String fieldName) {
    return getFields(fileType).get(fieldName).controlled;
  }

  @JsonIgnore
  public List<String> getPks(FileType referencedFileType) {
    return pks.get(referencedFileType);
  }

  @JsonIgnore
  public List<String> getFks(FileType referencingFileType) {
    return fks.get(referencingFileType);
  }

  @JsonIgnore
  public FileType getReferencedFileType(FileType referencingFileType) {
    return getJoin(referencingFileType).target;
  }

  @JsonIgnore
  public boolean isInnerJoin(FileType referencingFileType) {
    return joins.get(referencingFileType).innerJoin;
  }

  private JoinModel getJoin(FileType referencingFileType) {
    return joins.get(referencingFileType);
  }

  @JsonIgnore
  private Map<String, FileModel.FieldModel> getFields(final FileType fileType) {
    return files.get(fileType).fields;
  }

  private static Predicate<FileModel.FieldModel> HAS_MAPPING = new Predicate<FileModel.FieldModel>() {

    @Override
    public boolean apply(FileModel.FieldModel model) {
      return model.mapping.isPresent();
    }

  };

  private static Function<FileModel.FieldModel, Optional<Map<String, String>>> TO_OPTIONAL_MAP =
      new Function<FileModel.FieldModel, Optional<Map<String, String>>>() {

        @Override
        public Optional<Map<String, String>> apply(FileModel.FieldModel model) {
          return model.mapping;
        }

      };

  private static Function<FileModel.FieldModel, Map<String, String>> TO_PRESENT_MAP =
      new Function<FileModel.FieldModel, Map<String, String>>() {

        @Override
        public Map<String, String> apply(FileModel.FieldModel model) {
          checkArgument(model.mapping.isPresent(),
              "Expecting to find mapping");
          return model.mapping.get();
        }

      };

  private static Function<FileModel.FieldModel, ValueType> TO_VALUE_TYPE =
      new Function<FileModel.FieldModel, ValueType>() {

        @Override
        public ValueType apply(FileModel.FieldModel model) {
          return model.type;
        }

      };

}
