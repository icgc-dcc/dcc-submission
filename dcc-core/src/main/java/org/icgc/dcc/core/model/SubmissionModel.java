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

import lombok.NonNull;
import lombok.Value;

import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.core.model.SubmissionModel.FileModelDigest.FieldModelDigest;
import org.icgc.dcc.core.util.Guavas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

/**
 * A digest of the submission model.
 */
@Value
public class SubmissionModel implements Serializable, ControlFieldsReference {

  String dictionaryVersion;
  Map<FileType, FileModelDigest> files;
  Map<FileType, Join> joins;
  Map<FileType, List<String>> pks;
  Map<FileType, List<String>> fks;
  Map<String, String> generalMapping;

  @Value
  public static class FileModelDigest implements Serializable {

    Pattern pattern;
    Map<String, FieldModelDigest> fields;

    @Value
    public static class FieldModelDigest implements Serializable {

      ValueType type;
      boolean controlled;
      Optional<Map<String, String>> mapping;

    }
  }

  @Value
  public static class Join implements Serializable {

    FileType target;
    boolean innerJoin; // TODO: enum?

  }

  @JsonIgnore
  public Map<String, Optional<Map<String, String>>> getFileMapping(FileType fileType) {

    return Guavas.transformValues(
        getFields(fileType),
        TO_OPTIONAL_MAP);
  }

  @JsonIgnore
  public Map<String, ValueType> getValueTypes(FileType fileType) {
    return Guavas.transformValues( // TODO: defensive copy
        getFields(fileType),
        TO_VALUE_TYPE);
  }

  @JsonIgnore
  public Map<String, Map<String, String>> getMappings(
      @NonNull final FileType fileType) {

    return Guavas.transformValues(
        filterValues(
            getFields(fileType),
            HAS_MAPPING),
        TO_PRESENT_MAP);
  }

  @JsonIgnore
  public Pattern getPattern(FileType fileType) {
    return getFiles().get(fileType).getPattern();
  }

  @JsonIgnore
  public List<String> getFieldNames(FileType fileType) {
    return ImmutableList.copyOf(getFields(fileType).keySet()); // TODO: cleanup
  }

  @JsonIgnore
  public Set<DataType> getDataTypes() {
    return newLinkedHashSet(transform(
        getFiles().keySet(), FileType.TO_DATA_TYPE));
  }

  @Override
  @JsonIgnore
  public boolean isControlled(FileType fileType, String fieldName) {
    return getFields(fileType).get(fieldName).isControlled();
  }

  @JsonIgnore
  private Map<String, FieldModelDigest> getFields(final FileType fileType) {
    return getFiles().get(fileType).getFields();
  }

  private static Predicate<FieldModelDigest> HAS_MAPPING = new Predicate<FieldModelDigest>() {

    @Override
    public boolean apply(FieldModelDigest digest) {
      return digest.getMapping().isPresent();
    }

  };

  private static Function<FieldModelDigest, Optional<Map<String, String>>> TO_OPTIONAL_MAP =
      new Function<FieldModelDigest, Optional<Map<String, String>>>() {

        @Override
        public Optional<Map<String, String>> apply(FieldModelDigest digest) {
          return digest.getMapping();
        }

      };

  private static Function<FieldModelDigest, Map<String, String>> TO_PRESENT_MAP =
      new Function<FieldModelDigest, Map<String, String>>() {

        @Override
        public Map<String, String> apply(FieldModelDigest digest) {
          checkArgument(digest.getMapping().isPresent(),
              "Expecting to find mapping");
          return digest.getMapping().get();
        }

      };

  private static Function<FieldModelDigest, ValueType> TO_VALUE_TYPE =
      new Function<FieldModelDigest, ValueType>() {

        @Override
        public ValueType apply(FieldModelDigest digest) {
          return digest.getType();
        }

      };

}
